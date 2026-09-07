# MIGRATION_ROADMAP.md

# NexPlay Kotlin — Step-by-Step Migration Roadmap
## Pembuatan Ulang NexPlay dari Nol / From Scratch

> Roadmap ini berisi urutan implementasi NexPlay native Android menggunakan Kotlin.
> Pengerjaan menggunakan pendekatan **incremental, checkpoint-based, dan hybrid manual + agent** sehingga progress tidak hilang atau tumpang tindih.

---

# 1. Strategi Besar

Migrasi mengikuti alur:

```text
Flutter Production Baseline
          ↓
Kotlin Project Bootstrap
          ↓
Core Architecture
          ↓
Media Discovery
          ↓
Media Library
          ↓
Queue
          ↓
Playback
          ↓
Audio / Video Player
          ↓
Folders / Search / PlayHub / Playlist
          ↓
Android Integration
          ↓
Feature Parity
          ↓
Stabilization
          ↓
Production Cutover
          ↓
Post-Migration Enrichment
```

---

# 2. Mode Pengerjaan Hybrid

Setiap fase dapat dikerjakan:

```text
MANUAL
oleh developer

atau

AGENT
oleh coding agent

atau

HYBRID
agent implement → developer review/test
developer design → agent implement
```

Namun satu prinsip wajib berlaku:

> **Progress harus ditentukan oleh repository dan dokumen status, bukan oleh ingatan percakapan.**

---

# 3. Dokumen Kontrol Progress

Buat dan pertahankan:

```text
MIGRATION_PLAN.md
MIGRATION_ROADMAP.md
MIGRATION_STATUS.md
CHANGELOG.md
```

Direkomendasikan juga:

```text
docs/
├── ARCHITECTURE.md
├── DECISIONS.md
└── TEST_MATRIX.md

prompt/
├── tasks/
└── output/
```

---

# 4. Format Status

`MIGRATION_STATUS.md` disarankan mempunyai format:

```markdown
# Current Migration Status

## Current Phase
PHASE_XX — ...

## Current Task
TASK_XXX — ...

## Last Completed Task
TASK_XXX — ...

## Completed
- ...

## In Progress
- ...

## Blocked
- ...

## Next
- ...

## Verification
- ...

## Last Commit
<hash> <message>
```

Dengan ini manusia maupun agent dapat membaca state terbaru sebelum bekerja.

---

# 5. Aturan Task Hybrid

Setiap task harus memiliki:

```text
TASK ID
Objective
Scope
Allowed Files / Areas
Do Not Change
Implementation Requirements
Acceptance Criteria
Verification
Expected Output
```

Satu task idealnya menyentuh **satu subsystem atau satu capability**.

Jangan memberikan prompt:

```text
"Rewrite NexPlay ke Kotlin."
```

Gunakan:

```text
TASK_004
Implement MediaStore audio scanner.

TASK_005
Implement MediaStore video scanner.

TASK_006
Create canonical media repository.
```

---

# 6. Git Checkpoint Strategy

Setiap unit kerja harus memiliki checkpoint.

Contoh:

```text
feat(kotlin): bootstrap nexplay native project
feat(media): add audio MediaStore scanner
feat(media): add video MediaStore scanner
feat(queue): implement queue state and navigation
feat(playback): add Media3 playback engine
feat(player): add native audio player
```

Jangan menunggu satu fase besar selesai sebelum commit.

---

# 7. Phase 00 — Freeze dan Audit Flutter Baseline

## Tujuan

Menetapkan `nexplay_flutter` sebagai baseline resmi.

## Langkah

1. pastikan working tree Flutter bersih;
2. jalankan verification existing project;
3. catat commit baseline;
4. buat tag migration baseline;
5. audit seluruh feature;
6. audit seluruh plugin;
7. audit navigation;
8. audit player behavior;
9. audit queue behavior;
10. audit database/persistence;
11. audit search;
12. audit Folders;
13. audit PlayHub;
14. audit video behavior;
15. audit Android native integration yang sudah ada.

## Output

```text
docs/FLUTTER_BASELINE_AUDIT.md
```

## Parity-Critical Behavior yang Terkonfirmasi

Audit Flutter menemukan existing behavior yang harus dipertahankan selama migrasi:

```text
A-B Repeat
Playback speed cycle
Playback Snapshot restore
Continue Playing
Recently Played
Video audio-only handoff
Floating video mini player
Android Picture-in-Picture
Android Open With / ACTION_VIEW
Cached-library-first startup
Automatic media refresh after app resume
Internal theme persistence
Folder-context playback from Search
Mixed audio/video folder queue
Top-level vs nested Back behavior
```

Detail kontrak behavior dan known limitation dicatat di `docs/FLUTTER_BASELINE_AUDIT.md`.

## Acceptance

```text
[x] Baseline commit diketahui
[x] Existing feature list lengkap
[x] Known issues dicatat
[x] Existing behavior dicatat
[x] Flutter tidak lagi menjadi moving target selama core migration
```

---

# 8. Phase 01 — Bootstrap `nexplay_kotlin`

## Tujuan

Membuat project native baru dari nol.

## Stack

```text
Kotlin
Jetpack Compose
Material 3
Navigation Compose
Coroutines
StateFlow
ViewModel
```

## Langkah

1. buat project `nexplay_kotlin`;
2. gunakan application ID com.nexplay.app selama migrasi;
3. pertahankan Flutter pada com.nexplay.media.app agar kedua aplikasi dapat coexist;
4. setup Gradle;
5. setup Compose;
6. setup basic lint/testing;
7. setup directory/package baseline;
8. pastikan project build.

## Acceptance

```text
[x] App dapat build
[x] App dapat launch
[x] Flutter dan Kotlin debug dapat coexist
[x] Package structure awal tersedia
[x] No unnecessary dependency
```

## Checkpoint

```text
feat(kotlin): bootstrap native NexPlay project
```

---

# 9. Phase 02 — App Shell, Theme, dan Navigation

## Tujuan

Membuat skeleton UI native sebelum media logic.

## Implementasi

- NexPlay theme;
- typography dan color system;
- dynamic Material color dinonaktifkan agar identitas warna NexPlay konsisten;
- edge-to-edge system UI;
- status bar dan Android navigation/gesture area mengikuti background aplikasi;
- root scaffold;
- flat bottom navigation tanpa elevation, pill, atau visual noise;
- primary root navigation:
  - Folders;
  - Playhub;
- Navigation Compose route graph:
  - Folders;
  - Playhub;
  - Search;
  - Playlist;
  - Player;
- Search, Playlist, dan Player merupakan nested route dan menyembunyikan bottom bar;
- Player memiliki route terpisah;
- Navigation Compose `2.9.8` digunakan untuk mempertahankan `minSdk 23`;
- Home tidak digunakan sebagai root karena baseline NexPlay menggunakan Folders dan Playhub;
- Settings deferred sampai benar-benar diperlukan.

## Motion Contract

```text
Folders → Playhub
SLIDE LEFT

Playhub → Folders
SLIDE RIGHT

Folders/Playhub → Search
INSTANT

Search → Back
INSTANT

Playhub → Playlist
PUSH UP

Playlist → Back
PUSH DOWN

Folders/Playhub → Player
PUSH UP

Player → Back
PUSH DOWN
```

Motion menggunakan durasi 200 ms tanpa fade atau scale.

## Back Navigation Contract

```text
Nested route → Back
kembali ke parent

Playhub root → Back
kembali ke Folders

Folders root → Back pertama
tampilkan "Tap back again to exit NexPlay"

Back kedua setelah debounce 700 ms
dan masih dalam window 2 detik
keluar dari aplikasi
```

## Acceptance

```text
[x] Semua route yang diperlukan dapat dibuka
[x] Bottom bar bekerja
[x] Back navigation benar
[x] Player route terpisah
[x] Theme konsisten
[x] System navigation area menyatu dengan app pada device verification
[x] Motion navigation sesuai contract
[x] Tidak ada MediaStore/ExoPlayer di UI
```

## Verification

```text
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] installDebug
[x] monkey runtime launch
[x] manual route verification
[x] manual motion verification
[x] manual Android Back verification
[x] manual double-back-to-exit verification
```

## Checkpoint

```text
feat(ui): add app shell theme and navigation
```
---

# 10. Phase 03 — Canonical Media Model

## Tujuan

Menentukan representasi media yang digunakan seluruh aplikasi.

## Implementasi

Definisikan:

```text
MediaItem
AudioItem
VideoItem
MediaType
FolderInfo
```

Field minimum:

```text
id
uri
displayName
title
duration
mimeType
folder
size
dateAdded
dateModified
```

Audio tambahan:

```text
artist
album
albumId
track
```

Video tambahan:

```text
width
height
resolution
```

## Acceptance

```text
[x] Satu canonical model tersedia
[x] Audio/video dapat dibedakan
[x] Model tidak tergantung UI
[x] Model cocok untuk scanner, queue, player, search, playlist
```

## Verification

```text
[x] AudioItem dan VideoItem mengimplementasikan MediaItem
[x] MediaType ditentukan secara konsisten oleh concrete media type
[x] Video resolution diturunkan dari width dan height
[x] Domain model tidak bergantung pada Compose
[x] Domain model tidak bergantung pada MediaStore
[x] Domain model tidak bergantung pada Media3/ExoPlayer
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] git diff --check
```

## Checkpoint

```text
feat(core): add canonical media models
```

---

# 11. Phase 04 — Media Scanner: Audio

## Tujuan

Membaca audio menggunakan Android MediaStore.

## Implementasi

- permission;
- `ContentResolver`;
- query audio;
- URI;
- metadata dasar;
- folder path/bucket;
- invalid row handling.

## Acceptance

```text
[x] Audio lokal terbaca
[x] No duplicate
[x] Deleted/stale/invalid item tidak membuat scan crash
[x] Permission Android modern ditangani
[x] Scanner tidak punya dependency UI
```

## Verification

```text
[x] API 23-32 menggunakan READ_EXTERNAL_STORAGE
[x] API 33+ menggunakan READ_MEDIA_AUDIO
[x] MediaStore.Audio query diimplementasikan melalui ContentResolver
[x] Stable content URI dibentuk untuk setiap item
[x] Audio metadata dipetakan ke canonical AudioItem
[x] Invalid row dilewati tanpa menggagalkan seluruh scan
[x] Duplicate stable ID dideduplicate
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] installDebug
[x] READ_MEDIA_AUDIO granted pada Android 15 verification device
[x] AudioMediaScannerInstrumentedTest pada physical device
[x] git diff --check
```

## Checkpoint

```text
feat(media): add native audio MediaStore scanner
```

---

# 12. Phase 05 — Media Scanner: Video

## Tujuan

Membaca video lokal.

## Implementasi

- MediaStore Video;
- URI;
- duration;
- dimensions;
- MIME;
- folder;
- invalid row handling.

## Acceptance

```text
[x] Video lokal terbaca
[x] Metadata dasar tersedia
[x] Invalid/broken row aman
[x] Audio/video scanner punya pola konsisten
```

## Verification

