Discovery menunjukkan active docs yang relevan adalah `MIGRATION_PLAN.md`, `MIGRATION_ROADMAP.md`, dan `MIGRATION_STATUS.md`; tidak ada active `CHANGELOG.md`/README dalam workspace yang ditemukan, jadi command ini tidak akan menebak atau menyentuh file legacy.

Jalankan **satu paket ini dari root `C:\laragon\www\nexplay`**. Ia akan verify → update docs → diff check → stage hanya PHASE 13 + docs → commit → push → final status.

```powershell
$ErrorActionPreference = "Stop"

$utf8 = New-Object System.Text.UTF8Encoding($false)
$fence = [string]::new([char]96, 3)

function Normalize-Lf {
    param([string]$Text)

    return $Text.Replace("`r`n", "`n")
}

function Read-Lf {
    param([string]$Path)

    return Normalize-Lf(
        [System.IO.File]::ReadAllText($Path)
    )
}

function Write-Lf {
    param(
        [string]$Path,
        [string]$Content
    )

    [System.IO.File]::WriteAllText(
        $Path,
        (Normalize-Lf $Content),
        $utf8
    )
}

function Replace-Required {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Replacement,
        [string]$Label
    )

    $regex = New-Object System.Text.RegularExpressions.Regex(
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )

    if (-not $regex.IsMatch($Text)) {
        throw "Required documentation block not found: $Label"
    }

    return $regex.Replace(
        $Text,
        (Normalize-Lf $Replacement),
        1
    )
}

$requiredDocs = @(
    "MIGRATION_PLAN.md",
    "MIGRATION_ROADMAP.md",
    "MIGRATION_STATUS.md"
)

foreach ($path in $requiredDocs) {
    if (-not (Test-Path $path)) {
        throw "Required documentation file not found: $path"
    }
}

Write-Host ""
Write-Host "=== PHASE 13 VERIFICATION ==="

.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin testDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    throw "testDebugUnitTest failed."
}

.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin lintDebug
if ($LASTEXITCODE -ne 0) {
    throw "lintDebug failed."
}

.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw "assembleDebug failed."
}

Write-Host ""
Write-Host "=== UPDATE MIGRATION_PLAN.md ==="

$plan = Read-Lf "MIGRATION_PLAN.md"

$newVideoSubsystem = @"
# 11. Subsystem 07 — Video Player

## Fungsi

Video Player menangani presentation dan interaction untuk canonical `VideoItem` tanpa memiliki playback runtime sendiri.

## Tanggung Jawab

- video rendering surface;
- portrait playback;
- landscape fullscreen;
- orientation;
- system UI;
- controls;
- progress dan seek;
- previous / next;
- playback error presentation;
- Video → Audio Only handoff;
- gesture interaction;
- Child Lock presentation.

## Current Native Video Player Contract (PHASE 13)

PHASE 13 menetapkan native Video Player berbasis Media3 `PlayerView` di atas playback architecture yang sudah tersedia.

Current native contract:

