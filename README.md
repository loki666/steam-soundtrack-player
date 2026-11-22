# Steam Soundtracks

An Android player for the soundtracks you own on Steam. Sign in with your Steam account, browse
your soundtracks, download an album to the phone, and play it with background and lock-screen
controls.

Steam has no streaming API for soundtracks: they are delivered as depot files, like DLC. So this
app talks to Steam the way the desktop client does (via [JavaSteam](https://github.com/Longi94/JavaSteam)),
downloads the album files, and plays them locally.

## Architecture

Single-activity, full Jetpack Compose, Navigation 3.

```
di/            Hilt: the app-wide CoroutineScope; everything else uses @Inject constructors
ui/            Compose screens + Hilt ViewModels (sign in, Steam Guard, library, album, now playing)
nav/           Navigation 3 keys and NavDisplay; the back stack follows the auth state
steam/         SteamSession: CM connection, credential sign-in, Steam Guard, refresh-token log on
               TokenStore: refresh token encrypted with an Android Keystore AES key
library/       SteamCatalog (PICS: licenses -> packages -> apps of type "Music")
               LibraryStore (catalog.json + one folder per downloaded album)
               LibraryRepository (catalog + local files + download progress, as one flow)
               TrackScanner (orders tracks, reads tags, finds cover art)
download/      WorkManager foreground worker running JavaSteam's DepotDownloader
playback/      Media3 MediaSessionService + an app-scoped MediaController wrapper
```

### Sign-in

- A native form sends your account name and password to Steam. JavaSteam encrypts the password
  with Steam's RSA key; it is never stored.
- Steam Guard: the app polls for an approval in the Steam mobile app while also accepting a typed
  code (from the authenticator or by email), whichever comes first.
- Only the long-lived refresh token is kept, encrypted with an Android Keystore key. Later
  launches log on with it silently.

### Library and downloads

- Owned soundtracks come from your licenses over the client protocol (PICS), filtered to
  `common/type = Music`. Album metadata from Steam provides the track list and a square cover
  before anything is downloaded.
- Downloads go to the app's external files directory (`Android/data/<package>/files/albums/<appId>`),
  so no storage permission is needed. Signing out keeps downloaded files.
- When a soundtrack ships several audio formats (MP3 + WAV, MP3 + FLAC…), only one is downloaded.
  Opening an album runs a manifest-only pass (`FormatProbe`) that lists every file with its size,
  so the album page shows each format's download size. The user picks one in a sheet, or the
  default from Account › Downloads applies: "Best quality" (FLAC > WAV > OGG/Opus/M4A/AAC > MP3)
  or "Smaller files" (the smallest). JavaSteam's internal per-file filter (enabled via
  reflection, see `DepotFiles`) then fetches just that format plus cover art and booklets. If the
  filter can't be enabled, everything is downloaded and the other formats are deleted afterwards.
- Albums download one at a time; the others show as waiting.

### Fonts

Space Grotesk (titles, cover text) and Inter (everything else), as in the designs. Static TTFs
from Google Fonts live in `app/src/main/res/font`; both are under the SIL Open Font License, whose
texts ship in `app/src/main/assets/licenses`.

## Build

Requirements: JDK 17+, Android SDK with platform 37.1.

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # R8-minified, signed with the debug key for now
./gradlew testDebugUnitTest
```

Release builds are minified with R8. Everything reached by reflection has a keep rule in
`app/proguard-rules.pro` (JavaSteam, protobuf, BouncyCastle, zstd-jni, Navigation 3 keys, and the
DepotDownloader filter fields). `DepotFilesTest` fails if a JavaSteam upgrade renames the latter.

## Known limitations

- Untested against a live Steam account so far: sign-in, the catalog and downloads need a real
  device and account.
- The per-file download filter relies on JavaSteam internals until it is exposed publicly.
- Release builds use the debug signing key until a release keystore is set up.
- Older soundtracks whose files live in the parent game's depots (rather than their own app) may
  not download.
