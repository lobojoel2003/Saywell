# Security policy

## Supported versions

Only the latest release receives fixes. Updating is safe: boards and settings are kept.

## Reporting a vulnerability

Please report security problems privately through GitHub's
[private vulnerability reporting](https://github.com/lobojoel2003/Saywell/security/advisories/new),
not in a public issue. Include the Saywell version (Settings › About Saywell), the Android version
and steps to reproduce. You should get a reply within a week.

## What Saywell does to stay safe

- **No network by default.** The internet is used only to download a voice the person picks, or
  pictures linked from a board file they import. Downloads use HTTPS.
- **Untrusted files are read defensively.** Board files (`.obf`, `.obz`) and voice packages are
  size-limited, zip and tar paths that try to escape their folder are refused, and nothing in them
  is executed.
- **Private storage.** Boards, photos, voices and history live in the app's private storage.
  Android's backup includes boards and settings (not voices); turn device backup off if that isn't wanted.
- **Pinned build inputs.** The native speech library, the built-in voice and the symbols are
  downloaded at build time and checked against SHA-256 checksums in `gradle/libs.versions.toml`.
- **Signed releases.** Release APKs are signed in CI; each release lists the signing certificate
  fingerprint and SHA-256 checksums of the APKs.