- Video Player menggunakan Media3 Player yang berasal dari `QueuePlaybackCoordinator`;
- Video Player tidak membuat ExoPlayer kedua;
- Video Player tidak memiliki queue kedua atau hidden playback state;
- video yang dipilih dari Folder Detail membentuk canonical video queue dengan `FolderContext`;
- playback portrait tersedia;
- landscape fullscreen tersedia;
- fullscreen mempertahankan playback runtime yang sama;
- orientation change tidak membuat playback source baru;
- video surface menggunakan background hitam;
- status/navigation system-bar area menyatu dengan Video Player;
- controls auto-hide ketika playback berjalan;
- pada normal Video Player, status bar mengikuti visibility controls;
- single tap menampilkan atau menyembunyikan controls;
- double tap sisi kiri melakukan seek backward 10 detik;
- double tap area tengah melakukan play/pause;
- double tap sisi kanan melakukan seek forward 10 detik;
- seek dibatasi pada range media yang valid;
- previous dan next mengikuti canonical Queue Engine;
- Video → Audio Only dilakukan tanpa mengganti queue atau restart playback;
- Audio Only untuk `VideoItem` dapat diminimize menjadi Audio Mini Player;
- Audio Only → Video mengembalikan presentation ke Video Player tanpa membuat playback baru;
- direct Video Player exit menghentikan dan membersihkan playback queue sesuai current Video Player back contract;
- playback error dari Playback Engine ditampilkan sebagai Video Player error state;
- Child Lock tersedia dari menu Video Player;
- Child Lock memblokir player tap, double tap, transport controls, dan Android Back di dalam player;
- Child Lock menggunakan Android `startLockTask()` / Screen Pinning sebagai OS-level protection untuk aplikasi biasa;
- Child Lock bukan Device Owner / managed-device kiosk mode;
- Child Lock menggunakan persistent local 4-digit PIN;
- first use meminta Set PIN dan Confirm PIN;
- penggunaan berikutnya memakai PIN yang sudah tersimpan;
- unlock affordance membutuhkan hold sekitar 2 detik sebelum PIN verification muncul;
- PIN salah mempertahankan Child Lock;
- PIN benar keluar dari Child Lock dan menghentikan Lock Task jika sedang aktif;
- Change / Reset Child Lock PIN UI ditunda sampai Settings surface yang tepat tersedia;
- broad codec/device compatibility audit tetap menjadi PHASE 21;
- software decoder atau FFmpeg tidak ditambahkan pada PHASE 13.

## Acceptance Criteria

$fence
[x] Video play
[x] Seek
[x] Previous / Next
[x] Portrait playback
[x] Landscape fullscreen
[x] Orientation tidak membuat playback state hilang
[x] Controls auto-hide
[x] Single-tap controls toggle
[x] Double-tap -10 / play-pause / +10
[x] Video → Audio Only handoff
[x] Audio Only → Video handoff
[x] Video Audio Only → Audio Mini Player
[x] System-bar presentation menyatu dengan Video Player
[x] Playback error dapat ditampilkan
[x] Child Lock tersedia
[x] Persistent 4-digit Child Lock PIN tersedia
[x] Long-hold + PIN unlock tersedia
[x] Video Player bukan pemilik ExoPlayer
[x] Video Player bukan pemilik queue
[ ] Broad codec / old-device compatibility matrix
$fence

Broad codec compatibility bukan blocker PHASE 13 dan tetap menjadi scope PHASE 21.
"@

$plan = Replace-Required `
    -Text $plan `
    -Pattern '# 11\. Subsystem 07 — Video Player\n.*?(?=\n# 12\. Subsystem 08 — Folders)' `
    -Replacement $newVideoSubsystem `
    -Label "Subsystem 07 — Video Player"

$plan = $plan.Replace(
    "Video rendering surface tetap menjadi PHASE 13.",
    "Native Video Player UI selesai dan terverifikasi pada PHASE 13."
)

$plan = $plan.Replace(
    "Video rendering surface tetap menjadi scope PHASE 13.",
    "Native Video Player UI selesai dan terverifikasi pada PHASE 13."
)

Write-Lf "MIGRATION_PLAN.md" $plan

Write-Host ""
Write-Host "=== UPDATE MIGRATION_ROADMAP.md ==="

$roadmap = Read-Lf "MIGRATION_ROADMAP.md"

$newPhase13 = @"
# 20. Phase 13 — Video Player UI

**Status: COMPLETED + VERIFIED - 2026-09-04**

## Tujuan

Membuat native Video Player di atas canonical Queue Engine, Playback Engine, dan MediaSession runtime yang sama tanpa membuat ExoPlayer atau queue kedua.

## Implemented

