# NexPlay Agent Rules

## Project Identity

NexPlay is a native Android, local-first media player for music and video.

- Repository root: `C:\laragon\www\nexplay`
- Active application: `nexplay_kotlin/`
- Primary implementation: Kotlin, Jetpack Compose, and Android Media3
- Public documentation: `README.md`, `CHANGELOG.md`, and `docs/`
- Local development context: `notes/`

The current repository and source are the authority for what is implemented.

## Required Reading

Before implementation or review, read:

1. `AGENTS.md`;
2. `README.md`;
3. the active documents under `docs/` that are relevant to the task;
4. `notes/CURRENT_HANDOFF.md` when it exists and the work continues an active session;
5. historical files under `notes/` only when they are relevant evidence.

NexPlay has no permanent root roadmap. Create one only when a real multi-step
product cycle needs it, not for structural consistency.

## Source of Truth

Product intent, in descending order:

1. the latest explicit user instruction;
2. confirmed project decisions;
3. active project documentation;
4. current conversation context;
5. historical development material.

Implementation facts, in descending order:

1. current repository and source;
2. current terminal, build, and test evidence;
3. current runtime evidence;
4. current screenshots and reports;
5. active documentation assumptions;
6. historical development material.

`notes/CURRENT_HANDOFF.md`, when present, is the current development handoff.
All files under `notes/` are local context only and never override repository or
source evidence.

## Architecture and Ownership

Preserve this flow:

```text
Android MediaStore
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

Ownership boundaries:

- `MediaLibraryRepository` owns canonical media-library access.
- `QueueEngine` owns authoritative queue state and queue policy.
- `QueuePlaybackCoordinator` is the queue/playback integration boundary.
- `PlaybackEngine` owns the single ExoPlayer runtime.
- `PlaylistRepository` owns playlist persistence.
- Preferences/DataStore owns application preferences.
- Jetpack Compose UI owns presentation and user interaction.

Do not create:

- a second `QueueEngine`;
- a second `PlaybackEngine`;
- a second ExoPlayer;
- hidden playback state;
- duplicate persistence ownership.

Detailed architecture is documented in `docs/ARCHITECTURE.md`.

## Engineering Constraints

- Inspect current repository evidence before changing source.
- Preserve unrelated user changes.
- Prefer the smallest change that correctly solves the task.
- Do not add dependencies without approval.
- Do not replace architecture or state management without approval.
- Do not silently change unrelated UI, UX, routing, persistence, or behavior.
- Do not perform large refactors unless explicitly requested.
- Keep media, queue, playback, persistence, and UI ownership explicit.
- Record unrelated findings as concerns rather than fixing them silently.

When implementation evidence is insufficient, perform one scoped discovery from
the relevant entry point through its direct callers, consumers, boundaries,
tests, and configuration. Store local discovery evidence under
`patches/output/`; do not turn temporary findings into permanent project rules.

## Manual Patch Format

When a manual source patch is requested, use:

```text
FILE:
<path>

CARI:
<exact current source block>

GANTI MENJADI:
<complete replacement block>

EFEK:
<short explanation>
```

`CARI` must exactly match current source, include enough search context, preserve
indentation, and never use ellipses inside a source block.

## Verification

Run commands from the repository root.

Standard Android verification:

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin testDebugUnitTest
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin lintDebug
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebug
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebugAndroidTest
git diff --check
```

Run only the checks proportionate to the change, plus release-specific checks
when release configuration or artifacts are involved. Runtime behavior must be
validated on an appropriate physical Android device when the change affects it.

Always distinguish:

- `IMPLEMENTED`
- `VERIFIED`
- `RUNTIME_VALIDATED`
- `CONCERN`
- `KNOWN_LIMITATION`
- `OUT_OF_SCOPE`
- `BLOCKED`

Do not claim a fix, full verification, production readiness, or absence of
regression without supporting evidence.

## Documentation Policy

Active and public documentation must be written in English:

- `README.md` is the public product and repository overview.
- `docs/` contains active architecture, build, privacy, and release guidance.
- `CHANGELOG.md` contains factual public release history.
- `AGENTS.md` contains durable NexPlay-specific engineering rules.

Local development context may use Indonesian. Historical notes, ChatGPT
snapshots, handoffs, and temporary implementation context belong under `notes/`,
not in public documentation.

Public documentation must describe the current native Android application only
and must not present planned or historical behavior as implemented.

## Repository Policy

Tracked public content includes:

- `nexplay_kotlin/`;
- `AGENTS.md`;
- `README.md`;
- `CHANGELOG.md`;
- `LICENSE`;
- `docs/`.

Local-only and ignored content includes:

- `notes/`;
- `patches/`;
- `patches/output/`;
- `prompt/`.

These local-only directories and all secrets or signing material must never be
committed. There is one NexPlay repository for active source, public
documentation, and public releases.

Before committing, verify the relevant change, run `git diff --check`, inspect
`git status`, stage only relevant files, confirm local-only evidence is not
staged, and inspect the staged file list. Do not use `git add .` for controlled
closeout work.

## Release and Maintenance

NexPlay is a released project. Follow `docs/RELEASE.md` for ongoing releases.

- Freeze the accepted release candidate before release work.
- Verify identity, versioning, signing, the signed artifact, installation,
  persistence, supported-device runtime behavior, documentation, and repository
  hygiene.
- Update `CHANGELOG.md` with factual release history.
- Create a release tag only after acceptance.
- Do not add unrelated features during release cutover or stabilization.

Use GitHub issues, the changelog, release notes, and
`notes/CURRENT_HANDOFF.md` when present for normal maintenance. Do not store
temporary task progress in this file.