```text
[x] API 23-32 menggunakan READ_EXTERNAL_STORAGE
[x] API 33+ menggunakan READ_MEDIA_VIDEO
[x] MediaStore.Video query diimplementasikan melalui ContentResolver
[x] Stable content URI dan stable ID tersedia
[x] Video metadata dipetakan ke canonical VideoItem
[x] Width dan height dipetakan dan resolution diturunkan dari dimensions
[x] Invalid row dilewati tanpa menggagalkan seluruh scan
[x] Duplicate stable ID dideduplicate
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] installDebug
[x] READ_MEDIA_VIDEO granted pada Android 15 verification device
[x] VideoMediaScannerInstrumentedTest pada physical device
[x] app reinstall setelah instrumented test cleanup
[x] MainActivity cold launch pada physical Android 15 device
[x] runtime process aktif setelah launch
[x] tidak ada FATAL EXCEPTION atau ANR pada runtime smoke check
[x] git diff --check
```

## Checkpoint

```text
feat(media): add native video MediaStore scanner
```

---

# 13. Phase 06 — Media Library / Repository

## Tujuan

Membentuk canonical data access layer.

## API Minimum

```text
getAllMedia()
getAudio()
getVideos()
getFolders()
getMediaInFolder()
getRecentlyAdded()
getByUri()
refresh()
```

## Implementasi

- scanner aggregation;
- StateFlow/library state;
- refresh;
- sorting;
- deduplication;
- stale media handling.

## Acceptance

```text
[x] Fitur lain tidak perlu mengakses MediaStore
[x] Media library dapat refresh
[x] Audio/video tersedia dalam satu repository
[x] Folder grouping tersedia
```

## Verification

```text
[x] AudioMediaScanner dan VideoMediaScanner diagregasikan
[x] StateFlow<MediaLibraryState> tersedia
[x] getAllMedia()
[x] getAudio()
[x] getVideos()
[x] getFolders()
[x] getMediaInFolder()
[x] getRecentlyAdded()
[x] getByUri()
[x] refresh()
[x] canonical library deduplicate berdasarkan URI
[x] default media sorting tersedia
[x] folder grouping berdasarkan canonical FolderInfo
[x] successful refresh menghapus stale item
[x] temporary scanner failure mempertahankan last known items
[x] permission denied mengosongkan media type yang tidak dapat diakses
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] MediaLibraryRepositoryInstrumentedTest pada physical Android 15 device
[x] real audio dan video diagregasikan melalui production repository wiring
[x] app reinstall setelah instrumented test cleanup
[x] MainActivity cold runtime launch
[x] tidak ada FATAL EXCEPTION atau ANR
[x] git diff --check
```

## Checkpoint

```text
feat(media): add canonical media repository
```

---

# 14. Phase 07 — Folders Basic

## Tujuan

Membuat feature pertama yang menggunakan canonical Media Library dan membentuk native presentation foundation untuk pengembangan UI berikutnya.

## Implementasi

- Folders terhubung ke MediaLibraryRepository;
- initial library refresh dijalankan di luar UI thread;
- canonical MediaLibraryState di-observe oleh Compose;
- list folder;
- audio/video count per folder;
- Folder Detail untuk menampilkan contents;
- folder sorting:
  - Folder name;
  - Media count;
  - Date modified;
- media sorting:
  - Name;
  - Date modified;
  - Type;
  - Size;
  - Duration;
- pull-to-refresh;
- loading state;
- permission-denied state;
- scan-failure state;
- empty state;
- state-driven native presentation shell:
  - Root Layer;
  - Nested Layer;
  - Overlay Layer;
  - Vertical Layer;
  - Bottom Bar Layer;
- Folders dan Playhub tetap composed selama root-tab transition;
- Folders → Playhub menggunakan full slide left;
- Playhub → Folders menggunakan full slide right;
- Folder Detail menggunakan independent hierarchical motion;
- Search menggunakan reversible top-overlay motion;
- bottom bar bergerak independen tanpa me-resize root content;
- flat native color system dengan restrained accent;
- restrained shape system dan minimal elevation;
- shared list-row geometry antara Folders dan Folder Detail;
- folder icon menggunakan optical scaling tanpa mengubah leading-slot geometry;
- leading media/folder visual menggunakan balanced screen-edge spacing;
- Chevron, MoreVert, dan header trailing action menggunakan shared trailing optical axis;
- Navigation Compose dependency dihapus setelah tidak lagi direferensikan;
- generated Android Studio example tests tanpa product coverage dihapus;
- UI tidak mengakses MediaStore secara langsung.

## Acceptance

```text
[x] Folder list sesuai canonical library
[x] Folder contents benar
[x] Audio/video count per folder tersedia
[x] Folder sorting tersedia
[x] Media sorting tersedia
[x] Pull-to-refresh tersedia
[x] Loading state tersedia
[x] Permission-denied state tersedia
[x] Scan-failure state tersedia
[x] Empty state tersedia
[x] Tidak ada scanning langsung dari screen
[x] Folder Detail dapat dibuka dan kembali ke Folders
[x] Folders ↔ Playhub memiliki directional root motion yang konsisten
[x] Search memiliki reversible top-overlay motion
[x] Bottom bar memiliki independent enter/exit motion
[x] Folder dan media row menggunakan shared geometry
[x] Leading dan trailing optical spacing seimbang
[x] Chevron dan MoreVert berada pada shared trailing axis
[x] Native flat visual direction diterapkan
[x] Unused native dependency/source yang terbukti obsolete dibersihkan
```

## Verification

```text
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] git diff --check
[x] installDebug
[x] READ_MEDIA_AUDIO granted
[x] READ_MEDIA_VIDEO granted
[x] MainActivity cold launch
[x] runtime process aktif
[x] tidak ada FATAL EXCEPTION atau ANR pada runtime smoke check
[x] media lokal tampil pada Folders
[x] Folder Detail menampilkan media lokal
[x] Folders/Folder Detail row geometry diverifikasi secara visual
[x] trailing optical alignment diverifikasi secara visual
[x] final UI polish diterima pada physical Android device
```

## Checkpoint

```text
feat(folders): add native folder browsing
```

---


# 15. Phase 08 — Queue Engine

## Tujuan

Membangun pure domain Queue Engine sebelum player dan Media3 playback.

## Implementasi

State:

```text
items
currentIndex
currentItem
shuffleEnabled
repeatMode
playbackContext
```

Operation:

```text
setQueue()
playAt()
next()
previous()
append()
insertNext()
remove()
move()
clear()
toggleShuffle()
setRepeat()
```

Playback Context:

```text
FolderContext
PlaylistContext
SearchContext
PlayHubContext
SingleItemContext
```

Architecture:

- `QueueEngine` berada di pure domain layer;
- state observable melalui `StateFlow<QueueState>`;
- `currentItem` diturunkan dari `items/currentIndex`;
- canonical audio dan video dapat berada dalam queue yang sama;
- queue tidak mengontrol playback;
- queue tidak memiliki dependency ke Media3 atau ExoPlayer;
- queue tidak memiliki dependency ke Compose atau Android UI;
- queue tidak menangani persistence pada phase ini;
- queue mutation mempertahankan current item bila masih tersedia;
- explicit mutation saat shuffle aktif membatalkan stale original-order restore;
- `clear()` mempertahankan shuffle/repeat preference;
- Repeat One disimpan sebagai mode tetapi completion behavior ditunda ke PHASE 10.

## Acceptance

```text
[x] Next/previous benar
[x] Current index konsisten
[x] Current item selalu sinkron dengan items/currentIndex
[x] Mixed audio/video queue didukung
[x] Queue replacement aman
[x] playAt() aman
[x] append() tersedia
[x] insertNext() tersedia
[x] remove() mempertahankan current item bila memungkinkan
[x] move() mempertahankan current item
[x] clear() membersihkan queue dan playback context
[x] Shuffle mempertahankan current item
[x] Disable shuffle mengembalikan original order ketika valid
[x] Repeat ALL melakukan manual boundary wrapping
[x] Repeat ONE tidak mengambil alih manual next/previous
[x] FolderContext tersedia untuk folder-origin queue
[x] PlaylistContext tersedia
[x] SearchContext tersedia
[x] PlayHubContext tersedia
[x] SingleItemContext tersedia
[x] Queue Engine tidak terhubung langsung ke ExoPlayer
[x] Unit test core queue tersedia
```

## Verification

```text
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] compiler warning-free
[x] git diff --check
[x] installDebug
[x] READ_MEDIA_AUDIO granted
[x] READ_MEDIA_VIDEO granted
[x] MainActivity cold launch
[x] runtime process aktif
[x] tidak ada FATAL EXCEPTION atau ANR pada runtime smoke check
```

## Checkpoint

```text
feat(queue): implement native queue engine
```

---


# 16. Phase 09 — Playback Engine

## Tujuan

Membangun media playback terpisah dari UI dan Queue Engine.

## Teknologi

```text
Android Media3
ExoPlayer
StateFlow
```

## Implementasi

- `PlaybackEngine` menjadi single/clear owner satu private ExoPlayer;
- canonical `MediaItem` menjadi single-item playback input;
- audio dan video menggunakan satu runtime playback engine;
- `PlaybackState` tersedia sebagai immutable observable state;
- state diekspos melalui `StateFlow<PlaybackState>`;
- playback status:
  - IDLE;
  - PREPARING;
  - BUFFERING;
  - READY;
  - ENDED;
  - ERROR;
- `prepare()`;
- `play()`;
- `pause()`;
- `stop()`;
- `seek()`;
- current position;
- duration;
- buffering state;
- playback error mapping;
- MediaStore `content://` source handling;
- URI berscheme lain;
- file-path fallback;
- periodic position update hanya saat playback aktif;
- explicit `release()` lifecycle boundary;
- invalid source menjadi observable SOURCE error;
- Media3 runtime failure menjadi observable RUNTIME error;
- raw ExoPlayer tidak diekspos ke UI;
- Playback Engine tidak memiliki queue/current-index/next/previous logic;
- completion → queue integration tetap ditunda ke PHASE 10;
- MediaSession/background playback tetap ditunda ke PHASE 12;
- video rendering surface tetap ditunda ke native Video Player phase.

## Acceptance

```text
[x] Audio play pada engine level
[x] Video play pada engine level
[x] Seek
[x] Pause/resume
[x] Playback state observable
[x] Buffering/runtime state observable
[x] Position updates tersedia
[x] Duration tersedia
[x] Invalid source menjadi error state
[x] Runtime playback error tidak crash app
[x] ExoPlayer ownership tunggal/jelas
[x] Playback Engine tidak memiliki hidden queue logic
[x] Playback Engine tidak diekspos langsung ke UI
```

## Verification

```text
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] git diff --check
[x] installDebug
[x] connectedDebugAndroidTest
[x] 6 instrumentation tests passed
[x] audio prepare/play/pause/seek pada physical Android device
[x] video prepare/play/pause pada engine level
[x] invalid source menghasilkan SOURCE error state
[x] reinstall setelah instrumentation test cleanup
[x] READ_MEDIA_AUDIO granted
[x] READ_MEDIA_VIDEO granted
[x] MainActivity cold launch
[x] runtime process aktif
[x] tidak ada FATAL EXCEPTION
[x] tidak ada ANR
```

