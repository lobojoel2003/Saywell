# Releasing

## Version numbers

- The version is in [`version.properties`](../version.properties) and follows
  [Semantic Versioning](https://semver.org): `MAJOR.MINOR.PATCH`.
  - **PATCH**: fixes only.
  - **MINOR**: new features; boards and settings from earlier versions keep working.
  - **MAJOR**: changes that need people to do something when updating.
- The Android build number (`versionCode`) is derived from it:
  `MAJOR × 10000 + MINOR × 100 + PATCH` (0.1.0 → 100, 1.2.3 → 10203). It always increases, so
  Android installs each release as an update. MINOR and PATCH stay below 100.
- Release tags are `v` plus the version: `v0.1.0`.
- CI builds carry the workflow run number and commit, shown in **Settings › About Saywell**.
  Debug builds use the package `io.github.lobojoel2003.saywell.debug` and version suffix `-debug`, so
  they install next to a release build.

## Making a release

1. Make sure `main` is green in [CI](https://github.com/lobojoel2003/Saywell/actions/workflows/ci.yml).
2. Update `version.properties`.
3. In [`CHANGELOG.md`](../CHANGELOG.md), rename **Unreleased** to `## [X.Y.Z] - YYYY-MM-DD`, add a
   new empty **Unreleased** section above it, and update the comparison links at the bottom.
4. Commit (`release: X.Y.Z`) and push to `main`.
5. Tag and push the tag:

   ```bash
   git tag -a vX.Y.Z -m "Saywell X.Y.Z"
   ```

   ```bash
   git push origin vX.Y.Z
   ```

   Or create the release from GitHub's web interface (**Releases › Draft a new release**, new tag
   `vX.Y.Z` on `main`); the workflow runs on the tag either way and fills in the release.

The [Release workflow](../.github/workflows/release.yml) then:

1. runs the full CI (core tests, lint, Robolectric tests, APK builds, voice links);
2. checks the tag matches `version.properties` and that `CHANGELOG.md` has the section;
3. builds release APKs for `arm64-v8a`, `armeabi-v7a`, `x86_64` and a universal APK, signed with
   the project key;
4. verifies the signatures, and writes `SHA256SUMS.txt`;
5. publishes the GitHub Release with the changelog section, install instructions, the signing
   certificate's SHA-256 fingerprint and the files.

`python3 scripts/check_version.py vX.Y.Z` runs the same version checks locally.

## Signing key

Android only installs an update if it is signed with the same key as the installed app, so the
release key must stay the same forever. Keep a safe offline copy.

Create it once (on your own computer):

```bash
keytool -genkeypair -keystore saywell-release.jks -storetype PKCS12 -alias saywell -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Saywell"
```

Then add these repository secrets (**Settings › Secrets and variables › Actions › New repository secret**):

| Secret | Value |
|---|---|
| `SAYWELL_KEYSTORE_BASE64` | The keystore as base64: `base64 -w0 saywell-release.jks` (macOS: `base64 -i saywell-release.jks`) |
| `SAYWELL_KEYSTORE_PASSWORD` | The keystore password |
| `SAYWELL_KEY_ALIAS` | `saywell` (or the alias you chose) |
| `SAYWELL_KEY_PASSWORD` | The key password (for PKCS12 keystores, the same as the keystore password) |

Without these secrets the workflow still publishes, but signs with a temporary key and says so in
the release notes. Installs from such a release can't be updated by a later release signed with
the project key; people would need to back up their boards, uninstall and reinstall.

To build a signed release locally, set the same names as environment variables, with
`SAYWELL_KEYSTORE_FILE` pointing at the keystore file instead of the base64 secret, and run
`./gradlew assembleRelease`.

## Updating pinned downloads

`gradle/libs.versions.toml` pins the Sherpa-ONNX library, the built-in voice and OpenMoji with
SHA-256 checksums. After changing a version, run a build: it fails and prints the new file's
checksum. Check the new file (release notes, licence, the voice's `MODEL_CARD`), then paste the
checksum in.
