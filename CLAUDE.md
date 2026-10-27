# LMusic notes for Claude

## Build and test (Windows)

- `minSdk` is 33 (Android 13) on the phone and 26 (Wear OS 2) on the watch, `shared` and `streaming`. It was 35 after the rebrand, but lint's `NewApi` check at 33 found no unguarded Android 14/15 calls (2026-10-20). Rerun that check before using any new platform API.
- Watch unit tests: `./gradlew :wear:testDebugUnitTest`. The watch database migration test is instrumented: `./gradlew :wear:connectedDebugAndroidTest` (schemas in `wear/schemas`).

- JDK: Android Studio's bundled one. Set `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` and `ANDROID_HOME="%LOCALAPPDATA%\Android\Sdk"` (there is no `local.properties`).
- `./gradlew :app:assembleDebug` builds ABI-split APKs (arm64-v8a, armeabi-v7a). The x86_64 `Pixel_8a` emulator runs the arm64 APK through translation.
- Unit tests are JUnit 5: `./gradlew :app:testDebugUnitTest --tests "com.twilighty.lmusic.data.ytmusic.*"`.
- `YouTubeMusicStreamTest` has a live test that hits YouTube. It only runs with `YTMUSIC_LIVE=1` in the environment.

## Watch (`wear/`, `shared/`)

Ported from the Nobody Listens fork (MIT, same author; its notice is in `LICENSE`) in 2026-10, keeping LMusic's look.

- **Look:** `presentation/theme/WearTheme.kt` `DefaultWearPalette` is LMusic's purple palette; album-art and phone palettes are used while a song plays. `playContainer`/`playContent` (the play button) are the same as `controlContainer`/`controlContent` in every palette. Titles use Google Sans Flex variable fonts from `WearTitleFonts.kt` (`WearTitleFont`, `WearLibraryTitleFont`, `WearWatchLibraryTitleFont`). `bestContrastContent` is plain Kotlin so `WearPaletteContrastTest` runs on the JVM. `WearScreen` (`rememberWearScreen()`) gives the player its round/square inset and a control scale designed at 192 dp and capped at 1.1×, because larger buttons push the bottom row past the curve. Test on the Galaxy Watch5 Pro (225 dp round) over wireless adb.
- **Bezel/crown:** one owner per page (`WearRotary`, `WatchScalingColumn`), one item per click on Galaxy bezels.
- **Standalone:** the watch must stay useful with no phone. Search (`WatchSearchScreen`, `WearSearchViewModel`) matches songs on the watch first (`WearSearchMatcher`) then asks the phone (`WearBrowseRequest.SEARCH`, answered in `WearCommandReceiver`); YouTube Music and JioSaavn only when their chip is tapped. Saved playlists (`WearPlaylistRepository`, tables `local_playlists` / `local_playlist_songs`) store the phone playlist and fetch its songs one transfer at a time, so they play offline. The phone marks songs saveable (`canSaveToWatch`); the transfer still checks the file.
- **Watch database:** `WearMusicDatabase` migrations must be listed in `ALL_MIGRATIONS` (registered in `WearModule`, no destructive fallback). Version 5 is LMusic before the port, 6 adds the playlist tables. Schemas are exported to `wear/schemas`; `WearMusicDatabaseMigrationTest` checks 5 → 6 keeps every song.
- **Streaming on the watch:** `WearStreamingCatalog` gives search and queue mapping for YouTube Music and JioSaavn. `WearYouTubeRepository` (signed out, Normal quality) and `WearJioSaavnRepository` (signed out, asks 320 kbps) use the watch's own connection; on the phone's Bluetooth proxy they drop to Data saver and 160 kbps (`WearStreamingNetwork.isFast`). Queue items are `ytmusic://track/<id>` / `jiosaavn://track/<id>`, resolved as ExoPlayer loads by `wearPlayerDataSourceFactory` (`WearStreamDataSource.kt`): YouTube gets the client User-Agent and `Range: bytes=0-` (no factory User-Agent), JioSaavn gets jiosaavn.com's User-Agent, Referer and Origin. **Streams stutter on the Bluetooth proxy** (about 1.6 Mbps, and Wear OS keeps Wi-Fi off while the phone is connected), so `WearStreamingPlayback` requests Wi-Fi while a stream is meant to play and releases it 15 s after a pause or the end. Playback waits up to 4 s for Wi-Fi, buffers up to 3 minutes ahead (`wearStreamingLoadControl`) and retries network errors three times per song. Watch songs never request Wi-Fi.
- **Handoff (Output screen):** switching to the phone sends `WearPlaybackCommand.CONTINUE_ON_PHONE` with the watch's queue (`WearHandoffSong`: phone song ids, `yt:<videoId>`, `jiosaavn:<id>`; `device:` songs stay behind) and position. The phone maps it in `WatchHandoffQueue` and plays it. Switching to the watch plays the watch's copy, or streams the song itself when the phone publishes `WearPlayerState.streamId` (`MusicService.currentStreamIdForWear`), or else asks for a temporary file transfer. The phone publishes its position only on changes, so the watch uses `WearStateRepository.livePhonePositionMs()`.
- **Starting phone playback from the watch:** `WearCommandReceiver` is destroyed as soon as a message is handled, so queue loads (`PLAY_FROM_CONTEXT`, `CONTINUE_ON_PHONE`) run in `WatchStartedPlayback` (app scope, its own `MediaController` on the application context). A controller built on the receiver's context crashed the app (`Service not registered`) when released after the service died.
- **Signing:** the watch release build uses the phone's key (`keystore.properties`, else `lmusic-release.jks`); the Data Layer only pairs apps with the same `applicationId` and key.