## Known Limitation

- PHASE 09 belum menghubungkan Queue Engine ke Playback Engine;
- video engine belum memiliki visual rendering surface;
- Audio Player dan Video Player UI belum menggunakan Playback Engine;
- MediaSession dan background playback belum diimplementasikan.

## Checkpoint

```text
feat(playback): add Media3 playback engine
```

---

# 17. Phase 10 — Queue + Playback Integration

## Tujuan

Menghubungkan queue dan playback tanpa membuat keduanya saling tumpang tindih.

## Architecture

- `QueueEngine` tetap menjadi owner queue state dan queue policy;
- `PlaybackEngine` tetap menjadi owner ExoPlayer dan runtime playback;
- `QueuePlaybackCoordinator` menjadi explicit integration boundary;
- coordinator tidak menyimpan hidden queue;
- coordinator tidak memindahkan repeat/shuffle policy ke Playback Engine;
- canonical mixed audio/video `MediaItem` tetap menjadi source queue dan playback.

## Behavior

```text
Queue selects item
↓
QueuePlaybackCoordinator
↓
Playback prepares/plays item

Playback reaches ENDED
↓
Coordinator validates completion belongs to current queue item
↓
Queue resolves completion action
↓
STOP / REPLAY_CURRENT / ADVANCED
↓
Playback follows queue decision
```

## Completion Policy

- Repeat OFF:
  - advance selama masih ada next item;
  - stop pada final boundary;
- Repeat ONE:
  - replay current item;
  - current index tidak berubah;
- Repeat ALL:
  - wrap pada final boundary;
  - single-item queue replay current item;
- manual next/previous tetap menggunakan Queue Engine navigation contract;
- shuffle order dan current-item preservation tetap dimiliki Queue Engine.

## Race-Safety Contract

Verification PHASE 10 menemukan race condition ketika stale `ENDED` callback dari source sebelumnya dapat memajukan queue setelah manual `previous()` mengganti current item.

Fix yang diterapkan:

- completion hanya diproses jika `PlaybackState.currentMedia` masih sama dengan `QueueState.currentItem`;
- stale `ENDED` dari source sebelumnya diabaikan;
- mixed audio/video navigation test dipertahankan sebagai regression coverage.

## Acceptance

```text
[x] Queue menentukan item
[x] Playback tidak punya hidden queue logic
[x] End-of-track behavior benar
[x] Next/previous sinkron
[x] Repeat sinkron
[x] Shuffle policy tetap dimiliki Queue Engine
[x] Mixed audio/video queue sinkron dengan playback
[x] Repeat OFF completion advance/stop benar
[x] Repeat ONE replay tanpa mengubah current index
[x] Repeat ALL completion wrap benar
[x] Single-item Repeat ALL replay benar
[x] Stale completion tidak membatalkan current navigation
[x] Queue dan Playback Engine tetap memiliki responsibility terpisah
```

## Verification

```text
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] git diff --check
[x] Queue completion policy unit tests
[x] installDebug
[x] connectedDebugAndroidTest
[x] 9 instrumentation tests passed
[x] mixed audio → video → audio navigation synchronized
[x] actual short-media ENDED advances queue
[x] Repeat ONE actual replay verified
[x] stale ENDED race reproduced by regression test and fixed
[x] READ_MEDIA_AUDIO granted
[x] READ_MEDIA_VIDEO granted
[x] app reinstall after instrumentation
[x] MainActivity cold launch
[x] runtime process active
[x] no FATAL EXCEPTION
[x] no ANR
```

## Known Limitation

- PHASE 10 belum menyediakan Audio Player UI;
- presentation layer belum menyediakan user-facing media tap → playback flow;
- video belum memiliki rendering surface;
- MediaSession/background playback belum diimplementasikan.

## Checkpoint

```text
feat(playback): integrate queue with playback engine
```

---

# 18. Phase 11 — Audio Player UI

**Status: COMPLETED + VERIFIED - 2026-09-02**

## Tujuan

Membuat native audio player.

## Implemented

- Full Audio Player;
- Audio Mini Player;
- title, artist, dan album metadata;
- NexPlay artwork fallback;
- elapsed/duration;
- interactive seek;
- animated wavy active progress;
- straight dimmed remaining progress;
- play/pause;
- previous;
- next;
- shuffle;
- Repeat OFF / ONE / ALL;
- real A-B Repeat;
- queue access;
- Add to Playlist placeholder;
- NexPlay Control Deck;
- swipe-down Full Player minimize;
- tap Mini Player expand;
- swipe-right Mini Player stop;
- runtime media permission UX;
- persisted SYSTEM / LIGHT / DARK theme selection;
- Sora bundled application typography;
- Rhythm Analyzer placeholder;
- Share placeholder;
- Details placeholder.

## Acceptance

```
[x] UI selalu mengikuti playback state
[x] Seek akurat
[x] Next/previous mengikuti queue
[x] Artwork fallback bekerja
[x] Player UI bukan pemilik ExoPlayer
[x] Player UI tidak memiliki hidden queue
[x] Shuffle/repeat mengikuti QueueEngine
[x] Real A-B Repeat bekerja
[x] Full/Mini Player choreography bekerja
[x] Runtime permission flow tersedia
[x] Internal theme selection persisted
[x] Sora typography terintegrasi
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] connectedDebugAndroidTest PASS
[x] 11 instrumentation tests PASS pada Android 15 physical device
```

## Known Limitation

- Add to Playlist masih placeholder sampai native playlist persistence tersedia.
- Real artwork belum dimigrasikan.
- Rhythm Analyzer masih placeholder.
- Share masih placeholder.
- Details masih placeholder.
- MediaSession/background playback tetap menjadi PHASE 12.

## Checkpoint

```
feat(player): add native audio player
```

---

# 19. Phase 12 — Android MediaSession dan Background Audio

**Status: COMPLETED + VERIFIED - 2026-09-03**

## Tujuan

Mengintegrasikan player dengan Android OS tanpa membuat queue atau playback runtime kedua.

## Implemented

- Media3 Session dependency;
- process-level playback ownership melalui `NexPlayApplication`;
- satu canonical `QueuePlaybackCoordinator`;
- satu canonical `QueueEngine`;
- satu `PlaybackEngine`;
- satu ExoPlayer;
- `MediaSession`;
- `MediaSessionService`;
- non-authoritative `NexPlaySessionPlayer` adapter;
- canonical queue exposure ke Android system surfaces;
- media notification;
- background playback;
- foreground media-playback service;
- notification Previous / Play-Pause / Next;
- lockscreen Previous / Play-Pause / Next;
- lockscreen seek;
- headset media controls;
- Bluetooth media controls;
- audio focus handling;
- audio-becoming-noisy handling;
- safe pause ketika wired audio output terputus;
- canonical title/artist/duration metadata untuk system media surfaces;
- stable Previous / Play-Pause / Next system transport layout.

## Architecture

```text
Android OS
Notification
Lockscreen
Headset
Bluetooth
        ↓
MediaSessionService
        ↓
NexPlaySessionPlayer
(non-authoritative adapter)
        ↓
QueuePlaybackCoordinator
        ├── QueueEngine
        └── PlaybackEngine
                ↓
           Same ExoPlayer
```

Ownership contract:

- QueueEngine tetap authoritative untuk queue/current-index/shuffle/repeat;
- PlaybackEngine tetap authoritative untuk ExoPlayer dan runtime playback;
- QueuePlaybackCoordinator tetap menjadi queue/playback integration boundary;
- MediaSession tidak memiliki hidden queue;
- MediaSession tidak memiliki duplicate playback state;
- service tidak membuat ExoPlayer kedua;
- UI dan Android system controls menggunakan playback runtime yang sama.

## Acceptance

```text
[x] Background audio
[x] Notification control
[x] Lockscreen control
[x] Bluetooth control
[x] Headset button
[x] Audio focus
[x] Unplug headset safe
```

## Verification

```text
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] native debug build dapat dijalankan pada physical Android device
[x] media notification tampil
[x] notification Previous / Play-Pause / Next bekerja
[x] lockscreen media controls tampil
[x] lockscreen Previous / Play-Pause / Next bekerja
[x] lockscreen progress/seek tersedia
[x] background playback setelah Home bekerja
[x] playback dengan screen off bekerja
[x] active playback tetap berjalan setelah app keluar dari foreground
[x] Bluetooth transport controls bekerja
[x] headset transport controls bekerja
[x] audio focus handling bekerja
[x] unplug/noisy-audio safe pause bekerja
[x] system controls tetap sinkron dengan canonical NexPlay playback runtime
```

## Known Limitation

- Real artwork belum dimigrasikan sehingga Android system media surface masih menggunakan fallback identity.
- Verification saat ini merupakan physical-device validation pada development device, bukan broad OEM/device compatibility matrix.

## Checkpoint

```text
feat(android): add MediaSession background playback
```
---

# 20. Phase 13 — Video Player UI

**Status: COMPLETED + VERIFIED - 2026-09-04**

## Tujuan

Membuat native Video Player di atas canonical Queue Engine, Playback Engine, dan MediaSession runtime yang sama tanpa membuat ExoPlayer atau queue kedua.

## Implemented

- Media3 PlayerView video surface;
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
- direct Video Player exit menghentikan runtime playback tanpa menghapus retained QueueState;
- Child Lock;
- Android Screen Pinning melalui startLockTask();
- persistent local 4-digit Child Lock PIN.

## Architecture

```
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
```

Ownership contract:

- Queue Engine tetap authoritative untuk queue/current-index/shuffle/repeat;
- Playback Engine tetap authoritative untuk ExoPlayer dan runtime playback;
- Video Player hanya presentation/interaction layer;
- Audio Only tidak membuat source atau queue baru;
- MediaSession tetap menggunakan playback runtime yang sama;
- direct player close menghentikan runtime tanpa menghapus retained queue;
- Child Lock tidak mengubah playback architecture.

## Acceptance

```
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
[x] Video close retains QueueState
[x] Child Lock
[x] Persistent Child Lock PIN
[x] Long-hold + PIN unlock
[x] Player tetap menggunakan canonical ExoPlayer
[x] Player tetap menggunakan canonical Queue Engine
```

## Verification

```
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
```

## Known Limitation

- Android Screen Pinning bukan true managed-device kiosk / Device Owner mode.
- System-level escape behavior tetap mengikuti Android/OEM policy.
- Change / Reset Child Lock PIN UI ditunda sampai Settings surface tersedia.
- Broad codec compatibility dan old-device hardening tetap menjadi PHASE 21.

## Checkpoint

```
feat(video): add native video player
```

---
# 21. Phase 14 — Search

**Status: COMPLETED + VERIFIED - 2026-09-04**

## Tujuan

Membuat native media search berdasarkan canonical library tanpa membuat scanner, repository, queue, atau playback runtime kedua.

## Implemented

- dedicated native Search screen;
- Search tersedia dari Folders dan PlayHub root surfaces;
- existing reversible top-overlay presentation tetap digunakan;
- bottom bar tetap tersembunyi selama Search aktif;
- Search membaca canonical `MediaLibraryRepository`;
- live in-memory filtering;
- case-insensitive substring matching;
- field pencarian:
  - title;
  - filename melalui `displayName`;
  - artist;
  - album;
  - folder name;
