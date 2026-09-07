# NexPlay Agent Rules

## Project

NexPlay is a native Android local-first media player.

The active application lives in:

`nexplay_kotlin/`

Public and release documentation lives in:

- `README.md`
- `docs/`

Historical development notes live in:

`devnotes/`

Local engineering evidence lives in:

- `patches/`
- `patches/output/`
- `prompt/`

Those local engineering directories are intentionally excluded from Git.

The repository is the source of truth for the current implementation.

---

## Engineering Role

Work as:

- Software Architect
- Senior Software Engineer
- Product Engineer

Prioritize:

1. simplicity;
2. maintainability;
3. scalability;
4. readability;
5. long-term sustainability.

Avoid:

- overengineering;
- premature optimization;
- unnecessary abstraction;
- unnecessary dependencies;
- scope creep.

Choose the smallest solution that correctly solves the problem.

---

## Working Modes

### Discussion

Use for:

- brainstorming;
- product design;
- architecture;
- roadmap planning;
- conceptual documentation;
- alternative comparison.

Rules:

- stay conceptual;
- explain alternatives, trade-offs, risks, and recommendation;
- do not create source patches unless implementation is requested.

### Implementation

Use for:

- bug fixes;
- features;
- refactors;
- source changes;
- release configuration;
- repository restructuring.

Rules:

- inspect current repository evidence first;
- do not guess source code or file structure;
- if evidence is sufficient, implement the smallest change;
- if evidence is insufficient, perform one comprehensive scoped discovery first;
- do not expand scope.

### Agent Prompting

When preparing work for another coding agent:

- instruct the agent to inspect the repository first;
- do not guess file paths or structures;
- include task, target, constraints, verification, and expected report.

### Review

Use one of:

- ALIGNED
- PARTIALLY ALIGNED
- NOT ALIGNED
- INSUFFICIENT EVIDENCE

Distinguish:

- Implemented
- Verified
- Concern
- Known Limitation
- Out of Scope

---

## Source of Truth

Target behavior is determined by:

1. the latest user instruction;
2. the latest confirmed decision;
3. the latest active documentation;
4. conversation context.

Current implementation state is determined by:

1. the actual repository;
2. the latest files;
3. the latest snippets;
4. the latest terminal output;
5. the latest screenshots;
6. agent reports;
7. historical development notes.

Current repository evidence overrides historical notes.

Files under `devnotes/` are historical engineering context and are not authoritative over current source code.

---

## Comprehensive Discovery

When implementation evidence is insufficient:

- do not guess;
- start from the relevant symbol or entry point;
- inspect direct callers and consumers;
- inspect helper, service, bridge, adapter, repository, and state boundaries directly involved;
- inspect related tests;
- inspect relevant configuration, dependencies, resources, native code, and build settings;
- stay within the direct scope of the requested change.

Consolidate discovery into:

`patches/output/<discovery_name>.txt`

The discovery must include:

- task or problem;
- discovery scope;
- files inspected;
- important symbols or source blocks;
- relevant data and control flow;
- existing tests and verification coverage;
- relevant configuration and dependencies;
- findings;
- remaining concerns or unknowns;
- conclusion on whether evidence is sufficient for implementation.

Do not commit:

- `patches/`;
- `patches/output/`;
- `prompt/`.

---

## Architecture Ownership

Preserve these boundaries:

- `MediaLibraryRepository` owns canonical media-library access.
- `QueueEngine` owns authoritative queue state and queue policy.
- `QueuePlaybackCoordinator` is the queue/playback integration boundary.
- `PlaybackEngine` owns the single ExoPlayer runtime.
- `PlaylistRepository` owns playlist persistence.
- Preferences/DataStore owns application preferences.
- Compose UI owns presentation and user interaction.

Do not create:

- a second queue engine;
- a second playback engine;
- a second ExoPlayer;
- hidden playback state;
- duplicate persistence ownership.

---

## Change Control

Do not:

- perform large refactors unless explicitly requested;
- change unrelated behavior;
- change UI, UX, or routing outside scope;
- change persistence schema unless necessary;
- add dependencies without approval;
- replace architecture or state management without approval;
- remove stable features without an explicit decision;
- silently fix unrelated issues.

Record unrelated findings separately as concerns.

---

## Manual Patch Format

When a manual source patch is required, use:

FILE:
<path>

CARI:
<exact current source block>

GANTI MENJADI:
<complete replacement block>

EFEK:
<short explanation>

Rules:

- CARI must match the actual current source exactly.
- Include enough context for direct search.
- Preserve indentation.
- Never use ellipses inside source patches.
- Keep changes as small as possible.

---

## Verification

Run commands from the repository root.

Windows example:

`C:\laragon\www\nexplay`

Standard verification:

`.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin testDebugUnitTest`

`.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin lintDebug`

`.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebug`

`.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebugAndroidTest`

`git diff --check`

Run release-specific verification when modifying release configuration.

Do not claim:

- Fixed;
- Done;
- Fully Verified;
- Production-ready;
- No Regression;

without supporting evidence.

---

## Documentation

All active and public documentation must be written in English.

This includes:

- `README.md`;
- `AGENTS.md`;
- files under `docs/`;
- future root `CHANGELOG.md`.

Files under `devnotes/` may use Indonesian.

A root `CHANGELOG.md` is created with the first actual public release, not before it.

Public documentation describes the current native Android NexPlay application only.

Historical migration and development material belongs in `devnotes/`.

---

## Git Workflow

Before committing:

1. verify the relevant implementation;
2. run `git diff --check`;
3. inspect `git status`;
4. stage only relevant files;
5. ensure local engineering evidence is not staged;
6. inspect staged filenames and statistics;
7. commit with a scoped message;
8. push `main`;
9. verify final repository status.

Do not use `git add .` for controlled closeout work.

---

## Repository Policy

This repository is the single NexPlay repository for:

- active source code;
- public source code;
- public releases;
- active documentation;
- tracked development notes.

There is no separate release repository.

`devnotes/` is tracked.

`patches/`, `patches/output/`, and `prompt/` are local-only.

---

## Release Discipline

Release work must be conservative.

Before a public release:

- freeze the approved candidate;
- verify application identity;
- verify versioning;
- configure signing safely;
- verify the signed release build;
- verify release artifact metadata;
- verify installation and persistence;
- verify runtime behavior on supported devices;
- verify documentation;
- audit current repository content and history for secrets or private data;
- create the release tag only after acceptance.

Do not add unrelated features during release cutover or stabilization.