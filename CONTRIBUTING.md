# Contributing to Saywell

Thank you for helping people be heard. Bug reports, board files that don't import well, switch
hardware reports, voices, code and careful translations are all welcome.

## Ground rules

- **Never get between a person and their words.** Talking must keep working offline, start fast,
  and never lose boards. Changes that could lose data need a migration and a test.
- **Positions don't move.** People find words by where they are as much as by their picture.
  Don't reflow a board automatically; only the person editing it moves buttons.
- **The app must stay accessible**: every control works with touch, TalkBack, Android Switch Access
  and Saywell's own scanning; text has 4.5:1 contrast; nothing needs a drag or a fast double tap.
- **Write for a parent or teacher**: short sentences, no jargon, in the interface and in errors.
- **No tracking, no accounts, no network** except where the person asks for it (downloading a voice).

## Translations

Translations are welcome only from people who speak the language well and can check every string
in context. **Machine-translated strings will not be merged**: a wrong word in an AAC app is worse
than an English one. To translate:

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<language>/strings.xml`.
2. Translate every string, keeping the `%1$s`-style placeholders.
3. In the pull request, say how you checked the wording (for example on a device, with an AAC user
   or a speech and language therapist). A second fluent reviewer is very welcome.

A starter vocabulary for another language is a bigger job (word choice, grammar, symbols); please
open an issue first so it can be planned with native speakers and AAC practitioners.

## Development setup

You need JDK 17 or newer (21 recommended) and the Android SDK; Android Studio provides both.

```bash
./gradlew assembleDebug -Psaywell.bundleVoice=false
```

Leaving out the bundled voice makes builds faster; the app then uses the phone's voice until a
Saywell voice is downloaded or installed.

## Tests and checks

CI runs all of these on every push and pull request:

```bash
./gradlew :core:check
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:lintRelease
```

```bash
python3 scripts/check_voice_catalog.py
```

- `core` holds the logic that doesn't need Android (board model, Open Board Format, message bar,
  scanning, symbol search, prediction). Put new logic there when you can, with tests.
- The app's tests run on the JVM with Robolectric, including Compose UI tests of the main flows.

## Code style

Kotlin's official style (`kotlin.code.style=official`, see `.editorconfig`), four-space indents,
trailing commas. Keep comments short and about *why*. User-facing text goes in `strings.xml`.

## Commits and pull requests

- One topic per pull request, with a short description of what changes for the person using Saywell.
- Use [Conventional Commits](https://www.conventionalcommits.org/) prefixes where they fit
  (`feat:`, `fix:`, `docs:`, `ci:`, `build:`, `test:`, `refactor:`).
- Add a line under **Unreleased** in [CHANGELOG.md](CHANGELOG.md) for anything a user would notice.

## Reporting problems

Use the [issue forms](https://github.com/lobojoel2003/Saywell/issues/new/choose). For board import
problems, attach the `.obf` or `.obz` file if it has no personal photos. Report security problems
privately as described in [SECURITY.md](SECURITY.md).

By contributing you agree that your work is licensed under the GPL-3.0-or-later, like the rest of
Saywell, and to follow the [code of conduct](CODE_OF_CONDUCT.md).
