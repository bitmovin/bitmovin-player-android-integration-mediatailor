# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Fixed
- `IndexOutOfBoundsException` when MediaTailorAdBreak (Avail) contains no ads. When player reaches such ad break `AdBreakStarted` and `AdBreakFinished` ere emitted, but no `AdStarted` or `AdFinished` are emitted

## [0.1.2] - 2025-09-03

### Fixed

- Race condition that could cause `IndexOutOfBoundsException` when `MediaTailorAdBreak`s got removed during playback

## [0.1.1] - 2025-07-11

### Fixed

- `IndexOutOfBoundsException` when jumping from an ad break to another ad break with less ads then the previous active ad index
- `MediaTailorSessionManager` not emitting all ad related events when seeking or time shifting from an active ad break to another ad break

## [0.1.0-alpha.6] - 2025-07-03

### Added

- Initial alpha release of the MediaTailor integration for Bitmovin Player Android
