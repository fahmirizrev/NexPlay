# NexPlay Release Process

## Purpose

This document defines the ongoing release process for NexPlay.

Release work must remain conservative, evidence-driven, and limited to the
accepted candidate. Do not mix unrelated features or speculative refactors into
release cutover or stabilization.

## 1. Freeze the Release Candidate

Before release work begins:

- freeze the approved source candidate;
- stop unrelated feature work;
- confirm the working tree is clean;
- confirm the target version and release scope;
- review known limitations and release blockers;
- confirm the application identity remains `com.nexplay.app`.

Any source change after the freeze invalidates affected verification and must be
reviewed before the candidate proceeds.

## 2. Verify Versioning

Confirm the release candidate uses the intended:

- application ID;
- version code;
- version name;
- minimum, target, and compile SDK configuration;
- release notes and changelog entry.

Every public release must increment Android version metadata as appropriate.
The version in the built artifact, Git tag, changelog, and GitHub Release must
agree.

## 3. Configure Signing Safely

Production releases must use the stable NexPlay production signing identity.
Private signing material must never be committed to Git.

The release build reads its signing configuration from:

```text
NEXPLAY_RELEASE_STORE_FILE
NEXPLAY_RELEASE_STORE_PASSWORD
NEXPLAY_RELEASE_KEY_ALIAS
NEXPLAY_RELEASE_KEY_PASSWORD
```

`NEXPLAY_RELEASE_STORE_FILE` must point to the production keystore outside the
tracked repository. Passwords must exist only in an approved local or CI secret
environment and must never be written to tracked files, shell history, build
logs, or public documentation.

Run production signing tasks with Gradle configuration cache disabled. Clear
signing-password environment variables from the active shell after the signed
build is complete.

## 4. Run the Build Gate

Run commands from the repository root.

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin testDebugUnitTest
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin lintDebug
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebug
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebugAndroidTest
git diff --check
```

Verify the production signing configuration and build the signed release APK:

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin signingReport --no-configuration-cache
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleRelease --no-configuration-cache
```

The expected signed APK path is:

```text
nexplay_kotlin/app/build/outputs/apk/release/app-release.apk
```

A failed check blocks publication until the failure is understood and the
candidate is verified again.

## 5. Verify the Signed Artifact

Inspect the actual release APK rather than inferring release properties from a
debug build or source configuration.

Verify:

- package identity;
- version code and version name;
- production signature and certificate identity;
- installability on supported Android versions;
- upgrade behavior from the previous public release when applicable;
- persistence of playlists and application preferences across the upgrade;
- artifact file size and SHA-256 checksum.

Record the exact artifact and checksum selected for publication. Do not publish
an unverified rebuild under the same release evidence.

## 6. Run Physical-Device Verification

Install the signed release artifact on representative physical devices. Verify
the flows affected by the release and, for a full release gate, cover:

- application startup;
- media permission flow;
- Folders and Folder Detail;
- Search;
- PlayHub and Queue;
- playlists;
- Audio Player and Audio Mini Player;
- Video Player and fullscreen video;
- Child Lock;
- background playback;
- notification and lock-screen controls;
- headset or Bluetooth transport where available;
- Android Open With;
- theme persistence;
- playlist persistence;
- relevant preference persistence.

Document devices, Android versions, artifact identity, observations, and any
known limitation. Automated verification does not replace physical-device
runtime evidence.

## 7. Prepare Public Documentation

Before publication:

- add a factual entry to `CHANGELOG.md`;
- verify `README.md` requirements and download links;
- update affected active documents under `docs/`;
- prepare GitHub Release notes that match the accepted scope;
- confirm the version, tag, and artifact names are consistent;
- ensure the release notes do not claim unverified behavior.

`CHANGELOG.md` is cumulative release history. Update it for every public release;
do not recreate or replace earlier factual entries.

## 8. Audit Repository Hygiene

Audit the current tracked tree and relevant Git history for:

- API keys, passwords, and tokens;
- keystores and signing configuration;
- private personal information;
- local development context or generated output;
- licensing and attribution issues;
- unexpected or unrelated files in the release commit.

Confirm `notes/`, `patches/`, `patches/output/`, and `prompt/` remain local-only
and untracked. A security or privacy concern blocks publication until resolved.

## 9. Commit, Tag, and Publish

Only after release acceptance:

1. verify the final working tree and staged file list;
2. create the approved release commit;
3. verify the release commit in a clean checkout when practical;
4. create the version tag from the accepted release commit;
5. push the release commit and tag;
6. publish the GitHub Release for that tag;
7. attach the exact approved signed APK;
8. publish its SHA-256 checksum;
9. verify the public tag, source archive, release notes, APK, and checksum;
10. download the published APK and confirm it matches the approved artifact.

Do not move or recreate an accepted public tag silently. If publication evidence
is wrong, stop and correct it transparently.

## 10. Post-Release Verification

After publication:

- test the public download path;
- verify the published checksum;
- verify installation of the downloaded artifact;
- confirm the GitHub latest-release destination resolves to the new release;
- verify README and changelog links;
- record any confirmed release issue for stabilization.

## Stabilization

Stabilization changes must address reproducible regressions, security/privacy
issues, compatibility failures, or a broken release promise.

Do not use stabilization as an excuse for speculative refactoring or feature
expansion. Each fix must pass proportionate automated, signed-artifact, and
physical-device verification before a maintenance release is published.
