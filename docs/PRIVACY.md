# NexPlay Privacy

## Local-First Design

NexPlay is designed primarily for media stored on or exposed to the Android device.

Core playback does not require a NexPlay account or a cloud media library.

## Media Access

NexPlay requests Android permissions required to discover and play supported local audio and video.

Media access remains subject to Android platform permission rules.

## Local Persistence

NexPlay may store application state locally, including:

- playlists;
- application preferences;
- media sorting preferences;
- theme selection;
- Child Lock configuration.

## Playback State

Runtime playback and queue state are not automatically equivalent to persistent history.

Only explicitly implemented persistent features should be treated as durable application data.

## Networking

Core local media discovery and playback do not require network access.

Any future network-connected feature must document its data handling before release.

## User Control

Users can remove NexPlay application data using normal Android application-management controls.

Uninstalling the application also removes application-private local data according to Android platform behavior.

## Documentation Policy

This document describes current NexPlay behavior.

It must be updated whenever application data handling or network behavior changes.