- hasil hanya canonical media files;
- empty-query state;
- no-result state;
- clear-query action;
- AudioItem dan VideoItem ditampilkan melalui native result rows;
- result list tidak menjadi playback queue;
- tap result me-resolve original folder;
- AudioItem membuka existing Audio Player;
- VideoItem membuka existing Video Player;
- playback tetap menggunakan existing `QueuePlaybackCoordinator`;
- source-folder playback menggunakan `FolderContext`;
- fallback single item menggunakan existing single-item playback context;
- tidak ada dependency baru;
- tidak ada scanner, MediaLibraryRepository, QueueEngine, PlaybackEngine, MediaSession, atau ExoPlayer kedua.

## Field

```text
title
filename
artist
album
folder
```

## Behavior

```text
Canonical MediaLibraryRepository
        ↓
Live in-memory Search
        ↓
MediaItem result
        ↓
Resolve original folder
        ↓
Current native folder queue contract
        ↓
FolderContext
        ↓
QueuePlaybackCoordinator
        ↓
Audio Player / Video Player
```

## Acceptance

```text
[x] Live search
[x] Case-insensitive
[x] Field lengkap
[x] Files only
[x] Tap audio
[x] Tap video
[x] Prev/Next mengikuti current native folder playback contract
[x] Search result list bukan hidden playback queue
[x] Tidak ada duplicated scanner/repository/queue/playback runtime
```

## Verification

```text
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] git diff --check
[x] Native Search berhasil dibuild dan dijalankan pada physical Android development device
[x] Live query menampilkan matching local media
[x] Runtime Search UI diterima user
```

## Known Limitation

- Current native Folder Detail dan Search source queues masih type-specific per media type.
- Canonical Queue Engine sendiri sudah mendukung mixed audio/video queue, tetapi full Flutter mixed-folder queue presentation parity belum dimasukkan ke PHASE 14.
- Gap tersebut harus ditangani sebagai explicit parity work, bukan dengan menjadikan Search result list sebagai hidden queue.

## Checkpoint

```text
feat(search): add native media search
```

---

# 22. Phase 15 — PlayHub

**Status: COMPLETED - 2026-09-05**

## Tujuan

Membuat native PlayHub menggunakan canonical queue/playback architecture tanpa duplicated scanner, queue, atau playback runtime.

## Implemented

```
PlayHub
├── Queue
└── Playlists
```

- Queue menggantikan Continue Playing sebagai playback-session surface;
- Queue card menggunakan canonical QueueState dan PlaybackState;
- seluruh Queue card membuka dedicated Queue Screen;
- Queue card tetap fixed pada PlayHub;
- current title menggunakan one-line running marquee;
- metadata mengikuti Title → Artist → Album/Source Folder;
- progress mengikuti visual Audio Player Kotlin;
- Queue Screen menggunakan canonical queue items/currentIndex;
- current-playing Queue row menggunakan running marquee;
- Queue item tap menggunakan playAt(index);
- AudioItem membuka Audio Player;
- VideoItem membuka Video Player;
- direct source playback menggunakan setQueue() replacement contract;
- source A tidak otomatis digabung dengan source B;
- stop playback dipisahkan dari clear queue melalui stopPlayback();
- Audio Mini Player stop mempertahankan QueueState;
- Video Player close mempertahankan QueueState;
- Video ↔ Audio Only tidak mengubah QueueState;
- retained queue bersifat in-memory dan bukan persistent History;
- retained queue dapat dimainkan kembali dari canonical current item;
- media service mengikuti active playback runtime;
- Search PlayHub dipindahkan dari header ke 3-dots menu;
- Recently Played tidak digunakan pada PHASE 15;
- Playlists tetap placeholder sampai PHASE 16;
- Queue card dan Playlists heading tetap fixed;
- hanya playlist rows area yang scrollable.

## Architecture

```
Folder / Search
      ↓
setQueue()
      ↓
Canonical QueueState
      ↓
PlayHub Queue Card
      ↓
Queue Screen
      ↓
playAt(index)
      ↓
QueuePlaybackCoordinator
      ↓
PlaybackEngine / ExoPlayer
```

Stop contract:

```
stopPlayback()
→ stop playback runtime
→ retain QueueState

clear()
→ stop playback runtime
→ clear QueueState
```

## Acceptance

```
[x] Queue berasal dari canonical QueueState
[x] No duplicated scanning
[x] No duplicated queue/playback runtime
[x] Whole Queue card membuka Queue Screen
[x] Queue item tap menggunakan playAt(index)
[x] Direct source playback replaces previous source queue
[x] Audio / Video / Video Audio Only mengikuti Queue contract yang sama
[x] Stop playback dapat mempertahankan queue
[x] Search tetap menggunakan existing Search overlay
[x] Playlist persistence tidak masuk PHASE 15
[x] Persistent History tidak masuk PHASE 15
[x] Playlist rows memiliki independent scroll area
```

## Verification

```
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] git diff --check PASS
[x] Manual runtime PlayHub/Queue behavior divalidasi secara iteratif pada physical Android development device
[!] connectedDebugAndroidTest latest run: 8/12 PASS, 4 playback instrumentation failures
```

## Known Concern

Latest connectedDebugAndroidTest run pada physical Android 15 device memiliki empat playback instrumentation failures:

- playbackEnd_advancesQueueAndStartsNextItem;
- repeatOne_replaysCurrentItemAfterCompletion;
- abRepeat_loopsWithinMarkersAndResetsOnMediaChange;
- stopPlayback_retainsQueueAndPlayRestartsCurrentItem.

Tiga failure pertama berasal dari existing playback instrumentation coverage dan bersama test retained-queue baru sama-sama gagal pada generated silent-WAV playback start/completion path.

Unit test, lint, dan debug build tetap PASS.

Root cause instrumentation failure belum dibuktikan dan harus diperlakukan sebagai follow-up concern terpisah, bukan diklaim sudah solved.

## Known Limitation

- Retained queue hanya in-memory selama process hidup.
- Playlist persistence belum tersedia sampai PHASE 16.
- Persistent playback History belum tersedia dan tetap deferred; PHASE 17 sekarang digunakan untuk UI/UX Refinement.
- Current Folder Detail/Search source queues masih type-specific per media type.

## Checkpoint

```
feat(playhub): add native PlayHub
```

---
# 23. Phase 16 — Playlist Persistence

**Status: COMPLETED - 2026-09-05**

## Tujuan

Memigrasikan user-owned playlist collection ke native Room dan mengintegrasikannya dengan existing Folders, Audio Player, PlayHub, Queue Engine, dan Playback Engine tanpa membuat duplicate media/playback architecture.

## Implemented

### Persistence

- Room-backed NexPlayDatabase;
- PlaylistEntity;
- PlaylistItemEntity;
- PlaylistDao;
- PlaylistRepository;
- stable String playlist IDs;
- persistent ordered media URI membership;
- duplicate URI protection;
- cascade playlist-item deletion;
- create;
- rename;
- delete;
- clear;
- add;
- remove;
- reorder;
- observable playlist summaries;
- observable playlist detail.

MediaStore metadata tidak diduplikasi secara penuh ke Room.

Canonical media tetap di-resolve melalui MediaLibraryRepository.

### Folder Detail Selection Mode

- long press memasuki selection mode;
- selected media menggunakan canonical URI;
- tap ketika selecting melakukan toggle selection;
- playback tidak dimulai ketika selection mode aktif;
- selected row visual;
- selected / unchecked indicators;
- Select All;
- Deselect All;
- Android Back membersihkan selection terlebih dahulu;
- fixed 64dp selection action bar;
- selected count;
- Add;
- Clear;
- multi-item Add to Playlist;
- create playlist dengan selected media sebagai initial items;
- Audio Mini Player bergeser di atas selection action bar;
- selection state tetap presentation-local.

### Add to Playlist

Shared Add to Playlist flow menerima one-or-many canonical media items.

Entry point:

```
Folder Detail Selection
→ Add to Playlist

Audio Player
→ Add to Playlist
```

Playlist creation dilakukan dari Add to Playlist flow.

Standalone New action di PlayHub dihapus.

### PlayHub

- Playlists placeholder diganti persistent playlist summaries;
- playlist name dan item count ditampilkan;
- playlist row membuka Playlist Detail;
- Queue card dan Playlists heading tetap fixed;
- hanya playlist rows area yang scrollable;
- Queue empty state disejajarkan dengan filled media geometry;
- Queue card background/header membuka Queue Screen;
- current media area membuka current player;
- transport controls tetap transport-only;
- player media clickable area tidak menggunakan rounded clipping yang memotong duration text.

### Playlist Detail

- Play All;
- Shuffle;
- Rename;
- Clear;
- Delete;
- ordered item rows;
- remove;
- native drag-handle reorder refined pada PHASE 17 UI/UX Refinement #1;
- unavailable media presentation;
- safe Back transition.

Playlist exit crash yang terjadi ketika selected playlist ID dibersihkan selama AnimatedVisibility exit diperbaiki dengan null-safe playlist layer rendering.

### Playlist Playback

Playlist playback membentuk:

```
PlaylistContext
↓
QueuePlaybackCoordinator.setQueue()
↓
Canonical QueueState
↓
Existing Playback Engine
```

Tidak ada hidden queue atau player kedua.

### NexPlay Audio-First Playlist Policy

Playlist adalah media-agnostic collection tetapi audio-first playback context.

```
AudioItem in Playlist
→ Audio Player

VideoItem in Playlist
→ Audio Player / Audio Only

Mixed Audio + Video Playlist
→ Audio Player continuously
```

Canonical VideoItem tidak dikonversi menjadi AudioItem.

Queue tetap dapat berisi mixed MediaItem.

Explicit Switch to Video tetap tersedia untuk current VideoItem tanpa queue rebuild.

Ketika current playlist item berubah setelah explicit video presentation, presentation kembali ke Audio Player.

Direct Folder/Search VideoItem tetap membuka Video Player.

## Architecture

```
Folder Detail Selection ─┐
                         │
Audio Player Add ────────┼──→ PlaylistRepository
                         │           ↓
                         │          Room
                         │
PlayHub Playlist Row ────┘
          ↓
    Playlist Detail
          ↓
Resolve Media URI
          ↓
MediaLibraryRepository
          ↓
QueuePlaybackCoordinator
          ↓
PlaylistContext
          ↓
Canonical QueueState
          ↓
Existing Playback Engine
```

Ownership contract:

- Room memiliki NexPlay-owned playlist persistence;
- PlaylistRepository memiliki playlist persistence semantics;
- MediaLibraryRepository tetap memiliki canonical current media metadata;
- Queue Engine tetap memiliki active queue state dan policy;
- Playback Engine tetap memiliki ExoPlayer/runtime playback;
- UI hanya memiliki presentation state;
- Folder selection state tidak dipersist;
- PlaylistContext menentukan audio-first presentation policy;
- media identity tidak berubah hanya karena presentation mode.

## Acceptance