## Streaming module (`streaming/`)

The client cores live in the `:streaming` library so the watch can use them, with unchanged packages: YouTube Music (`YouTubeMusicApi`, `Clients`, `Models`, `Parsers`, `Formats`, `Thumbnails`, `Auth`, `YouTubeMusicStreamFinder`), JioSaavn (`JioSaavnApi`, `Clients`, `Models`, `Parsers`, `JioSaavnStreamFinder`) and `StreamingTrack`/`StreamingCollection`/`StreamingShelf` (`data/stream/StreamingCatalog.kt`). App-only parts (repositories, libraries, downloaders, proxies, cache, remuxer, URIs) stay in `app/`. The stream finders own the lookup logic (YouTube's client order and 10-minute backoff; JioSaavn's details, signed URL and 320-to-160 fallback); the phone repositories add their caches and shared in-flight lookups on top.

## YouTube Music (`data/ytmusic`, `presentation/ytmusic`)

Streams, downloads and video use the **VISIONOS** innertube client (`YouTubeMusicClients.STREAM`). As of 2026-10 it is the only client that returns direct googlevideo URLs which play to the end without a PO token or signature deciphering. ANDROID_VR, IOS_MUSIC and ANDROID_MUSIC hit bot checks. Plain IOS URLs are cut off after about 1 MB. WEB_REMIX is used only for browse and search.

Rules that each caused a real "won't play" bug:

- **Use `YouTubeMusicClients.http(okHttpClient)`, never the app-wide `OkHttpClient` directly.** The shared client rewrites `User-Agent` to `LMusic/1.0`, and googlevideo URLs are bound to the requesting client's User-Agent. In debug it also logs whole bodies, which buffers entire songs in memory.
- **Always send a `Range` header to googlevideo.** Requests without one stall forever. ExoPlayer sends none for position 0, so `YouTubeMusicStreamProxy` sets `alwaysRequestRange` and answers with a plain 200. The downloader fetches in 4 MB ranges.
- **Saved queues hold proxy URLs (`http://127.0.0.1:<port>/ytmusic/<id>`), and the port changes every launch.** `DualPlayerEngine`'s resolver calls `rebindStaleProxyUri` to point them at the running proxy.
- `resolveStream` shares one lookup per song and caches it until 30 minutes before the URL expires. The player, the proxy and the next-song pre-loader all ask for the same song at the same time.
- Only clients with `probeBeforeUse` (the IOS fallback) get their URL probed past 1.5 MB. VISIONOS URLs don't get cut off, so probing them only adds a round trip. A client that fails gets a 10-minute backoff.

Other behaviour:

