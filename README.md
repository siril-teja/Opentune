# OpenTune (siril-teja's edition)

A free, ad-free Android music player built on YouTube Music. This is a personal
fork of [OpenTune by justshivv](https://github.com/justshivv/Opentune), released
under the same licence (GPL v3; see [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)).

## Download

Every [release](https://github.com/siril-teja/Opentune/releases/latest) has two APKs:

| File | For |
|---|---|
| `OpenTune-vX-universal.apk` | **Standard edition**, for any Android phone. Nothing personal in it. |
| `OpenTune-vX-personal-arm64-v8a.apk` | **Personal edition**, with its own playlists built in and a Secret asked once per phone. |

Each edition updates itself: when a release is out, the app offers it, and one tap
downloads and installs it. From the second update on, Android 12+ installs it
without asking again.

## What this fork adds

- Paste a YouTube or YouTube Music link into Search: a video plays (with a Download
  button), a playlist, album or channel opens.
- Shared or pasted videos are named after the video and its channel.
- Rename any song from its menu; the name shows everywhere, and can be undone.
- Export data includes which songs are downloaded; Import offers to download them again.
- Optionally keep a copy of each download in Music/OpenTune.
- No usage counting, and nothing is looked up outside YouTube unless turned on
  (Settings › Audio quality › Lossless streaming / JioSaavn streams).

## Building

Android Studio, or `./gradlew assembleStandardDebug` with the Android SDK
(platform 37). `assemblePersonalRelease` builds the personal edition; its Secret
comes from the `OPENTUNE_PERSONAL_SECRET` environment variable (none: no lock).

Releases are made by `.github/workflows/release.yml` when `versionName` in
`app/build.gradle.kts` changes on `main`. They're signed with the key in the
`OPENTUNE_KEYSTORE_*` repository secrets.
