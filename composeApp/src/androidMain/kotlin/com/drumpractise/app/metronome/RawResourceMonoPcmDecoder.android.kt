package com.drumpractise.app.metronome

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.annotation.RawRes
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * 将 `res/raw` 中的压缩音频解码为 **单声道 S16 PCM**，并重采样到 [targetSampleRate]，
 * 供 [android.media.AudioTrack] 以采样时钟驱动节拍。
 */
internal object RawResourceMonoPcmDecoder {
    private const val TIMEOUT_US = 10_000L

    fun decodeMonoS16Resampled(
        ctx: Context,
        @RawRes resId: Int,
        targetSampleRate: Int = 48_000,
    ): ShortArray {
        val result = decodeToInterleavedS16(ctx, resId) ?: return ShortArray(0)
        val mono = downmixToMono(result.samples, result.channelCount)
        return resampleMonoLinear(mono, result.sampleRate, targetSampleRate)
    }

    private data class InterleavedPcm16(
        val samples: ShortArray,
        val sampleRate: Int,
        val channelCount: Int,
    )

    private fun decodeToInterleavedS16(ctx: Context, @RawRes resId: Int): InterleavedPcm16? {
        val chunks = ArrayList<ShortArray>(64)
        var totalShorts = 0
        var outFmt: MediaFormat? = null
        ctx.resources.openRawResourceFd(resId).use { afd ->
            val extractor = MediaExtractor()
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            val audioTrack =
                (0 until extractor.trackCount).firstOrNull { i ->
                    extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: return null
            extractor.selectTrack(audioTrack)
            val inFormat = extractor.getTrackFormat(audioTrack)
            val mime = inFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inFormat, null, null, 0)
            codec.start()
            try {
                val info = MediaCodec.BufferInfo()
                var inputEos = false
                var outputEos = false
                while (!outputEos) {
                    if (!inputEos) {
                        val inIx = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIx >= 0) {
                            val inBuf = codec.getInputBuffer(inIx)!!
                            val n = extractor.readSampleData(inBuf, 0)
                            if (n < 0) {
                                codec.queueInputBuffer(inIx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEos = true
                            } else {
                                codec.queueInputBuffer(inIx, 0, n, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    when (val outIx = codec.dequeueOutputBuffer(info, TIMEOUT_US)) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            outFmt = codec.outputFormat
                        }
                        else -> {
                            if (outIx >= 0) {
                                if (info.size > 0) {
                                    val fmt = outFmt ?: codec.outputFormat.also { outFmt = it }
                                    val buf = codec.getOutputBuffer(outIx)!!
                                    buf.position(info.offset)
                                    buf.limit(info.offset + info.size)
                                    val enc =
                                        if (fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                            fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                        } else {
                                            AudioFormat.ENCODING_PCM_16BIT
                                        }
                                    val shorts = byteBufferToShorts(buf, enc)
                                    chunks.add(shorts)
                                    totalShorts += shorts.size
                                }
                                val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                                codec.releaseOutputBuffer(outIx, false)
                                if (eos) outputEos = true
                            }
                        }
                    }
                }
            } finally {
                try {
                    codec.stop()
                } catch (_: Exception) {
                }
                codec.release()
                extractor.release()
            }
        }
        if (chunks.isEmpty() || outFmt == null) return null
        val channels = outFmt!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val rate = outFmt!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val merged = ShortArray(totalShorts)
        var off = 0
        for (c in chunks) {
            c.copyInto(merged, off)
            off += c.size
        }
        return InterleavedPcm16(merged, rate, channels)
    }

    private fun byteBufferToShorts(buf: ByteBuffer, pcmEncoding: Int): ShortArray {
        val dup = buf.slice().order(ByteOrder.LITTLE_ENDIAN)
        return when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val n = dup.remaining() / 4
                val out = ShortArray(n)
                for (i in 0 until n) {
                    val f = dup.float
                    val s = (f * 32767.0f).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    out[i] = s.toShort()
                }
                out
            }
            else -> {
                val n = dup.remaining() / 2
                val out = ShortArray(n)
                var i = 0
                while (i < n && dup.remaining() >= 2) {
                    out[i++] = dup.short
                }
                if (i < n) out.copyOf(i) else out
            }
        }
    }

    private fun downmixToMono(interleaved: ShortArray, channels: Int): ShortArray {
        val ch = channels.coerceAtLeast(1)
        if (ch == 1) return interleaved
        val frames = interleaved.size / ch
        if (frames <= 0) return ShortArray(0)
        val out = ShortArray(frames)
        var s = 0
        for (f in 0 until frames) {
            var acc = 0
            repeat(ch) {
                acc += interleaved[s++].toInt()
            }
            out[f] = (acc / ch).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun resampleMonoLinear(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        if (fromRate == toRate || input.isEmpty()) return input
        val outLen = (input.size * toRate.toLong() / fromRate).toInt().coerceAtLeast(1)
        val out = ShortArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i * fromRate.toDouble() / toRate
            val i0 = srcPos.toInt().coerceIn(0, input.lastIndex)
            val i1 = (i0 + 1).coerceAtMost(input.lastIndex)
            val frac = srcPos - i0
            val v = input[i0] * (1 - frac) + input[i1] * frac
            out[i] = v.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
