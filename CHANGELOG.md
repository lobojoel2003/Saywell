# Changelog

All notable changes to Saywell are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/). The Android build number (`versionCode`)
is `MAJOR * 10000 + MINOR * 100 + PATCH`.

## [Unreleased]

## [0.1.0] - 2026-11-04

First release: an offline, symbol-based AAC talker for Android 7.0 and newer.

### Added

- **Talking with symbols**: a fixed-position board grid, a message bar that shows the sentence
  with its symbols and speaks it when tapped, *Speak*, *Delete* (last word, ending or letter) and
  *Clear*, back and home navigation between boards.
- **Starter vocabulary**: 12 linked English boards with 231 words and phrases — a core-word home
  board plus chat, feelings, food and drink, people, places, activities, body, questions,
  describing, time and a letter board for spelling — colour-coded with the modified Fitzgerald Key
  and illustrated with OpenMoji symbols.
- **Offline neural voice**: the Piper voice Kristin (US English, trained on public-domain
  LibriVox recordings) built in and run on the device by Sherpa-ONNX 1.13.8, with streaming
  playback and instant replay of short phrases.
- **More voices**: 9 more Piper voices with clear licences (English US and UK, Spanish, German,
  Portuguese; female and male) to download once, or install from a file without internet; each
  shows its licence. The phone's own text-to-speech voices can be used instead, with rate and
  pitch. If a voice can't speak, Saywell falls back to the other engine.
- **Editing on the device**: labels, spoken text, colours, 2,456 searchable OpenMoji symbols,
  photos from the gallery or camera, links to other boards (or a new board), Open Board Format
  actions, hidden buttons, moving buttons, board name and size (up to 12 × 12), adding, deleting
  and choosing the home board, and undo.
- **Switch scanning**: one switch (automatic) or two switches (step), rows-then-buttons or linear,
  adjustable speed and rounds, auditory scanning, "tap anywhere" as a switch, and learning which
  keys a switch interface sends.
- **Touch accommodations**: hold to select with a progress bar, and ignore repeated presses.
- **Type to talk** with word prediction that learns the person's words, and **recent sentences**.
- **Display options**: text size, labels above, below or hidden, light, dark and high-contrast
  themes, keep the screen on; TalkBack and Switch Access labels on every control.
- **Open Board Format** import (`.obf`, `.obz`, lenient with other apps' files, add or replace)
  and export (all boards as `.obz`, one board as `.obf`).
- **Safety**: editing and settings locked behind a long press; boards saved atomically with a
  recovery copy of damaged files; voices and board files unpacked with size and path checks.
- **Privacy**: no accounts, analytics or tracking; the network is used only to download voices
  and online pictures of imported boards.
- **Releases**: per-processor APKs (`arm64-v8a`, `armeabi-v7a`, `x86_64`) and a universal APK,
  signed, with SHA-256 checksums; build numbers derived from the version.
- **CI**: core and Robolectric UI tests, Android lint, APK builds and a check that every voice
  download link works, on every push.

[Unreleased]: https://github.com/lobojoel2003/Saywell/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/lobojoel2003/Saywell/releases/tag/v0.1.0