```
[x] Create playlist
[x] Rename playlist
[x] Delete playlist
[x] Clear playlist
[x] Add media
[x] Multi-add media
[x] Remove media
[x] Reorder media
[x] Playlist summaries on PlayHub
[x] Playlist Detail
[x] Folder Detail selection mode
[x] Select All / Deselect All
[x] Add to Playlist from Folder Detail
[x] Add to Playlist from Audio Player
[x] Playlist → Queue
[x] PlaylistContext
[x] Video-only playlist → Audio Player
[x] Mixed playlist → Audio Player continuously
[x] Explicit Video switch preserves queue
[x] Playlist Back no longer crashes
[x] No duplicate Queue Engine
[x] No duplicate Playback Engine
[x] No duplicate MediaLibraryRepository
[x] No duplicate MediaStore metadata in Room
```

## Verification

```
[x] testDebugUnitTest
[x] lintDebug
[x] assembleDebug
[x] git diff --check
[x] Manual Folder Detail selection verification
[x] Manual Add to Playlist verification
[x] Manual Playlist Detail runtime verification
[x] Manual Playlist Back regression verification
[x] Manual PlayHub playlist verification
[x] Manual Queue-card interaction verification
[x] Manual mixed playlist audio-first verification
[x] Manual video-only playlist audio-first verification
[ ] Targeted Room instrumentation executed successfully
```

Latest targeted Room instrumentation attempt:

```
INSTALL_FAILED_USER_RESTRICTED
Starting 0 tests
Finished 0 tests
```

The targeted Room test did not execute because the physical device rejected installation of the Android test APK.

This is recorded as a verification concern and is not treated as a PlaylistRepository test failure.

## Known Limitation / Concern

- Dedicated Room instrumentation persistence test still needs a successful device run.
- Missing-media handling is implemented, but broad deleted-file/device matrix is not yet exhaustive.
- Playlist drag-and-drop reorder kemudian ditambahkan pada PHASE 17 UI/UX Refinement #1 tanpa third-party reorder dependency.
- Persistent playback History is intentionally not part of PHASE 16.
- Retained QueueState remains process-local and separate from persistent playlist data.
- Current Folder/Search queue parity gap remains separate from playlist behavior.
- Broad codec/device hardening remains PHASE 21.

## Checkpoint

```
feat(playlist): add Room-backed playlists
```

---
# 24. Phase 17 — UI/UX Refinement

**Status: COMPLETED + VERIFIED — CHECKPOINT #2 - 2026-09-06**

## Tujuan

Melakukan iterative UI/UX refinement pada native NexPlay setelah core Folders, Players, Search, PlayHub, dan Playlist tersedia.

PHASE 17 menggantikan arah lama History / Recently Played.

Phase ini bukan broad final polish PHASE 20. Fokusnya adalah memperbaiki user-facing interaction dan visual issues yang ditemukan selama penggunaan native implementation saat ini, secara incremental dan checkpoint-based.

Persistent playback History / Recently Played tetap deferred dan tidak dikerjakan pada PHASE 17.

## Checkpoint #1 — Implemented

### Typography dan Theme

- active application typography kembali menggunakan default Android / Compose font;
- custom text sizes, weights, line heights, dan letter spacing tetap dipertahankan;
- bundled Sora FontFamily tidak lagi dipaksakan melalui Typography;
- Sora resource files belum dihapus pada checkpoint ini;
- Folders overflow tidak lagi menggunakan binary Dark mode switch;
- theme choices sekarang explicit:
  - System;
  - Light;
  - Dark;
- selected theme ditandai pada menu;
- existing persisted NexPlayThemeMode contract tetap digunakan.

### Audio Player Refinement

- refined Audio Player visual hierarchy;
- NexPlay identity block ditambahkan;
- artwork fallback tetap digunakan;
- artwork presentation diperbesar dan dipusatkan;
- title menggunakan one-line running marquee;
- title hierarchy diperkuat;
- Control Deck menggunakan circular buttons;
- setiap Control Deck action memiliki caption;
- action tetap mencakup:
  - seek -10;
  - Shuffle;
  - A-B;
  - Add;
  - Repeat;
  - seek +10;
- Previous dan Next menggunakan clearer side transport presentation;
- Play / Pause menggunakan refined primary transport geometry;
- Audio Player footer identity ditambahkan;
- playback behavior, queue ownership, A-B behavior, swipe minimize, queue access, dan PlaylistContext policy tetap dipertahankan.

### Playlist Detail Refinement

- Play All foreground icon/text menggunakan white content;
- move-up / move-down controls dihapus;
- dedicated drag handle digunakan untuk playlist reorder;
- reorder menggunakan native frame-driven drag interaction;
- pointer gesture hanya memperbarui drag position;
- reorder calculation mengikuti display frame;
- dragged row mempertahankan visual relation terhadap live LazyColumn slot;
- edge auto-scroll tetap native;
- final order dipersist sekali melalui existing PlaylistRepository setelah drop;
- Room schema tidak berubah;
- tidak ada third-party reorder dependency;
- stale drag callback setelah item berubah index diperbaiki menggunakan current callback state;
- item dapat di-reorder kembali setelah previous reorder;
- direct row delete icon diganti menjadi 3-dots;
- row 3-dots menyediakan:
  - Delete;
  - Add to Playlist;
- Add to Playlist menggunakan existing shared dialog/repository path;
- row 3-dots mengikuti shared right-edge optical alignment dengan header;
- Playlist Detail entry/exit motion menggunakan dedicated 320 ms duration;
- playlist playback tetap menggunakan PlaylistContext audio-first policy.

## Architecture Guardrails

```text
Queue Engine
    tetap authoritative queue owner

Playback Engine
    tetap authoritative runtime / ExoPlayer owner

PlaylistRepository
    tetap persistence boundary

Room
    schema unchanged

PlaylistContext
    audio-first policy unchanged

UI refinement
    presentation + interaction only
```

Tidak ada:

- Queue Engine kedua;
- Playback Engine kedua;
- MediaLibraryRepository kedua;
- MediaSession kedua;
- ExoPlayer kedua;
- playlist database schema baru;
- reorder dependency baru.

## Verification — Checkpoint #1

```text
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] git diff --check PASS
[x] Audio Player visual refinement accepted on runtime
[x] Playlist drag reorder accepted on runtime
[x] Reorder after a previous reorder works
[x] Reverse/subsequent drag responsiveness accepted
[x] Playlist row 3-dots alignment accepted
[x] Playlist Detail motion refinement accepted
```

## Checkpoint #2 — Implemented

### Floating Bottom Navigation

- Bottom Navigation menggunakan floating overlay presentation;
- visible navigation surface memiliki height 64dp;
- outer floating footprint mempertahankan vertical spacing;
- root content tidak lagi di-resize oleh Bottom Navigation;
- Folders dan PlayHub content dapat bergerak di belakang floating navigation;
- trailing list clearance menjaga item terakhir tetap dapat diakses;
- floating navigation tetap hanya memiliki Folders dan Playhub;
- navigation behavior tidak berubah.

### Floating Surface Treatment

- Bottom Navigation dan Audio Mini Player menggunakan opaque application background;
- transparency eksperimen dihapus;
- kedua surface menggunakan 2dp outline;
- outline tetap restrained terhadap application background;
- tidak ada shadow;
- tidak ada blur;
- dark background native diselaraskan menjadi #111317;
- light theme tetap menggunakan existing light background contract;
- Audio Mini Player tetap menggunakan 64dp geometry;
- existing Audio Mini Player interaction tetap dipertahankan.

### Full-Width Row Interaction

- Folders dan Folder Detail tetap menjadi reference full-width tap / pressed surface;
- PlayHub playlist summary row sekarang memiliki full-width pressed state;
- Playlist Detail media row sekarang memiliki full-width pressed state;
- horizontal icon dan text alignment tetap dipertahankan;
- horizontal screen inset dipindahkan ke dalam clickable row ketika diperlukan;
- Playlist Detail dedicated drag handle tetap digunakan;
- reorder calculations tidak berubah;
- PlaylistRepository persistence path tidak berubah;
- row overflow menu behavior tidak berubah.

### Native App Icon

- current NexPlay Flutter launcher branding digunakan sebagai parity reference;
- launcher assets tersedia untuk mdpi, hdpi, xhdpi, xxhdpi, dan xxxhdpi;
- adaptive launcher icon menggunakan NexPlay foreground;
- round launcher icon tersedia;
- monochrome / themed launcher reference tersedia;
- Play Store launcher source asset tersedia;
- Android Studio default launcher artwork tidak lagi menjadi active launcher presentation;
- tidak ada launcher generator dependency baru.

### Native Splash

- native Kotlin launch theme tersedia;
- pre-Android-12 startup menggunakan branded launch background;
- centered NexPlay launch branding digunakan untuk legacy startup presentation;
- Android 12+ menggunakan native system splash attributes;
- Android 12+ splash menggunakan branded NexPlay foreground;
- splash background menggunakan current Kotlin dark background #111317;
- MainActivity berpindah dari launch theme ke normal Theme.NexPlay;
- tidak ada fake startup delay;
- tidak ada second-stage Compose splash;
- tidak ada splash generator dependency baru.

## Architecture Guardrails — Checkpoint #2

```text
Presentation refinement
        ↓
existing UI state / events
        ↓
existing repositories / coordinator
        ↓
canonical QueueEngine + PlaybackEngine
```

Tidak ada perubahan ownership pada:

- Queue Engine;
- Playback Engine;
- ExoPlayer;
- MediaSession;
- MediaLibraryRepository;
- PlaylistRepository;
- Room;
- PlaylistContext.

## Verification — Checkpoint #2

```text
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] git diff --check PASS
[x] Floating Bottom Navigation runtime presentation accepted
[x] Dark-mode floating surfaces accepted
[x] Light-mode floating surfaces accepted
[x] Audio Mini Player floating surface accepted
[x] PlayHub full-width playlist-row interaction accepted
[x] Playlist Detail full-width media-row interaction accepted
[x] Flutter to Kotlin launcher / branding hash checks PASS
[x] APK install PASS
[x] MainActivity cold launch PASS
[x] NexPlay launcher icon visually verified on physical Android device
```

Native splash resources dan cold-start path telah terverifikasi melalui build, install, dan runtime launch.

Broad OEM-specific launcher mask dan splash rendering matrix tetap di luar scope checkpoint ini.
## Known Limitation / Deferred

- real media artwork extraction/display belum dimigrasikan;
- Sora font resources/license masih berada di project walaupun active Typography sudah kembali ke default Android / Compose;
- dedicated PlaylistRepository Room instrumentation masih perlu successful device run;
- persistent playback History / Recently Played tetap deferred;
- current PHASE 17 refinement pass selesai pada checkpoint #2; additional broad polish dipindahkan ke PHASE 20 atau explicit scoped follow-up;
- current Folder/Search mixed-media queue parity gap tetap terpisah;
- broad codec/device hardening tetap PHASE 21.

## Next

Current PHASE 17 refinement pass selesai pada checkpoint #2. Migration progression dilanjutkan ke PHASE 18.

Setiap refinement berikutnya harus:

- tetap scoped;
- mempertahankan canonical queue/playback ownership;
- tidak memperluas persistence schema tanpa kebutuhan;
- tidak menambah dependency tanpa alasan;
- diverifikasi sebelum checkpoint berikutnya.