- Tracks that actually play are saved to the `songs` table (`YouTubeMusicLibrary`, ids from `YouTubeMusicUris.songId`, source type `YTMUSIC`), so likes, playlists and stats work. The queue is only remembered; a track is saved when its stream is first prepared (`saveIfQueued`), so browsing a shelf doesn't flood the library. Existing rows get only a metadata update (`updateStreamedSongMetadata`), because `songs.is_favorite` is kept in sync from the favorites table by database triggers.
- Downloads take the highest-bitrate audio (Opus itag 251 on free accounts). `YouTubeMusicOpusRemuxer` copies it into a tagged `.opus` file without re-encoding, saved under `Music/LMusic/YouTube Music`.
- Video plays the HLS manifest from the same VISIONOS response in `YouTubeMusicVideoActivity` (needs `media3-exoplayer-hls`). It opens in landscape with the system bars hidden, and a gear menu picks the quality (Auto or a fixed height). As of 2026-10, VISIONOS responses carry no ad placements and the HLS has no ad segments.
- The video activity is `singleTask` in its own task (`taskAffinity` `${applicationId}.video`) with `supportsPictureInPicture`. Back or Home while playing enters PiP, which has a play/pause `RemoteAction`; closing PiP finishes the activity. It connects a `MediaController` to `MusicService`: if that song (`YouTubeMusicUris.songId`) is current, the video starts at its position and pauses it, and on close the song is seeked to the video's position.
- YouTube WebM streams report no bitrate to ExoPlayer. `PlayerViewModel` takes it from `DualPlayerEngine.knownStreamBitrate`, so the label shows kbps and the player doesn't re-read the stream with `MediaMetadataRetriever`. 48 kHz is YouTube's maximum sample rate (Opus is always 48 kHz).
- Start time on the x86_64 emulator is mostly the emulator itself: about 1 s of DNS per new googlevideo host, and slow software Opus decoder setup. Measure on a real phone before optimising further.
- Settings › Playback › YouTube Music has a streaming quality and a download quality setting (`YouTubeMusicAudioQuality`: High, Normal, Data saver), stored in the plain `ytmusic_settings` prefs. Each one caps the bitrate in `YouTubeMusicFormats.ranked`. Downloads pass `opusOnly`, so every download can be remuxed to a tagged `.opus` file. The stream cache key includes the quality.
- Search suggestions from YouTube are **off by default** (Settings › Playback › YouTube Music › Search suggestions, `search_suggestions` in `ytmusic_settings`). `YouTubeMusicRepository.searchSuggestions` returns nothing while it is off, so neither search box sends keystrokes to YouTube.
- Search runs the YouTube Music search and a plain youtube.com video search (`WEB` client, videos-only params) at the same time. Videos not already in the music results are added as a "YouTube videos" shelf.
- **Offline first:** YouTube features only touch YouTube songs (`ytmusic://`) or YouTube places (the YouTube Music screen, Search with the YouTube chip on). Phone songs never go online for them.
- **Radio:** `next` with `RDAMVM<id>` gives about 50 songs and a continuation. `YouTubeMusicRadio` remembers the radio's song ids, and `PlayerViewModel.onMediaItemTransition` appends the next batch when three or fewer songs are left. It starts from "Start radio" in the song sheet.
- **Pages:** `YouTubeMusicPlaylist.kind` is PLAYLIST, ALBUM (`MPREb_`) or ARTIST (`UC`, only when `pageType` is `MUSIC_PAGE_TYPE_ARTIST`). The dashboard VM keeps a page stack so back goes artist → album → back.
- **Lyrics:** for YouTube songs, `LyricsRepositoryImpl` tries the lyrics API (synced) and then YouTube's plain lyrics (the `MPLY…` browse page from `next`). Embedded and local lyrics are skipped because there's no file.
- **Account sync:** `MusicRepositoryImpl.setFavoriteStatus` calls `YouTubeMusicAccountSync`, which sends `like/like` or `like/removelike` only for YouTube songs and only when signed in.
- **Thumbnails:** `YouTubeMusicThumbnails`. Album art is requested at 800px, or 1200px for download covers. For video thumbnails it tries maxresdefault → hq720 → sddefault → hqdefault and crops the download cover square. When the HD cover is found after playback starts, `YouTubeMusicLibrary.artworkUpdates` swaps it into the player.
- **Cache:** `YouTubeMusicCache` is a 1 GB LRU `SimpleCache` used only for YouTube streams, routed in `DualPlayerEngine`. Songs not played for 7 days are removed at startup.
- **Library:** the source filter cycles All → Cloud → Phone → Downloaded. `StorageFilter.DOWNLOADED` is SQL `filterMode = 3`: phone files under `…/LMusic/YouTube Music`.
- Search, feed, play, download and video work signed out. Only the Library tab needs the account cookie.
- `Velune/` next to this repo is the GPL-3 reference app. Do not copy its code here, because LMusic is MIT.

