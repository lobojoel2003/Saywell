# Voices

## Saywell voices

Saywell voices are neural text-to-speech voices from the [Piper](https://github.com/rhasspy/piper)
project, run on the device by [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) and ONNX Runtime.
Text is turned into phonemes by eSpeak NG, then into speech by the voice model. Nothing is sent
anywhere.

- One voice is built into the APK, so Saywell talks from the first minute without internet. It is
  set in `gradle/libs.versions.toml` (`bundledVoice`).
- Short phrases are cached, so tapping a word speaks it straight away the second time.
- If a voice can't load (for example on an unsupported processor), Saywell uses the phone's voice
  instead and says why.

### Downloadable voices

**Settings › Saywell voices › Download voices** lists the voices in
[`app/src/main/assets/voices/catalog.json`](../app/src/main/assets/voices/catalog.json). Each comes
from the `tts-models` release of Sherpa-ONNX on GitHub
(`vits-piper-<voice>.tar.bz2`, about 64 MB). CI checks every link on each push.

| Voice | Language | | Licence of the training data |
|---|---|---|---|
| Kristin (built in) | English (US) | female | public domain (LibriVox) |
| Norman | English (US) | male | public domain (LibriVox) |
| John | English (US) | male | public domain (LibriVox) |
| Bryce | English (US) | male | public domain |
| Cori | English (UK) | female | public domain (LibriVox) |
| Alba | English (UK) | female | CC BY 4.0 |
| Davefx | Spanish (Spain) | male | CC0 |
| Ald | Spanish (Mexico) | male | Unlicense |
| Thorsten | German | male | CC0 |
| Faber | Portuguese (Brazil) | male | CC0 |

The catalog only lists voices whose model card states a clear licence. Many other Piper voices
exist (including French and Italian ones) and can be installed from a file; several popular ones
are trained on data licensed for non-commercial use only (for example the HFC voices, CC BY-NC-SA
4.0), and some are fine-tuned from the Lessac voice, whose dataset has its own licence. Check the
model card, which Saywell shows next to every installed voice.

The built-in voice is Kristin because it was trained from scratch on public-domain recordings,
so it can be shipped inside a GPL app without extra conditions.

### Installing a voice without internet on the device

1. On a computer, download a voice package from
   <https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models> (any `vits-piper-*.tar.bz2`).
2. Copy it to the device (USB cable, SD card).
3. **Settings › Saywell voices › Install from a file** and pick it.

Packages in `.tar.bz2`, `.tar.gz` or `.zip` form work if they contain the `.onnx` model,
`tokens.txt` and `espeak-ng-data`.

### Licences

Each Piper voice is trained on a dataset with its own licence, stated in the voice's
`MODEL_CARD`. Saywell shows the licence in the voice list and the full model card behind the
document icon. The built-in voice's model card is printed in the build log and included in the APK.

## The phone's voice

**Settings › Voice › Voice to use › The phone's voice** uses Android's text-to-speech engine.
Voices marked *needs internet* only work online; install offline voice data in
**Android text-to-speech settings**. Pitch can be changed for the phone's voice only.

## Why Sherpa-ONNX 1.13

Sherpa-ONNX 2.0 removed the built-in eSpeak NG phonemizer that Piper voices need, so Saywell uses
1.13.8, the last 1.x release. Moving to 2.x would mean adding a phonemizer to Saywell itself.