## Checkpoint

```text
feat(ui): checkpoint UI/UX refinement #2
```

---

# 25. Phase 18 — Settings dan DataStore

**Status: COMPLETED + VERIFIED - 2026-09-06**

## Tujuan

Memindahkan application-owned preferences ke AndroidX Preferences DataStore dengan explicit defaults dan tanpa memindahkan ownership state subsystem lain.

## Implemented

- AndroidX Preferences DataStore dependency;
- application-scoped NexPlayPreferencesRepository;
- repository dimiliki NexPlayApplication;
- theme mode disimpan melalui DataStore;
- Folder sort field disimpan melalui DataStore;
- Folder sort direction disimpan melalui DataStore;
- Folder Detail media sort field disimpan melalui DataStore;
- Folder Detail media sort direction disimpan melalui DataStore;
- legacy theme SharedPreferences dimigrasikan menggunakan SharedPreferencesMigration;
- existing System / Light / Dark selector tetap digunakan;
- existing Folders sort menu tetap digunakan;
- existing Folder Detail media sort menu tetap digunakan;
- invalid / unknown stored enum value jatuh kembali ke explicit default;
- DataStore first emission digunakan untuk resolve persisted application preferences;
- startup system-bar transition diperbaiki agar dark native splash tidak diikuti gesture-navigation light flash;
- MainActivity menerapkan dark startup edge-to-edge state sebelum DataStore resolve;
- actual System / Light / Dark system-bar appearance tetap diterapkan setelah preference diketahui;
- tidak ada dedicated Settings screen baru pada phase ini;
- tidak ada Room schema change;
- tidak ada queue/playback architecture change.

## Persisted Preferences

```text
theme_mode
default: SYSTEM

folder_sort_field
default: NAME

folder_sort_direction
default: ASCENDING

media_sort_field
default: NAME

media_sort_direction
default: ASCENDING
```

## Ownership Decisions

```text
DataStore
→ application-owned settings only

QueueEngine
→ repeat + shuffle runtime ownership remains unchanged

NexPlayApp
→ root-tab navigation remains session-local

Child Lock preference store
→ PIN remains separately owned

PlaylistRepository / Room
→ playlist persistence remains unchanged

PlaybackEngine
→ playback runtime remains unchanged
```

Repeat dan shuffle tidak dipersist pada PHASE 18 karena keduanya adalah active QueueState policy.

Current root tab tidak dipersist karena startup contract tetap Folders.

Child Lock PIN tidak digabungkan ke general DataStore karena memiliki separate security/feature ownership.

Playback speed tetap deferred karena capability tersebut belum dimigrasikan.

## Acceptance

```text
[x] Settings persistent
[x] Default values jelas
[x] Tidak menyimpan data media besar
[x] Existing theme selection survives restart
[x] Folder sort survives restart
[x] Folder Detail media sort survives restart
[x] Queue runtime ownership unchanged
[x] Child Lock persistence ownership unchanged
[x] Startup system-bar flash regression fixed
```

## Verification

```text
[x] Manual Theme persistence verification
[x] Manual Folder sort persistence verification
[x] Manual Folder Detail media sort persistence verification
[x] Manual cold-start system-bar transition verification
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] assembleDebugAndroidTest PASS
[x] git diff --check PASS
```

## Known Limitation / Deferred

- centralized dedicated Settings screen belum diimplementasikan;
- Playback speed tetap deferred;
- Change / Reset Child Lock PIN UI tetap deferred sampai Settings surface tersedia;
- repeat dan shuffle intentionally tetap runtime-only pada checkpoint ini;
- root tab intentionally tetap session-local.

## Checkpoint

```text
feat(settings): add persistent app preferences
```

---
# 26. Phase 19 — Functional Parity + Native UX Polish + Cleanup Audit

**Status: COMPLETED + VERIFIED — 2026-09-06**

## Tujuan

Melakukan functional parity audit, native Android UX polish, dan targeted cleanup dalam satu phase.

Flutter tetap behavioral/reference baseline, bukan pixel-perfect visual target.

## Completed Checkpoints

### Checkpoint #1 — Dead Resource Cleanup

- unused template launcher resources dihapus;
- unused bundled Sora resources dihapus.

### Checkpoint #2 — Mixed Folder / Search Queue Parity

- Folder Detail mempertahankan mixed audio/video sorted queue;
- Search menggunakan full mixed source-folder queue;
- canonical QueueEngine / PlaybackEngine ownership tidak berubah.

### Checkpoint #3 — Media Library Lifecycle

- cold-start initial scan dipertahankan;
- background → foreground melakukan MediaStore refresh;
- manual refresh dan permission flow dipertahankan;
- cached-first persistent index tidak ditambahkan.

### Checkpoint #4 — Android Open With / ACTION_VIEW

- audio/video ACTION_VIEW didukung;
- MainActivity menggunakan singleTop;
- cold-start dan warm-start external intent didukung;
- external URI dipetakan ke canonical MediaItem;
- SingleItemContext digunakan;
- playback tetap menggunakan QueuePlaybackCoordinator + single ExoPlayer.

### Checkpoint #5 — Final Classification + Cleanup

- tidak ada parity-critical gap tersisa;
- obsolete PLAYLIST / PLAYER vertical states dihapus;
- unreachable placeholder UI dihapus.

## Final Classification

```text
MATCHED / IMPROVED
App Shell / Navigation
Folders
Folder Detail
Search
Queue
Playlist
Audio Player
Video Player core playback
Audio Mini Player
Bottom Navigation
MediaSession / Background Playback
Theme / Settings / DataStore
Android Open With / ACTION_VIEW
Media Library lifecycle refresh
Startup / Splash / App Icon

INTENTIONALLY CHANGED
PlayHub = Queue + Playlists only
Cached-first startup not replicated
Picture-in-Picture not implemented
Floating video mini player not implemented
Video Player direct close stops playback runtime
PHASE 20 merged into PHASE 19

DEFERRED
Playback speed
Persistent Recently Played / History
Playback queue snapshot / relaunch resume
Share
Media Details
Real artwork
Dedicated Settings screen
Broad codec / old-device hardening
```

## Acceptance

```text
[x] Parity-critical behavior classified
[x] No unknown parity-critical gap remains
[x] Mixed Folder queue parity restored
[x] Mixed Search source-folder queue parity restored
[x] Media lifecycle refresh restored
[x] Android Open With implemented
[x] Intentional Kotlin differences documented
[x] PiP intentionally excluded
[x] Floating video mini player intentionally excluded
[x] Cached-first startup intentionally excluded
[x] Dead resources removed
[x] Dead placeholder navigation/UI removed
[x] Kotlin-native UX improvements retained
[x] No new dependency
[x] Queue / Playback ownership unchanged
[x] Room schema unchanged
```

## Verification

```text
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] assembleDebugAndroidTest PASS
[x] git diff --check PASS
[x] Checkpoint #2 runtime accepted
[x] Checkpoint #3 runtime accepted
[x] Checkpoint #4 runtime accepted
```

Implementation commit:

```text
526c78d feat(parity): complete native parity and UX pass
```

---

# 27. Phase 20 — UI/UX Parity dan Polish

**Status: MERGED INTO PHASE 19 — NO STANDALONE EXECUTION**

Functional parity dan native UX polish telah dijalankan bersama di PHASE 19.

PHASE 20 tidak memiliki standalone implementation pass.

Nomor phase selanjutnya tetap dipertahankan untuk menjaga roadmap history.

---
# 28. Phase 21 — Codec Compatibility dan Old Device Hardening

**Status: COMPLETED + VERIFIED - 2026-09-06**

## Final Scope

PHASE 21 ditutup sebagai practical codec compatibility dan legacy-device acceptance phase.

Target akhirnya bukan menjamin seluruh codec, profile, level, container, bit depth, dan vendor decoder di seluruh perangkat Android.

Target yang divalidasi:

- historical old-device video rendering concern dari Flutter baseline tidak muncul kembali pada native Kotlin path yang diuji;
- native Media3 / ExoPlayer playback tetap menggunakan satu canonical PlaybackEngine;
- Video Player tidak membuat decoder, ExoPlayer, queue, atau playback runtime kedua;
- old-device Video Player presentation tidak memiliki system-bar mismatch yang membuat UI terlihat belang;
- compatibility issue yang nyata harus ditangani berdasarkan physical-device evidence, bukan asumsi;
- software decoder atau FFmpeg tidak ditambahkan tanpa failure yang membuktikan kebutuhan tersebut.

## Implemented

- native Media3 / ExoPlayer playback path dipertahankan;
- single ExoPlayer ownership tetap berada pada PlaybackEngine;
- QueuePlaybackCoordinator tetap canonical integration boundary;
- Video Player system-bar presentation di-harden untuk dark video surface;
- status bar dan navigation bar Video Player menggunakan dark presentation dengan readable light icons;
- previous application system-bar appearance direstore setelah keluar dari Video Player;
- existing fullscreen immersive behavior dipertahankan;
- existing Child Lock system-UI behavior dipertahankan;
- tidak ada QueueEngine change;
- tidak ada MediaSession ownership change;
- tidak ada PlaylistRepository / Room change;
- tidak ada DataStore ownership change;
- tidak ada dependency baru;
- tidak ada bundled software decoder;
- tidak ada FFmpeg integration.

## Physical Device Evidence

Video rendering berhasil pada current native Kotlin build di:

```
Poco F5
Samsung A57
Redmi Note 7
Infinix Hot 10 Play
```

Redmi Note 11 tidak menjadi bagian final acceptance gate dan tidak menjadi blocker PHASE 21.

Redmi Note 7 digunakan sebagai legacy-device comprehensive acceptance target.

Comprehensive manual runtime validation pada Redmi Note 7 mencakup penggunaan normal aplikasi dan critical playback/player flows. Tidak ditemukan blocking compatibility regression pada test yang dilakukan.

Historical Flutter symptom:

```
audio berjalan
video image tidak tampil
```

tidak berhasil direproduksi pada tested native Kotlin devices di atas.

## Video Player Old-Device Hardening

Physical-device testing menemukan Video Player system-bar presentation dapat terlihat tidak menyatu pada sebagian device/navigation implementation.

Source evidence menunjukkan:

```
MainActivity
→ app-theme-derived system-bar style

VideoPlayerScreen
→ black video surface
→ light system icons
→ independent system-bar visibility
```

VideoPlayerScreen kemudian mengambil video-specific visual ownership untuk system bars tanpa mengubah global application theme contract.

Runtime result diterima:

- Video Player surface tetap black;
- status/navigation system-bar presentation menyatu dengan Video Player;
- system icons tetap readable;
- fullscreen tetap immersive;
- exit Video Player mengembalikan normal application presentation.

## Compatibility Decision

NexPlay Kotlin menggunakan device decoder capability melalui Android Media3 / ExoPlayer.

PHASE 21 tidak menambahkan universal software decoding layer.

Jika future media tertentu gagal:

```
reproduce failure
↓
classify actual failure
↓
determine NexPlay-side vs device capability
↓
targeted fix only when evidence justifies it
```

