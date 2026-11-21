# Steam Soundtrack Player

An Android player for the soundtracks you own on Steam. Sign in with your Steam account, browse
your soundtracks, download an album to the phone, and play it with background and lock-screen
controls.

Steam has no streaming API for soundtracks: they are delivered as depot files, like DLC. So this
app talks to Steam the way the desktop client does (via [JavaSteam](https://github.com/Longi94/JavaSteam)),
downloads the album files, and plays them locally.

## Architecture

Single-activity, full Jetpack Compose, Navigation 3.

```
ui/            Compose screens + ViewModels (sign in, Steam Guard, library, album, now playing)
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
- When a soundtrack ships both FLAC and MP3, playback uses FLAC.

## Build

Requirements: JDK 17+, Android SDK with platform 37.1.

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest
```

## Known limitations

- Untested against a live Steam account so far: sign-in, the catalog and downloads need a real
  device and account.
- DepotDownloader fetches every file in the soundtrack's depots, so an album shipped in both
  MP3 and FLAC is downloaded twice. JavaSteam's per-file filter isn't exposed yet.
- Older soundtracks whose files live in the parent game's depots (rather than their own app) may
  not download.
- Fonts are the platform defaults rather than the Space Grotesk / Inter pairing in the designs.
