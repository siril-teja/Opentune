# OpenTune

A personal fork of [OpenTune](https://github.com/justshivv/Opentune), an Android music player built on YouTube Music. GPL v3, see LICENSE.

## Download

Get the latest APK from the releases page:

- `OpenTune-vX-universal.apk`: standard edition, works on any phone.
- `OpenTune-vX-personal-arm64-v8a.apk`: personal edition.

The app checks for new releases and updates itself with one tap.

## Build

    ./gradlew assembleStandardDebug

Releases are built by GitHub Actions when the version in `app/build.gradle.kts` changes on `main`.