Tidak ada parallel playback architecture yang ditambahkan untuk speculative compatibility.

## Acceptance

```
[x] Historical old-device video rendering issue tidak muncul pada tested Kotlin devices
[x] Video image + audio berjalan pada tested physical devices
[x] Comprehensive legacy-device acceptance dilakukan pada Redmi Note 7
[x] Video Player system-bar old-device presentation di-harden
[x] Runtime system-bar result diterima
[x] No blocking compatibility regression observed pada tested flows
[x] No second ExoPlayer / decoder path
[x] No FFmpeg / software decoder dependency
[x] Compatibility evidence terdokumentasi
[ ] Exhaustive universal codec/profile/level/device certification
```

Exhaustive universal codec certification sengaja tidak menjadi acceptance requirement karena tidak ada evidence yang membenarkan perluasan scope tersebut.

## Verification

```
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] assembleDebugAndroidTest PASS
[x] git diff --check PASS
[x] Poco F5 video rendering runtime PASS
[x] Samsung A57 video rendering runtime PASS
[x] Redmi Note 7 video rendering runtime PASS
[x] Infinix Hot 10 Play video rendering runtime PASS
[x] Redmi Note 7 comprehensive manual acceptance PASS
[x] Video Player system-bar presentation runtime accepted
```

## Known Limitation

- Media3 tetap bergantung pada actual decoder/capability perangkat untuk codec yang tidak universal.
- PHASE 21 tidak membuktikan seluruh codec, container, profile, level, bit depth, resolution, dan vendor implementation.
- Artificial unsupported-media corpus tidak menjadi blocker karena tidak ada reproducible compatibility failure yang membenarkan subsystem tambahan.
- Redmi Note 11 belum menjadi bagian physical-device verification PHASE 21.
- Future reproducible device/codec failure harus ditangani sebagai targeted compatibility issue berdasarkan actual evidence.

## Conclusion

Practical codec compatibility dan old-device hardening concern yang menjadi alasan PHASE 21 dinyatakan resolved untuk current migration scope.

Tidak ada evidence yang membenarkan penambahan software decoder, FFmpeg, parallel ExoPlayer, atau compatibility abstraction tambahan.

Migration progression dilanjutkan ke PHASE 22 — Performance dan Responsiveness.

---

# 29. Phase 22 — Performance dan Responsiveness

**Status: COMPLETED + VERIFIED - 2026-09-06**

## Tujuan

Memastikan NexPlay Kotlin tetap responsive pada device modern, legacy, dan low-end tanpa melakukan premature optimization atau architecture rewrite.

## Final Strategy

PHASE 22 menggunakan practical runtime acceptance.

Primary devices:

```
Redmi Note 7
→ legacy-device acceptance

Infinix Hot 10 Play
→ low-end / vendor-diversity acceptance

Poco F5
→ modern regression control
```

Tidak ada arbitrary benchmark target yang dipaksakan.

Optimization hanya dilakukan ketika terdapat actual observable problem.

## Runtime Acceptance

```
[x] Startup tidak terasa blocking pada Redmi Note 7
[x] Startup tidak terasa blocking pada Infinix Hot 10 Play
[x] Folders scrolling responsive
[x] Folder Detail scrolling responsive
[x] Search typing/filtering tidak terasa lag
[x] PlayHub / Queue / Playlist responsive
[x] Audio Player open/close responsive
[x] Video Player open/close responsive
[x] Audio ↔ Video transition tetap stabil
[x] Background/resume tidak membuat UI freeze
[x] Tidak ada obvious memory/resource issue pada tested flows
[x] Playback tetap stabil
[x] Architecture ownership tidak berubah
[x] No regression pada Poco F5
```

## Final UI Hardening

Sebelum closeout, remaining media-presentation gap juga diselesaikan.

Implemented:

- shared lazy media visual loading;
- bounded in-memory bitmap cache;
- audio embedded artwork;
- video thumbnail/frame loading;
- safe visual fallback;
- Folder Detail thumbnails/artwork;
- Search thumbnails/artwork;
- Playlist Detail thumbnails/artwork;
- PlayHub current Queue artwork/thumbnail;
- Queue Screen thumbnails/artwork;
- Full Audio Player real artwork;
- Audio Mini Player artwork;
- stable Queue card height untuk empty dan populated state;
- explicit Audio Mini Player screen visibility policy.

Audio Mini Player sekarang tampil hanya pada:

```
Folders
Folder Detail
Queue Screen
Playlist Detail
```

Audio Mini Player tidak tampil pada:

```
PlayHub
Search
Full Audio Player
Video Player
```

Playlist collection rows tetap menggunakan playlist icon.

Tidak ada dependency image loader baru.

Tidak ada disk artwork cache baru.

Tidak ada perubahan canonical MediaItem, Room schema, QueueEngine, QueuePlaybackCoordinator, PlaybackEngine, atau ExoPlayer ownership.

## Verification

```
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] assembleDebugAndroidTest PASS
[x] git diff --check PASS
[x] Runtime performance acceptance PASS
[x] Media visual runtime acceptance PASS
[x] Queue card geometry runtime acceptance PASS
[x] Mini Player visibility runtime acceptance PASS
[x] Playback regression acceptance PASS
```

## Known Limitation

- PHASE 22 tidak mengklaim exhaustive benchmark/profiler coverage.
- Tidak ada universal performance guarantee untuk seluruh Android/OEM/device combination.
- Android system media artwork belum menjadi bagian UI artwork migration ini.
- Future reproducible performance regression harus ditangani berdasarkan actual evidence.

## Conclusion

PHASE 22 selesai.

Current Kotlin implementation menjadi candidate untuk Production Migration Gate.

---

# 30. Phase 23 — Production Migration Gate

**Status: COMPLETED + VERIFIED - 2026-09-07**

## Tujuan

Menentukan apakah current Kotlin implementation memenuhi migration-critical contract dan cukup stabil untuk masuk Production Cutover tanpa menambah speculative feature atau architecture change.

## Implemented

Final pre-cutover hardening mencakup:

- Android 12+ splash hardening agar branding tidak terdistorsi oleh system splash masking;
- shared Add to Playlist Material 3 bottom sheet;
- Child Lock hanya pada fullscreen landscape;
- Child Lock PIN digantikan persistent 3x3 unlock pattern;
- minimum 4 unique nodes untuk unlock pattern;
- haptic feedback setelah successful hold-to-unlock;
- Share dan Details pada Audio Player dan Video Player;
- Audio Player swipe-left navigation menuju Queue;
- Queue Back mengembalikan presentation ke Audio Player;
- contextual horizontal Queue → Audio Player return motion;
- Audio Mini Player swipe-up expand;
- white foreground pada purple primary/secondary surfaces;
- real NexPlay logo pada About;
- About icon consistency antara Folders dan PlayHub;
- horizontal icon-only System / Dark / Light theme chooser;
- Folder Detail sorting: Title, Album, Track, Duration, Date Modified;
- Title ascending sebagai default Folder Detail sort;
- legacy media-sort preference yang tidak lagi valid fallback ke Title;
- shared right-side NexPlay scrollbar;
- scrollbar visual track/gutter;
- scrollbar fast-scroll melalui drag;
- scrollbar muncul saat list scroll/drag dan fade out ketika idle;
- scrollbar hanya ditampilkan untuk overflowing list;
- high-quality Full Audio Player artwork path;
- embedded audio artwork diprioritaskan untuk hero artwork;
- bounded Full Audio Player artwork target sampai 1024 px;
- list thumbnail dan Mini Player tetap memakai lightweight visual path.

## Architecture Contract

```
MediaStore / canonical MediaItem
        |
        +--> normal MediaVisualLoader path
        |        |
        |        +--> list / search / queue / mini player thumbnails
        |
        +--> high-quality Audio artwork path
                 |
                 +--> embedded artwork first
                 +--> bounded decode up to 1024 px
                 +--> Full Audio Player only

QueueEngine
    |
    +--> authoritative queue state

PlaybackEngine
    |
    +--> authoritative single ExoPlayer runtime

Compose UI
    |
    +--> presentation / gestures / dialogs / scrollbar / artwork rendering
```

Tidak ada QueueEngine kedua, PlaybackEngine kedua, ExoPlayer kedua, persistence schema baru, atau dependency baru pada final PHASE 23 hardening.

## Verification

```
[x] compileDebugKotlin PASS during iterative hardening
[x] testDebugUnitTest final gate
[x] lintDebug final gate
[x] assembleDebug final gate
[x] assembleDebugAndroidTest final gate
[x] git diff --check final gate
[x] Latest runtime smoke accepted on Poco F5
[x] Latest runtime smoke accepted on Redmi Note 7
[x] Full Audio Player high-quality artwork accepted on Poco F5
[x] Full Audio Player high-quality artwork accepted on Redmi Note 7
[x] Right-side scrollbar runtime presentation accepted
[x] Playback remained stable during targeted runtime checks
```

## Production Migration Gate Decision

**APPROVED TO ENTER PHASE 24 PRODUCTION CUTOVER.**

Approval ini berarti migration-critical Kotlin candidate dapat dibawa ke release/cutover validation. Ini bukan approval bahwa signed production artifact atau public release sudah selesai.

## Known Limitation

- Android Screen Pinning tetap bukan Device Owner / managed-device kiosk mode.
- System-level Child Lock escape behavior tetap mengikuti Android/OEM policy.
- Media playback untuk codec yang tidak universal tetap bergantung pada hardware/software capability perangkat.
- Signed release configuration dan release artifact validation belum dikerjakan pada PHASE 23.
- Install/upgrade/persistence smoke terhadap production release artifact belum dikerjakan pada PHASE 23.
- Publication ke future release repository belum dilakukan pada PHASE 23.

## Checkpoint

```
feat(hardening): finalize production migration gate
```
---

# 31. Phase 24 — Production Cutover

**Status: CURRENT**

## Tujuan

Melakukan perpindahan resmi dari development migration candidate ke release baseline setelah PHASE 23 PASS.

## Repository Contract

NexPlay menggunakan model satu project dengan dua repository.

```
Development
fahmirizrev/NexPlay-workspace
remote: origin

Release
fahmirizrev/NexPlay
remote: release
```

Local development root tetap:

```
C:\laragon\www\nexplay
```

origin tidak boleh diarahkan ke release repository.

## Planned Cutover

1. pastikan PHASE 23 PASS;
2. freeze approved Kotlin migration candidate;
3. pertahankan/tag Flutter historical baseline di development workspace;
4. finalisasi versionCode dan versionName;
5. verifikasi release build configuration;
6. verifikasi signed release build;
7. verifikasi install/upgrade path;
8. verifikasi Room/DataStore/Child Lock preference persistence yang relevan;
9. lakukan final release runtime smoke test;
10. siapkan repository release NexPlay;
11. tambahkan release remote tanpa mengganti origin;
12. publish hanya approved release baseline sesuai release-repository contract;
13. buat release tag/checkpoint;
14. pertahankan NexPlay-workspace sebagai development source of truth.

## Guardrails

