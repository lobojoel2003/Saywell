# Privacy

Saywell is an AAC app: what someone says with it is personal. So Saywell is built to know nothing
about the people who use it.

- **No accounts, ads, analytics, crash reporting or tracking.** Saywell contains no code that
  sends information anywhere.
- **Everything stays on the device.** Boards, photos you add, settings, recent sentences and the
  word counts used for keyboard prediction are stored in Saywell's private storage.
- **Android backup.** If device backup is turned on, Android may include Saywell's boards, photos
  and settings in the device's backup, as it does for other apps. Voices are not backed up.
- **Internet use.** Saywell connects to the internet only when you:
  - download a voice in Settings › Saywell voices (from the Sherpa-ONNX releases on GitHub), or
  - import a board file whose pictures are links rather than files (the pictures are fetched once).
- **The phone's voice.** If you choose the phone's text-to-speech engine instead of a Saywell
  voice, the text is spoken by that engine, which is covered by its maker's privacy policy. Some
  engines' voices marked *needs internet* send text to their servers; Saywell lists them as such.
- **Permissions.** Saywell asks for no runtime permissions. Photos are picked with Android's photo
  picker (only the chosen picture is shared with Saywell) and the camera opens in the camera app.

Questions: open an issue at https://github.com/lobojoel2003/Saywell/issues.
