# Architecture

Saywell is a single-activity Android app in Kotlin with Jetpack Compose, plus a plain-Kotlin
`core` library that holds everything that doesn't need Android.

```
core/   (Kotlin/JVM, no Android)          app/   (Android)
├── model        boards, buttons, edits   ├── data      settings, boards, history, images,
├── message      message bar rules        │             symbols, .obf/.obz transfer
├── obf          Open Board Format        ├── speech    neural + system engines, voices,
├── scan         switch-scanning states   │             audio playback, fallback
├── symbols      symbol search            ├── input     switch keys
├── prediction   word prediction          └── ui        Compose screens
└── vocabulary   starter boards
```

## Data

- **Boards** (`core/model`): a `BoardSet` of `Board`s, each a `rows × columns` grid of optional
  `Button`s. A button has a label, optional spoken text, a picture (`ImageRef.Symbol` for bundled
  OpenMoji, `ImageRef.File` for photos), colours, an optional link to another board, Open Board
  Format actions, and a hidden flag. Edits are pure functions (`resized`, `swapCells`,
  `deleteBoard`…) that throw `EditException` with a message for the person when an edit can't be
  made.
- **Storage** (`app/data`): each store is one JSON file written atomically with `AtomicFile`
  (`files/data/boards.json`, `settings.json`, `history.json`). Repositories expose `StateFlow`s and
  save on every change. The board repository keeps the last 30 versions in memory for undo.
  Photos live in `files/images/`; unused ones are removed at start-up.
- **Open Board Format** (`core/obf`): lenient readers and spec-shaped writers for `.obf` and `.obz`.
  Imports get fresh ids; zip reading is bounded.

## Talking

`TalkerViewModel` holds the path of open boards, the message, edit mode and the scanner.
A press goes through `Message.press(button)` in `core/message`, which returns the new message and
effects (say a word, open a board, go home or back, speak the message), so the rules are tested
without Android.

## Speech

`Speaker` picks the engine from settings and falls back to the other if it fails:

- `NeuralSpeechEngine` runs Piper voices with Sherpa-ONNX's `OfflineTts` on one dedicated thread.
  The built-in voice's model is read straight from the APK (stored uncompressed); eSpeak NG's data
  is unpacked to `files/espeak-ng-data` once per app version. Audio streams to an `AudioTrack` as
  it is generated, and short phrases are cached. Every file is checked before the native library
  is called, because Sherpa-ONNX exits the process on a bad configuration.
- `SystemSpeechEngine` wraps Android's `TextToSpeech`.
- `VoiceStore` lists the built-in and installed voices, downloads voices from the catalog and
  installs voice packages (`.tar.bz2`, `.tar.gz`, `.zip`) with path checks.

## Switch scanning

`core/scan/Scanner` is a small state machine over groups of item keys: `advance` (timer or move
switch), `select`, `back`. The view model builds the groups from the screen (message bar controls,
then each board row), runs the timer for one-switch scanning, and turns chosen keys back into
presses. `MainActivity.dispatchKeyEvent` sends key presses to `SwitchInput`, which only claims them
while the talking screen is scanning.

## Build

- `buildSrc` has tasks that verify pinned downloads (SHA-256) and turn them into assets:
  `PrepareOpenMojiSymbols` (SVGs without skin-tone variants, plus a search index) and
  `PrepareBundledVoice` (model, tokens, model card, English eSpeak data).
- Downloads come through Gradle's dependency cache, from ivy repositories declared in
  `settings.gradle.kts` for GitHub releases and the npm registry.
- One APK per ABI plus a universal APK; R8 shrinking with keep rules for the JNI classes.

## Tests

- `core`: JUnit tests for the model, edits, colours, message rules, scanning, OBF round trips and
  lenient parsing, symbol search, prediction, and the starter vocabulary.
- `app`: Robolectric tests for bundled assets, storage, voice installation, switch keys, and
  Compose UI tests of talking, folders, scanning and settings.
