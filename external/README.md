# Native dependencies

## Verovio (LGPL-3.0)

The Android app links **Verovio** as a shared native library (`libverovio-android.so` + bundled `libverovio` objects) for offline engraving of MEI, MusicXML, and related formats.

Obtain the source tree expected at `external/verovio/`:

```bash
git clone --depth 1 https://github.com/rism-digital/verovio.git external/verovio
```

Gradle task `copyVerovioData` copies `external/verovio/data` into `composeApp/src/androidMain/assets/verovio/data` on each `preBuild`.

### Optional: regenerate JNI (SWIG)

If you upgrade Verovio and the Java bindings drift, install [SWIG](https://www.swig.org/) and run:

```bash
# Windows PowerShell (example)
$env:VEROVIO_SWIG = "C:\path\to\swig.exe"
.\gradlew :composeApp:generateVerovioSwigBindings
```

The repository currently includes pre-generated `verovio_wrap.cxx` and `org.verovio.lib.*` sources under `composeApp/src/androidMain/` so a normal build does not require SWIG.
