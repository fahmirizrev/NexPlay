# NexPlay Release Process

## Purpose

This document defines the release discipline for NexPlay.

Release work must remain conservative and evidence-driven.

## Release Candidate

Before preparing a public release:

- freeze the approved source candidate;
- stop unrelated feature work;
- confirm the working tree is clean;
- confirm application identity;
- confirm version metadata.

## Signing

Production releases must use the stable NexPlay production signing identity.

Private signing material must never be committed to Git.

The release build reads its signing configuration from:

```text
NEXPLAY_RELEASE_STORE_FILE
NEXPLAY_RELEASE_STORE_PASSWORD
NEXPLAY_RELEASE_KEY_ALIAS
NEXPLAY_RELEASE_KEY_PASSWORD
```

The production keystore must remain outside the tracked repository.

Signing passwords must exist only in an approved local or CI secret environment and must never be written into tracked files.

Release signing tasks should run with Gradle configuration cache disabled.
## Build Gate

A release candidate must pass:

- unit tests;
- Android lint;
- debug build regression verification;
- Android test APK assembly;
- release build;
- Git diff validation.

## Artifact Verification

The actual release artifact must be checked for:

- package identity;
- version code;
- version name;
- signature;
- installability.

## Runtime Verification

The signed release artifact must be tested on representative physical devices.

Critical release flows include:

- application startup;
- media permission flow;
- Folders;
- Folder Detail;
- Search;
- PlayHub;
- Queue;
- Playlist;
- Audio Player;
- Audio Mini Player;
- Video Player;
- fullscreen video;
- Child Lock;
- background playback;
- notification controls;
- lock-screen controls;
- headset or Bluetooth transport where available;
- Android Open With;
- theme persistence;
- playlist persistence;
- relevant preference persistence.

## Public Repository Audit

Before making the repository public, audit:

- current files;
- Git history;
- API keys;
- passwords;
- tokens;
- signing files;
- private personal information;
- development notes;
- licensing.

## Release Publication

Only after release acceptance:

1. update and verify public documentation;
2. create the root `CHANGELOG.md` for the first actual public release;
3. run the final static, signed-artifact, and runtime verification gates;
4. create the approved release commit;
5. complete the final public-repository and Git-history audit while the repository is still private;
6. perform any approved public-history cleanup before changing repository visibility;
7. verify the final public baseline;
8. make the repository public;
9. create the release tag;
10. publish the GitHub Release;
11. attach the approved signed APK;
12. publish the artifact SHA-256 checksum;
13. verify the published release, tag, source tree, and downloadable artifact.
## Stabilization

After the first public release, fixes belong to the stabilization phase.

Only reproducible regressions should trigger stabilization fixes.

Do not use stabilization as an excuse for speculative refactoring or feature expansion.