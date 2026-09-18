# Accessible Music Player for Android

A lightweight, TalkBack-first music and video player for the audio and video
already on your phone. Ported from an accessible Electron desktop player, but
rebuilt natively so playback survives the screen locking.

## Download and install

1. Open the [Releases page](https://github.com/akankwatsa1/accessible-music-player/releases).
2. Download **`AccessibleMusicPlayer-release.apk`**.
3. Open it on your phone. Android will ask you to allow installs from this
   source the first time - allow it, then tap **Install**.

The APK is signed, so it installs directly. There is nothing to buy, no account
and no network access: every file plays from your own storage.

## What it does

**Finds your music automatically.** On first launch the app asks for access to
your media and then scans the device with MediaStore, so every song and video
already on the phone shows up without you picking folders. Short system sounds
are filtered out. Rescan any time from **More options -> Rescan library**.

**A clean screen you can declutter.** The top bar has one primary control, the
chevron on the right: tap it to hide the search box, filter chips and counters,
leaving just the title, search and options. Your choice is remembered.

**Controls where you expect them.** The video plays above a transport bar pinned
to the bottom with **previous, skip back 10 seconds, play, skip forward 10
seconds and next**, plus a position slider, shuffle and repeat.

**Plays in the background.** Audio keeps playing when you leave the app or lock
the screen. The lock screen, notification shade and Bluetooth headset buttons
all control playback, and unplugging headphones pauses instead of blasting the
speaker.

**Truly usable with TalkBack.** Every control has a spoken label, the list is a
single merged announcement per row, status changes such as tracks found and
skipped seconds are announced politely, and touch targets are at least 48 dp.

Other things included: search across title, artist and album; favourites;
shuffle, repeat all and repeat one; playback speed from 0.5x to 3x; a sleep
timer; video fit and fill zoom; and resume from where you left off.

## Accessibility notes

- Controls are labelled for TalkBack - nothing reads as "button" alone.
- Track rows announce title, artist, video status, favourite state and whether
  they are the current track, as one sentence.
- The position slider offers **Skip back 10 seconds** and **Skip forward 10
  seconds** as TalkBack custom actions.
- Rows offer an **Add to favourites** / **Remove from favourites** custom action.
- Counters and playback changes use polite live regions, so they are announced
  without interrupting.
- The theme is high contrast (dark background, light text) for low vision.

## Project layout

| Path | Purpose |
|---|---|
| `app/src/main/java/.../MainActivity.kt` | Entry point, permissions, incoming media intents |
| `.../MusicPlayerApp.kt` | Holds the repositories |
| `.../data/MediaLibraryRepository.kt` | MediaStore scan of all audio and video |
| `.../data/MediaTrack.kt` | One library item |
| `.../data/SettingsRepository.kt` | Preferences and favourites via DataStore |
| `.../playback/PlaybackService.kt` | ExoPlayer inside a MediaSessionService |
| `.../playback/PlaybackController.kt` | Shared bridge between service and UI |
| `.../ui/PlayerScreen.kt` | The whole Compose interface |
| `.../ui/PlayerViewModel.kt` | State, filtering, playback commands |
| `.../ui/theme/Theme.kt` | High-contrast Material 3 theme |

## How it is built

There is no local Android toolchain needed. Every push to `main` runs
`.github/workflows/build.yml`, which builds a debug and a release APK on a
GitHub runner, verifies the release APK signature with `apksigner`, and attaches
both to a new Release.

To build after a change, just push:

```sh
git add -A
git commit -m "your change"
git push
```

Then download the new APK from the Releases page.

## Technical choices

- **Kotlin + Jetpack Compose**, no XML layouts, no dependency injection
  framework - the app stays small and starts fast.
- **Media3 ExoPlayer inside a `MediaSessionService`.** This is what allows
  background playback and lock-screen controls. A WebView-based player cannot do
  this, because Android suspends web audio as soon as the app is backgrounded.
- **MediaStore rather than file paths**, so it works with scoped storage on
  Android 10 and later without any special permissions.
- **No minification** in the release build. The APK is small regardless and this
  removes any risk of R8 stripping Media3 entry points reached only by
  reflection.

## Requirements

- Android 7.0 (API 24) or newer.
- Media access permission, requested on first launch.

## Licence

MIT - see [LICENSE](LICENSE).