- Media3 `PlayerView` video surface;
- canonical VideoItem folder queue;
- portrait playback;
- landscape fullscreen;
- orientation handling;
- black edge-to-edge Video Player presentation;
- system-bar integration;
- controls auto-hide;
- status bar mengikuti normal player controls;
- play/pause;
- previous;
- next;
- progress;
- interactive seek;
- double tap left = seek -10 seconds;
- double tap center = play/pause;
- double tap right = seek +10 seconds;
- playback error presentation;
- seamless Video → Audio Only handoff;
- seamless Audio Only → Video handoff;
- VideoItem Audio Only dapat diminimize menjadi Audio Mini Player;
- playing-title marquee;
- direct Video Player exit menghentikan playback sesuai current back contract;
- Child Lock;
- Android Screen Pinning melalui `startLockTask()`;
- player tap/double-tap/transport/Back diblokir ketika Child Lock aktif;
- persistent local 4-digit Child Lock PIN;
- first-use Set PIN + Confirm PIN;
- subsequent Child Lock activation menggunakan stored PIN;
- unlock membutuhkan hold sekitar 2 detik;
- setelah hold, user harus memasukkan PIN;
- incorrect PIN mempertahankan Child Lock;
- correct PIN keluar dari Child Lock dan menghentikan active Lock Task.

## Architecture

$fence
Folder Detail
      ↓
QueuePlaybackCoordinator
      ├── QueueEngine
      └── PlaybackEngine
              ↓
         Same ExoPlayer
              ↓
       Media3 PlayerView
              ↓
       VideoPlayerScreen
$fence

Ownership contract:

- Queue Engine tetap authoritative untuk queue/current-index/shuffle/repeat;
- Playback Engine tetap authoritative untuk ExoPlayer dan runtime playback;
- Video Player hanya presentation/interaction layer;
- Audio Only tidak membuat source atau queue baru;
- MediaSession tetap menggunakan playback runtime yang sama;
- Child Lock tidak mengubah playback architecture.

## Acceptance

$fence
[x] Video play
[x] Seek
[x] Previous / Next
[x] Fullscreen
[x] Rotation/orientation
[x] Playback state tidak hilang
[x] Decoder/runtime error dapat ditampilkan
[x] Controls auto-hide
[x] Double-tap seek/play-pause
[x] Video ↔ Audio Only
[x] Video Audio Only → Audio Mini Player
[x] Child Lock
[x] Persistent Child Lock PIN
[x] Long-hold + PIN unlock
[x] Player tetap menggunakan canonical ExoPlayer
[x] Player tetap menggunakan canonical Queue Engine
$fence

## Verification

$fence
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] Manual runtime Video Player verification pada physical Android device
[x] Portrait playback
[x] Landscape fullscreen
[x] Video → Audio Only
[x] Audio Only → Video
[x] Video Audio Only → Audio Mini Player
[x] Double-tap left / center / right behavior
[x] Child Lock flow
[x] First-use PIN setup + confirmation
[x] Long-hold unlock flow
[x] Incorrect PIN tetap locked
[x] Correct PIN unlock flow
$fence

## Known Limitation

- Android Screen Pinning bukan true managed-device kiosk / Device Owner mode.
- System-level escape behavior tetap mengikuti Android/OEM policy.
- Change / Reset Child Lock PIN UI ditunda sampai Settings surface tersedia.
- Broad codec compatibility dan old-device hardening tetap menjadi PHASE 21.
- Media3 playback masih bergantung pada decoder/capability perangkat untuk codec yang tidak universal.

## Checkpoint

$fence
feat(video): add native video player
$fence

---
"@

$roadmap = Replace-Required `
    -Text $roadmap `
    -Pattern '# 20\. Phase 13 — Video Player UI\n.*?(?=\n# 21\. Phase 14 — Search)' `
    -Replacement $newPhase13 `
    -Label "PHASE 13 — Video Player UI"

Write-Lf "MIGRATION_ROADMAP.md" $roadmap

Write-Host ""
Write-Host "=== UPDATE MIGRATION_STATUS.md ==="