## Shared streaming screens (`data/stream`, `presentation/streaming`)

YouTube Music and JioSaavn share one dashboard (`StreamingDashboardScreen` + `StreamingDashboardViewModel`), one set of Search-screen results (`StreamingSearchResults`), the now-playing download button and the "Start radio" row. Their models implement `StreamingTrack`, `StreamingCollection` and `StreamingShelf`; each service's view model only supplies its calls. Change the shared files, not a copy, when a screen should change for both.

- **Import:** playlist and album pages have an Import button. It saves the tracks to `songs` (`saveAll`) and calls `StreamingPlaylists.importOrUpdate` with id `ytmusic_<id>` or `jiosaavn_<kind>_<id>`, so importing again refreshes the same LMusic playlist.
- The Downloaded filter (`filterMode = 3` in 17 `MusicDao` queries, and `LibrarySourceFilter.DOWNLOADS_FOLDER_SUFFIXES`) accepts both `…/LMusic/YouTube Music` and `…/LMusic/JioSaavn`. A new download folder must be added in both places.

## JioSaavn (`data/jiosaavn`, `presentation/jiosaavn`)

It uses JioSaavn's web API (`www.jiosaavn.com/api.php?__call=…&api_version=4&ctx=web6dot0`), the same calls jiosaavn.com makes. It is unofficial and may change.

- **Streams:** `song.getDetails` gives the `encrypted_media_url`, and `song.generateAuthToken(url, bitrate, pid)` turns it into a signed `saavncdn.com` URL. **Do not decrypt the URL or rewrite `_96` to `_320` yourself.** That gets around JioSaavn's Pro paywall; the server decides what each account gets. The granted bitrate is read from the file name (`_160.mp4`) and shown in the player. Songs with only `encrypted_drm_media_url` can't be played and say so.
- AAC in MP4. ExoPlayer plays it directly; `JioSaavnStreamProxy` (`http://127.0.0.1:<port>/jiosaavn/<id>`) keeps the player's URL stable while the signed URL expires (cached 20 minutes, signed again at the same bitrate on 403). It shares `YouTubeMusicCache`, keyed by the **granted** bitrate (`jiosaavn:<id>:<bps>`), because signing in or out can hand out a different file for the same setting and two files must never share a cache entry.
- **The file server answers 403 unless the request carries jiosaavn.com's `Referer`/`Origin`** (`JioSaavnClients.decorateMediaRequest`). Signed out, JioSaavn granted 320 kbps in 2026-10 (checked by file size).
- `jiosaavn://track/<id>`, `SourceType.JIOSAAVN = 8`, ids at offsets -18T/-19T/-20T. Saved on first play like YouTube (`JioSaavnLibrary`).
- **Radio** needs `ctx=android`: `webradio.createEntityStation` with `entity_id=["<id>"]`, then `webradio.getSong` (20 at a time, keyed "0", "1", …).
- **Sign-in** is a WebView at `/login`; the jiosaavn.com cookies are saved only after `library.getAll` accepts them. Library = `library.getAll` (+ `library.getDetails` for titles); likes = `library.add` / `library.delete` (`entity_type=song`). These were not checked against a real account when written: verify the reply shapes the first time someone signs in.
- Text comes HTML-escaped (`&quot;`), numbers come as strings or numbers, images at 150x150 (`JioSaavnParsers.artwork` asks for 500x500). Lyrics are plain (`lyrics.getLyrics`, `<br>` lines), tried after the synced lyrics API.
- Downloads are tagged `.m4a` (taglib) in `Music/LMusic/JioSaavn`. Settings › Playback › JioSaavn has stream and download quality (320/160/96 kbps, `jiosaavn_settings`).
- `JioSaavnLiveTest` hits JioSaavn and only runs with `JIOSAAVN_LIVE=1`.
