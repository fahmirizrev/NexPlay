# NexPlay Architecture

## Overview

NexPlay is a native Android local-first media player.

The architecture intentionally separates media discovery, queue ownership, playback ownership, persistence, and UI presentation.

## Core Flow

```text
Android MediaStore
        ↓
MediaScanner
        ↓
MediaLibraryRepository
        ↓
Folders / Search / PlayHub / Playlist
        ↓
QueueEngine
        ↓
QueuePlaybackCoordinator
        ↓
PlaybackEngine
        ↓
Single Media3 ExoPlayer
        ↓
Audio Player / Video Player / MediaSession
```

## Media Library

`MediaLibraryRepository` is the canonical application boundary for discovered audio and video media.

Feature UI must consume canonical media-library state rather than creating independent MediaStore ownership.

## Queue

`QueueEngine` owns:

- queue items;
- current index;
- queue mutation;
- shuffle;
- repeat;
- playback context.

Queue policy must not be duplicated inside UI or the playback engine.

## Playback

`PlaybackEngine` owns the single Media3 / ExoPlayer runtime.

It owns runtime playback state and transport operations, but not queue policy.

## Queue and Playback Integration

`QueuePlaybackCoordinator` synchronizes authoritative queue state with runtime playback.

It must not create a hidden queue or a second playback runtime.

## MediaSession

Android MediaSession surfaces reuse the same queue and playback ownership used by the application UI.

System transport controls must remain adapters around the canonical runtime.

## Playlist Persistence

Playlist persistence is separate from runtime queue ownership.

Persistent playlists must not become a hidden playback queue.

## Preferences

Application preferences are persisted independently from runtime playback state.

Examples include:

- theme selection;
- media sorting;
- Child Lock configuration.

## Media Visuals

Media visuals use bounded in-memory loading.

Small list visuals remain lightweight.

Full Audio Player artwork may use a higher-quality embedded-artwork path without changing media-library ownership.

## UI

Jetpack Compose owns presentation and user interaction.

UI components must not create independent queue, playback, or persistence ownership.

## Design Principles

- local-first behavior;
- one canonical media model;
- one media-library boundary;
- one queue owner;
- one playback owner;
- one ExoPlayer runtime;
- explicit persistence boundaries;
- minimal dependencies;
- targeted compatibility fixes based on evidence;
- smallest-change engineering.