$status = Read-Lf "MIGRATION_STATUS.md"

$status = Replace-Required `
    -Text $status `
    -Pattern '## Current Phase\n.*?(?=\n## Current Task)' `
    -Replacement @"
## Current Phase

PHASE 14 - Search
"@ `
    -Label "Current Phase"

$status = Replace-Required `
    -Text $status `
    -Pattern '## Current Task\n.*?(?=\n## Completed)' `
    -Replacement @"
## Current Task

Mempersiapkan PHASE 14 Search berdasarkan canonical MediaLibraryRepository dengan tetap mempertahankan folder playback context dan membuka player yang sesuai untuk AudioItem maupun VideoItem.
"@ `
    -Label "Current Task"

if (
    -not $status.Contains(
        "- PHASE 13 Video Player UI selesai dan terverifikasi."
    )
) {
    $status = $status.Replace(
        "- PHASE 12 Android MediaSession dan Background Audio selesai dan terverifikasi.",
        "- PHASE 12 Android MediaSession dan Background Audio selesai dan terverifikasi.`n- PHASE 13 Video Player UI selesai dan terverifikasi."
    )
}

if (
    -not $status.Contains(
        "### PHASE 13 - Implemented"
    )
) {
    $phase13Completed = @"
### PHASE 13 - Implemented

- Native Video Player tersedia untuk canonical `VideoItem`.
- Video Player menggunakan Media3 `PlayerView`.
- Video Player menggunakan Player/ExoPlayer yang sama dari existing playback runtime.
- Tidak ada ExoPlayer kedua atau hidden video playback engine.
- Video selection dari Folder Detail membentuk canonical video queue dengan FolderContext.
- Portrait playback tersedia.
- Landscape fullscreen tersedia.
- Orientation tidak membuat playback source baru.
- Video Player menggunakan seamless black edge-to-edge presentation.
- System-bar presentation menyatu dengan Video Player.
- Controls auto-hide ketika playback berjalan.
- Status bar mengikuti visibility normal player controls.
- Single tap melakukan show/hide controls.
- Double tap kiri melakukan seek backward 10 detik.
- Double tap tengah melakukan play/pause.
- Double tap kanan melakukan seek forward 10 detik.
- Previous / Next mengikuti canonical Queue Engine.
- Playback error ditampilkan melalui observable PlaybackState.
- Video → Audio Only handoff tersedia tanpa queue/playback restart.
- Audio Only → Video handoff tersedia tanpa queue/playback restart.
- VideoItem Audio Only dapat diminimize menjadi Audio Mini Player.
- Current playing title menggunakan running marquee pada area terkait.
- Direct Video Player exit menghentikan dan membersihkan playback sesuai current Video Player back contract.
- Child Lock tersedia pada Video Player.
- Child Lock memblokir player tap, double tap, transport controls, dan Android Back.
- Android Screen Pinning digunakan melalui `startLockTask()`.
- Persistent local 4-digit Child Lock PIN tersedia.
- First-use PIN setup meminta Set PIN dan Confirm PIN.
- PIN tersimpan secara lokal untuk penggunaan berikutnya.
- Unlock affordance membutuhkan hold sekitar 2 detik.
- Setelah hold, user harus memasukkan Child Lock PIN.
- Incorrect PIN mempertahankan Child Lock.
- Correct PIN keluar dari Child Lock dan menghentikan Lock Task bila aktif.
- Child Lock tetap merupakan parental convenience boundary, bukan true managed-device kiosk security.
"@

    $status = $status.Replace(
        "## In Progress",
        "$phase13Completed`n`n## In Progress"
    )
}

$status = Replace-Required `
    -Text $status `
    -Pattern '## In Progress\n.*?(?=\n## Blocked)' `
    -Replacement @"
## In Progress

- Persiapan PHASE 14 Search.
"@ `
    -Label "In Progress"

