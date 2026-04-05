#!/usr/bin/env python3
"""Generate placeholder metronome WAVs (mono 44.1kHz) for Android res/raw and iOS bundle."""
from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_RAW = ROOT / "composeApp" / "src" / "androidMain" / "res" / "raw"
IOS_RESOURCES = ROOT / "composeApp" / "src" / "iosMain" / "resources"

PRESETS = [
    ("clickwood", 620.0),
    ("beephigh", 1180.0),
    ("beeplow", 320.0),
    ("digital", 880.0),
    ("bell", 520.0),
    ("sharpclick", 1850.0),
    ("woodknock", 380.0),
    ("softtick", 740.0),
]
TIERS = [("strong", 0.9), ("medium", 0.55), ("weak", 0.28)]


def write_wav(path: Path, freq: float, amp: float) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    sample_rate = 44100
    duration_s = 0.045
    n = int(sample_rate * duration_s)
    with wave.open(str(path), "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sample_rate)
        for i in range(n):
            t = i / sample_rate
            env = math.exp(-t * 38.0)
            sample = int(amp * env * 32767.0 * math.sin(2.0 * math.pi * freq * t))
            sample = max(-32768, min(32767, sample))
            w.writeframes(struct.pack("<h", sample))


def main() -> None:
    for base, freq in PRESETS:
        for tier_name, tier_amp in TIERS:
            name = f"{base}_{tier_name}.wav"
            write_wav(ANDROID_RAW / name, freq, tier_amp)
            write_wav(IOS_RESOURCES / name, freq, tier_amp)
    print(f"Wrote {len(PRESETS) * len(TIERS)} files to {ANDROID_RAW} and {IOS_RESOURCES}")


if __name__ == "__main__":
    main()
