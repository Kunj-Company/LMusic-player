# Contributing to LMusic

Thanks for helping. LMusic is a small personal project, so issues and PRs are reviewed when time allows.

## Before you start

- **Check the issues.** Bugs and planned features live in [Issues](https://github.com/Kunj-Company/LMusic-player/issues) and on the LMusic Roadmap project board.
- **Open an issue first for anything big** (a new feature, a new screen, a new streaming service), so we can agree on the approach before you write code. Small fixes can go straight to a PR.
- Use the issue forms: **Feature**, **Bug** or **Task**.

## Licence: MIT only

LMusic is MIT-licensed (see [LICENSE](LICENSE)). Every contribution must be your own work or come from an MIT-compatible source, with its licence noted.

- **Do not copy code from GPL or proprietary projects** — for example Velune, NewPipe or ViMusic (GPL), or PixelPlayer versions after commit `3903015` (no longer MIT). Reading them to understand an approach is fine; pasting or closely translating their code is not.
- New fonts, models or other bundled assets need their licence file in [`licenses/`](licenses/) and a line in the README credits.

## Setting up

- **Android Studio** (latest stable) with its bundled JDK. The project targets **Java 21**.
- Point `JAVA_HOME` at Android Studio's JDK and `ANDROID_HOME` at your SDK if Gradle can't find them.
- `minSdk` is 33 (Android 13) on the phone and 26 (Wear OS 2) on the watch, `shared` and `streaming`.
- You don't need a signing key for debug builds. Release builds read `keystore.properties`; never commit it or any `.jks` file (both are in `.gitignore`).

## Build and test

```bash
# Debug APKs (arm64-v8a and armeabi-v7a)
./gradlew :app:assembleDebug

# Phone unit tests (JUnit 5)
./gradlew :app:testDebugUnitTest

# Watch unit tests
./gradlew :wear:testDebugUnitTest

# Watch database migration test (needs a connected watch or Wear emulator)
./gradlew :wear:connectedDebugAndroidTest
```

The live tests that call YouTube Music and JioSaavn only run when `YTMUSIC_LIVE=1` or `JIOSAAVN_LIVE=1` is set. Leave them off in normal runs.

Test on a real device when you can, especially for playback, the watch app and anything touching battery. Say in the PR what you tested on.

## Branches and pull requests

1. Fork the repo (or branch, if you have access) from `main`.
2. Name the branch by type: `feature/…`, `fix/…`, `chore/…`.
3. Keep each PR to one change. Link the issue with `Closes #123`.
4. Fill in the PR template, including the devices you tested on.
5. CI builds the app and runs a launch test on every PR. It must pass before merge.
6. PRs are merged into `main`; releases are cut from `main` (see *Releasing* in the README).

## Code style

- Kotlin + Jetpack Compose. Follow the style of the file you're editing; Android Studio's default formatter is fine.
- Keep YouTube Music and JioSaavn changes inside the shared streaming screens (`presentation/streaming`, `data/stream`) when a change should apply to both.
- Don't add features that get around a service's paywall or DRM. Such PRs won't be merged.
- User-facing text goes in string resources, not hard-coded in Composables.

## Reporting security issues

Please don't open a public issue. See [SECURITY.md](SECURITY.md).

## Code of conduct

Everyone taking part is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