$status = Replace-Required `
    -Text $status `
    -Pattern '## Known Limitation\n.*?(?=\n## Deferred)' `
    -Replacement @"
## Known Limitation

- Real media artwork extraction/display belum dimigrasikan; Audio Player dan Android system media surfaces masih menggunakan NexPlay fallback identity.
- Add to Playlist pada Control Deck masih placeholder sampai native playlist persistence tersedia.
- Rhythm Analyzer masih placeholder.
- Share masih placeholder.
- Details masih placeholder.
- Playhub masih placeholder.
- Search masih placeholder.
- Playlist screen masih placeholder.
- Repository refresh masih synchronous dan harus tetap dipanggil di luar UI thread.
- Persistence/cached-library startup belum dimigrasikan.
- MediaSession/background behavior telah diverifikasi pada current physical Android development device; broad OEM/device compatibility matrix belum divalidasi.
- Child Lock menggunakan Android Screen Pinning untuk aplikasi biasa dan bukan true Device Owner / managed-device kiosk mode.
- System-level escape behavior Child Lock tetap mengikuti Android/OEM policy.
- Change / Reset Child Lock PIN UI belum tersedia karena dedicated Settings surface belum diimplementasikan.
- Broad codec compatibility dan old-device playback matrix belum diverifikasi.
- Media3 playback tetap bergantung pada decoder/capability device untuk codec yang tidak universal.
"@ `
    -Label "Known Limitation"

$status = Replace-Required `
    -Text $status `
    -Pattern '## Deferred\n.*?(?=\n## Next)' `
    -Replacement @"
## Deferred

- Real artwork integration.
- Playlist persistence.
- Rhythm Analyzer implementation.
- Share implementation.
- Details implementation.
- Playback speed.
- Haptic micro-interactions.
- Artwork-driven player accent.
- DataStore migration untuk theme preference.
- Change / Reset Child Lock PIN UI sampai Settings surface tersedia.
- True managed-device kiosk / Device Owner mode.
- Broad codec compatibility dan old-device hardening sampai PHASE 21.
"@ `
    -Label "Deferred"

$status = Replace-Required `
    -Text $status `
    -Pattern '## Next\n.*?(?=\n## Verification)' `
    -Replacement @"
## Next

- Implementasikan PHASE 14 Search.
- Gunakan canonical MediaLibraryRepository.
- Search minimum mencakup title, filename, artist, album, dan folder.
- Search harus case-insensitive.
- Result hanya media file.
- Audio result membuka Audio Player.
- Video result membuka Video Player.
- Pertahankan playback context sesuai contract; jangan menjadikan search result list sebagai hidden queue baru.
- Jangan membuat ExoPlayer, Queue Engine, scanner, atau repository kedua.
- Jangan memperluas scope ke Playlist, Playhub, History, Settings, atau codec hardening.
"@ `
    -Label "Next"

if (
    -not $status.Contains(
        "### PHASE 13 Final Verification"
    )
) {
    $phase13Verification = @"
### PHASE 13 Final Verification

- `testDebugUnitTest`: BUILD SUCCESSFUL.
- `lintDebug`: BUILD SUCCESSFUL.
- `assembleDebug`: BUILD SUCCESSFUL.
- Native Video Player dijalankan pada physical Android development device.
- Portrait video playback: PASS.
- Landscape fullscreen: PASS.
- Video → Audio Only seamless handoff: PASS.
- Audio Only → Video seamless handoff: PASS.
- Video Audio Only → Audio Mini Player: PASS.
- Double tap kiri seek -10 detik: PASS.
- Double tap tengah play/pause: PASS.
- Double tap kanan seek +10 detik: PASS.
- System-bar Video Player presentation diterima pada runtime.
- Child Lock runtime flow diterima.
- Persistent 4-digit PIN setup/confirmation flow diterima.
- Long-hold unlock + PIN verification flow diterima.
- Incorrect PIN mempertahankan Child Lock.
- Correct PIN keluar dari Child Lock.
- Broad old-device / codec compatibility belum termasuk verification PHASE 13.

"@

    $status = $status.Replace(
        "### PHASE 12 Final Verification",
        "$phase13Verification### PHASE 12 Final Verification"
    )
}