- jangan menghapus Flutter reference sebelum cutover selesai;
- jangan memindahkan development ownership ke release repository;
- jangan menjadikan release repository tempat experimental development;
- jangan menambah feature baru selama cutover;
- jangan mengganti package/application identity tanpa explicit decision;
- jangan mengubah persistence schema tanpa kebutuhan terbukti.

## Acceptance

```
[ ] PHASE 23 PASS
[ ] Kotlin migration candidate frozen
[ ] Release configuration verified
[ ] Signed release build verified
[ ] Install/upgrade path verified
[ ] Persistence smoke test PASS
[ ] Final runtime smoke test PASS
[ ] Release repository prepared
[ ] origin tetap NexPlay-workspace
[ ] release remote menunjuk NexPlay
[ ] Release baseline published
[ ] Release tag/checkpoint dibuat
```

---

# 32. Phase 25 — Kotlin Stabilization Release

**Status: PLANNED**

## Tujuan

Menstabilkan Kotlin release setelah Production Cutover tanpa langsung memperluas product scope.

## Default Policy

PHASE 25 adalah stabilization-first release phase.

Prioritas:

- crash;
- ANR;
- playback regression;
- codec/device regression;
- MediaSession/background regression;
- queue synchronization regression;
- playlist/database edge case;
- DataStore preference regression;
- lifecycle regression;
- battery/resource issue;
- memory pressure;
- media thumbnail/artwork loading regression;
- low-end scrolling regression;
- Audio Mini Player visibility regression;
- UI regression yang menghambat penggunaan utama.

## Change Control

```
Reproducible regression
→ investigate
→ smallest targeted fix
→ verify

No demonstrated regression
→ no speculative refactor
```

PHASE 25 bukan tempat untuk:

- architecture rewrite;
- major UI redesign;
- dependency expansion tanpa kebutuhan;
- speculative optimization;
- mengaktifkan semua deferred feature;
- post-migration product expansion.

## Acceptance

```
[ ] No blocking crash/ANR pada supported release flows
[ ] Playback stable
[ ] Background playback stable
[ ] Queue synchronization stable
[ ] Playlist persistence stable
[ ] Media visuals stable
[ ] No obvious media-visual memory pressure
[ ] Legacy/low-end responsiveness acceptable
[ ] No blocking lifecycle regression
[ ] No blocking release regression
[ ] Stabilization issues terdokumentasi
```

Setelah PHASE 25 stabil, future product development dapat dilanjutkan sebagai post-migration expansion berdasarkan roadmap baru.

---

# POST-MIGRATION EXPANSION

# 33. Phase 26 — Tempo Detection

## Tujuan

Mendeteksi tempo/BPM audio lokal.

## Mode

```text
AUTO
deteksi BPM otomatis

MANUAL
user mengisi / mengoreksi BPM
```

## Arsitektur

Disarankan sebagai subsystem enrichment:

```text
Audio File
↓
Tempo Analyzer
↓
Detected BPM + confidence
↓
Metadata Enrichment Store
↓
UI
```

## Data

```text
detectedBpm
manualBpm
confidence
analysisVersion
lastAnalyzedAt
```

Prioritas:

```text
manual override
>
high-confidence detected
>
unknown
```

## Acceptance

```text
[ ] Analysis dapat dijalankan per track
[ ] Batch analysis opsional
[ ] Manual correction
[ ] Confidence tersedia
[ ] Tidak mengubah file asli tanpa persetujuan
```

---

# 34. Phase 27 — Lyrics Discovery

## Tujuan

Menemukan lyrics dari sumber eksternal dan/atau embedded metadata.

## Prioritas Sumber

```text
1. Embedded lyrics
2. Local sidecar lyrics (.lrc/.txt) bila didukung
3. Online discovery
4. Manual input/edit
```

## Mode

```text
AUTO DISCOVERY
MANUAL SEARCH
MANUAL EDIT
```

## Data

```text
lyricsText
lyricsType
source
language
isSynced
retrievedAt
manualOverride
```

## Catatan

Online lyrics provider harus dipilih dengan memperhatikan:

- API;
- terms of service;
- attribution;
- caching rules;
- copyright/legal constraints.

## Acceptance

```text
[ ] Embedded lyrics dapat dibaca
[ ] Manual lyrics dapat disimpan
[ ] Discovery dapat dipicu user
[ ] Source tercatat
[ ] No overwrite manual lyrics tanpa konfirmasi
```

---

# 35. Phase 28 — Metadata Discovery / Enrichment

## Tujuan

Mengisi metadata audio yang kosong/tidak lengkap.

Contoh:

```text
Unknown Artist
Unknown Album
filename-only title
missing track number
missing year
```

## Field Candidate

```text
title
artist
album
albumArtist
track
disc
year
genre
```

## Strategy

```text
Local Metadata
↓
Fingerprint / Search Candidate
↓
Confidence Matching
↓
Preview Changes
↓
User Confirm
↓
Store Enrichment
```

## Mode

```text
AUTO SUGGEST
MANUAL SEARCH
MANUAL EDIT
```

## Prinsip

Jangan langsung menulis ulang file audio pada fase pertama.

Gunakan NexPlay enrichment database/cache terlebih dahulu:

```text
Original MediaStore Metadata
+
NexPlay Enriched Metadata
=
Display Metadata
```

File write-back dapat menjadi fitur terpisah nanti.

## Acceptance

```text
[ ] Missing metadata dikenali
[ ] Candidate dapat ditemukan
[ ] Confidence tersedia
[ ] User dapat memilih/menolak
[ ] Manual override didukung
[ ] Original file tidak berubah otomatis
```

---

# 36. Phase 29 — Artwork Discovery / Enrichment

## Tujuan

Menampilkan artwork untuk audio yang:

- tidak memiliki embedded artwork;
- album art-nya kosong;
- artwork-nya salah;
- membutuhkan pilihan alternatif.

## Source Priority

```text
1. Embedded artwork
2. Existing Android/album artwork
3. Local folder image
4. Online discovery
5. Manual image selection
6. NexPlay fallback artwork
```

## Mode

```text
AUTO SUGGEST
MANUAL SEARCH
MANUAL SELECT
```

## Storage

Simpan sebagai NexPlay-owned cache/reference terlebih dahulu.

Jangan langsung menulis artwork ke file audio tanpa explicit user action.

## Acceptance

```text
[ ] Missing artwork dikenali
[ ] Candidate dapat ditampilkan
[ ] Manual selection tersedia
[ ] Cache policy jelas
[ ] Offline fallback tersedia
```

---

# 37. Phase 30 — Unified Audio Enrichment Layer

Setelah Tempo, Lyrics, Metadata, dan Artwork selesai, satukan melalui:

```text
Audio Enrichment Repository
```

Konsep:

```text
Original Media Metadata
        +
NexPlay Enrichment
        +
Manual Overrides
        ↓
Resolved Audio Metadata
```

Priority rule:

```text
Manual Override
>
Confirmed Enrichment
>
Original Metadata
>
Fallback
```

Subsystem:

```text
Tempo
Lyrics
Metadata
Artwork
```

tidak boleh langsung menulis UI sendiri.

---

# 38. Phase 31 — Batch Enrichment

Opsional setelah single-item enrichment stabil.

Feature:

```text
Scan missing metadata
Scan missing artwork
Detect tempo for selected songs
Discover lyrics for selected songs
Batch review
Approve / reject
```

Harus mempertimbangkan:

- network;
- rate limit;
- battery;
- background execution;
- user confirmation;
- provider policy.

---

# 39. Phase 32 — Optional File Write-Back

Ini fase terpisah dan **bukan default**.

Kemungkinan:

```text
Write title/artist/album to audio tags
Embed artwork
Write BPM
Write lyrics
```

Harus didesain sangat hati-hati karena menyentuh file user.

Syarat minimum:

```text
Preview
Backup strategy
Permission
Format compatibility
Failure rollback
Explicit confirmation
```

Jika tidak benar-benar diperlukan, enrichment cukup disimpan di database NexPlay.

---

# 40. Agent / Manual Handoff Protocol

Sebelum siapa pun mulai bekerja:

```text
1. Read MIGRATION_PLAN.md
2. Read MIGRATION_ROADMAP.md
3. Read MIGRATION_STATUS.md
4. Check git status
5. Check latest commits
6. Identify current task
```

Setelah selesai:

```text
1. Run verification
2. Record test result
3. Update MIGRATION_STATUS.md
4. Update CHANGELOG.md jika relevan
5. Commit
6. Record commit hash
```

---

# 41. Task Ownership Rules

Task status:

```text
TODO
READY
IN_PROGRESS
BLOCKED
REVIEW
DONE
```

Hanya satu owner aktif:

```text
owner: MANUAL
owner: AGENT
```

Jika handoff:

```text
owner: AGENT → MANUAL
```

wajib dicatat di `MIGRATION_STATUS.md`.

---

# 42. Definition of Done Global

Setiap task:

```text
[ ] Scope selesai
[ ] Tidak ada perubahan liar
[ ] Acceptance criteria terpenuhi
[ ] Verification pass
[ ] Relevant manual test selesai
[ ] Docs/status updated
[ ] Commit dibuat
```

Setiap phase:

```text
[ ] Semua task phase DONE
[ ] Integration check selesai
[ ] Regression check selesai
[ ] Roadmap status diperbarui
```

---

# 43. Prioritas Implementasi Singkat

Urutan final:

```text
00 Flutter Baseline Audit
01 Kotlin Bootstrap
02 App Shell
03 Media Model
04 Audio Scanner
05 Video Scanner
06 Media Repository
07 Folders
08 Queue Engine
09 Playback Engine
10 Queue + Playback Integration
11 Audio Player
12 MediaSession / Background Audio
13 Video Player
14 Search
15 PlayHub
16 Playlist
17 UI/UX Refinement
18 Settings
19 Functional Parity + Native UX Polish + Cleanup Audit
20 Merged into Phase 19 (no standalone execution)
21 Codec Compatibility
22 Performance
23 Production Gate
24 Cutover
25 Stabilization
26 Tempo Detection
27 Lyrics Discovery
28 Metadata Enrichment
29 Artwork Enrichment
30 Unified Enrichment Layer
31 Batch Enrichment
32 Optional File Write-Back
```

---

# 44. Prinsip Penutup

Roadmap ini sengaja menghindari big-bang rewrite.

NexPlay dibangun ulang sebagai rangkaian subsystem kecil:

```text
Build
↓
Verify
↓
Commit
↓
Document
↓
Continue
```

Dengan pola tersebut:

- developer dapat mengambil alih pekerjaan agent;
- agent dapat melanjutkan pekerjaan developer;
- setiap subsystem memiliki checkpoint;
- progress tidak bergantung pada satu sesi chat;
- regression lebih mudah dilacak;
- migrasi dapat berhenti sementara tanpa kehilangan arah.

Target akhirnya bukan sekadar:

> **NexPlay yang sama tetapi ditulis ulang dengan Kotlin.**

Target akhirnya adalah:

> **NexPlay native Android dengan arsitektur yang lebih jelas, playback yang lebih kuat, compatibility yang lebih terkendali, dan fondasi yang siap untuk fitur enrichment seperti tempo detection, lyrics discovery, metadata discovery, dan artwork discovery.**
