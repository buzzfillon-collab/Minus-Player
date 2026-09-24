# Minus Player

A modern, desktop-class Android media player for Android 17+.

## Vision

Minus Player is designed to handle local audio and video with a polished large-screen experience while remaining excellent on phones and tablets. The project prioritizes playback reliability, hardware acceleration, broad media compatibility, adaptive UI, accessibility, and a maintainable architecture.

## Initial playback targets

- Video: H.264/AVC, H.265/HEVC, VP8, VP9, AV1 where the device supports it
- Audio: MP3, AAC, Opus, Vorbis, FLAC, WAV/PCM, ALAC where supported
- Containers: MP4, MKV, WebM, MOV, AVI, TS/M2TS, FLV, OGG/OGM and common audio containers

Codec support is ultimately device/decoder dependent; the app will expose capabilities honestly rather than pretending every device supports every codec.

## Architecture goals

- Kotlin + Jetpack Compose
- Android 17+ / API 37 target
- Media3 playback foundation
- Adaptive large-screen and windowed UI
- Modular playback, media-library, playlist, subtitle and settings layers
- Unit and Compose UI tests from the beginning
- Continuous build/test validation through GitHub Actions

## Status

Early architecture stage. Playback and UI foundations are being built incrementally.