$status = Replace-Required `
    -Text $status `
    -Pattern '## Last Commit\n.*$' `
    -Replacement @"
## Last Commit

feat(video): add native video player - PHASE 13 checkpoint.
"@ `
    -Label "Last Commit"

Write-Lf "MIGRATION_STATUS.md" $status

Write-Host ""
Write-Host "=== DOCUMENTATION DIFF ==="

git diff -- `
    MIGRATION_PLAN.md `
    MIGRATION_ROADMAP.md `
    MIGRATION_STATUS.md

if ($LASTEXITCODE -ne 0) {
    throw "Documentation diff failed."
}

Write-Host ""
Write-Host "=== FINAL DIFF CHECK ==="

git diff --check
if ($LASTEXITCODE -ne 0) {
    throw "git diff --check failed."
}

$phase13Files = @(
    "MIGRATION_PLAN.md",
    "MIGRATION_ROADMAP.md",
    "MIGRATION_STATUS.md",
    "nexplay_kotlin/app/build.gradle.kts",
    "nexplay_kotlin/app/src/main/java/com/nexplay/app/playback/QueuePlaybackCoordinator.kt",
    "nexplay_kotlin/app/src/main/java/com/nexplay/app/ui/AudioPlayerScreen.kt",
    "nexplay_kotlin/app/src/main/java/com/nexplay/app/ui/FoldersScreen.kt",
    "nexplay_kotlin/app/src/main/java/com/nexplay/app/ui/NexPlayApp.kt",
    "nexplay_kotlin/app/src/main/java/com/nexplay/app/ui/NexPlayShell.kt",
    "nexplay_kotlin/app/src/main/java/com/nexplay/app/ui/VideoPlayerScreen.kt",
    "nexplay_kotlin/gradle/libs.versions.toml"
)

Write-Host ""
Write-Host "=== STAGE PHASE 13 ==="

git add -- $phase13Files
if ($LASTEXITCODE -ne 0) {
    throw "git add failed."
}

$stagedOutput =
    git diff --cached --name-only |
    Select-String -Pattern '^patches/output/'

if ($stagedOutput) {
    throw "patches/output must not be staged."
}

git diff --cached --check
if ($LASTEXITCODE -ne 0) {
    throw "git diff --cached --check failed."
}

Write-Host ""
Write-Host "=== STAGED STAT ==="

git diff --cached --stat
if ($LASTEXITCODE -ne 0) {
    throw "git diff --cached --stat failed."
}

Write-Host ""
Write-Host "=== STATUS BEFORE COMMIT ==="

git status
if ($LASTEXITCODE -ne 0) {
    throw "git status failed."
}

git diff --cached --quiet
if ($LASTEXITCODE -eq 0) {
    throw "No staged changes found."
}

Write-Host ""
Write-Host "=== COMMIT ==="

git commit -m "feat(video): add native video player"
if ($LASTEXITCODE -ne 0) {
    throw "git commit failed."
}

Write-Host ""
Write-Host "=== PUSH ==="

git push origin main
if ($LASTEXITCODE -ne 0) {
    throw "git push failed."
}

Write-Host ""
Write-Host "=== FINAL STATUS ==="

git status
if ($LASTEXITCODE -ne 0) {
    throw "final git status failed."
}

Write-Host ""
Write-Host "=== FINAL COMMIT ==="

git log -1 --oneline
if ($LASTEXITCODE -ne 0) {
    throw "git log failed."
}
```

Kalau seluruh rangkaian mencapai bagian **FINAL COMMIT** tanpa `throw`, maka PHASE 13 sudah terverifikasi oleh gate tersebut, dokumentasi tersinkron, commit dibuat, dan `main` sudah dipush.
