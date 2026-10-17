# LMusic

A local music player for Android 15 and newer, written in Kotlin with Jetpack Compose.
LMusic is built on top of [PixelPlayer](https://github.com/theovilardo/PixelPlayer)
(the last MIT-licensed version) and adds a Yuyu-styled look, a richer tag editor with
metadata autofill, and deeper listening analytics.

Made by **Abhiram NJ** ([Twilight Abhi](https://github.com/twilighty-abhi)).

## Install

1. Open the [latest release](https://github.com/twilighty-abhi/LMusic-player/releases/latest)
   on your phone and download `LMusic-<version>.apk`.
2. Open the file. The first time, Android asks you to allow installs from your browser
   or **My Files** — allow it once.
3. Later releases install as updates on top. Your library, playlists and stats stay.

Every push to `main` also builds an APK — open the run under **Actions** and download it
from **Artifacts**.

## What LMusic adds

**Yuyu design system**
- Ink-and-paper palette from Yuyu (light and dark), Ember accent, Yuyu radius scale.
- Bricolage Grotesque headings and Figtree text, bundled in the app.
- The now-playing screen still takes its colors from the album art.
- Settings → Appearance → **App colors** switches back to Material You wallpaper colors.

**Tag editor writes into the file**
- Title, artist, album, **album artist**, **year**, genre, track, disc, **composer**,
  lyrics, ReplayGain and embedded cover art — MP3, FLAC, M4A/AAC (plus OGG/Opus/WAV).

**Metadata autofill** (Edit song → **Autofill**)
- *From your files*: offline guesses from the filename (`01 - Artist - Title`), the
  album folder (`Artist - Album (2019)`) and what the other tracks in the folder agree on.
- *Online*: MusicBrainz search, ranked by title, artist, album and duration match,
  plus front covers from Cover Art Archive. Free, no account or API key, and only
  contacted when you tap Autofill.
- Pick a match, see each field as *now → new*, tick what you want, then Save.
  MusicBrainz IDs are stored in the file too.

**Analytics**
- A play counts after **30 seconds** of listening (shorter tracks: nearly all of it).
- Today, week, month, year, all time — or a **custom date range**.
- Listening time, plays, top songs / artists / albums / genres, listening by time of
  day and weekday, sessions, streaks — plus **skip rate**, **new songs** and **new
  artists** discovered in the period.
- History is kept for life (PixelPlayer trimmed it to two years).

### New in 1.2

- New Home, Yuyu redesign of every screen and widget, smaller now-playing layout with sleep timer and song menu.
- Refined haptics (Off / Subtle / Full), smart shuffle, music folders to include, weekly update alerts.
- Library health with batch autofill of empty tags; weekly and monthly recaps.
- Experimental, off by default: on-device mood/genre tagging with YAMNet (Apache-2.0, bundled), mixes,
  song radio and insights. Indexing runs only while charging, idle and above 50% battery; nothing leaves the phone.

## Everything from PixelPlayer is still there

Queue with drag-and-drop, shuffle and repeat, playlists, gapless playback, sleep timer
(including end of track), folders, genres, search, Daily Mix, lyrics (LRCLIB), equalizer,
crossfade/transitions, widgets, Chromecast, Android Auto, Wear OS companion, backups,
and the online sources (Jellyfin, Navidrome, Google Drive, Telegram, Netease, QQ Music).

Not part of LMusic's own plan but kept as-is: lyrics, equalizer, crossfade, widgets,
Chromecast, Android Auto, Wear OS and the online music sources.

## Build it yourself

Open the project in Android Studio (Ladybug or newer, JDK 21) and run the `app`
configuration, or:

```bash
./gradlew :app:assembleRelease   # APK per ABI; LMusic releases ship the arm64-v8a one
```

### Signing

APKs install as updates only when every build is signed with the same key.

- **CI default:** on its first `main` build, GitHub Actions creates a signing key and
  keeps it in the Actions cache (`lmusic-signing-key-v1`). The *Keep CI signing key
  alive* workflow touches it twice a week so it never expires.
- **Your own key (recommended for the long term):** add three repository secrets —
  `LMUSIC_KEYSTORE_B64` (the `.jks` file, base64), `LMUSIC_KEYSTORE_PASSWORD` and
  `LMUSIC_KEY_ALIAS` — and CI uses them instead. On a Mac:

  ```bash
  keytool -genkeypair -keystore lmusic.jks -alias lmusic -keyalg RSA -keysize 4096 -validity 36500
  base64 -i lmusic.jks | pbcopy   # paste as LMUSIC_KEYSTORE_B64
  ```

  Switching keys changes the signature once: uninstall the CI-signed app first
  (back up from Settings → Backup if you want to keep stats).

## Releasing

Bump `APP_VERSION_NAME` in `gradle.properties` (e.g. `1.1.0`), add
`release-notes/v1.1.0.md`, and push to `main`. The workflow sees there's no release
for that version yet, builds the APK and publishes **LMusic v1.1.0** with it (and creates
the tag). Pushing a `v*` tag yourself works too. Other pushes just build an APK artifact.

## Credits and licence

- [PixelPlayer](https://github.com/theovilardo/PixelPlayer) by theovilardo and
  contributors — this project is based on upstream commit `3903015` (12 May 2026),
  the last version released under the MIT License. See [LICENSE](LICENSE).
- Fonts: [Figtree](https://github.com/erikdkennedy/figtree) and
  [Bricolage Grotesque](https://github.com/ateliertriay/bricolage), SIL Open Font
  License — see [licenses/](licenses/).
- On-device audio model: [YAMNet](https://github.com/tensorflow/models/tree/master/research/audioset/yamnet) (Apache-2.0), run with TensorFlow Lite.
- Metadata: [MusicBrainz](https://musicbrainz.org) (CC0) and
  [Cover Art Archive](https://coverartarchive.org).
