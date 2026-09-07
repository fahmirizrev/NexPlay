# MIGRATION_PLAN.md

# NexPlay Flutter → NexPlay Kotlin
## Bedahan Subsystem dan Arsitektur Migrasi

> Dokumen ini menjadi **peta arsitektur utama** untuk migrasi NexPlay dari `nexplay_flutter` ke implementasi native Android berbasis Kotlin.  
> Tujuannya bukan menerjemahkan Dart ke Kotlin baris demi baris, tetapi **membangun ulang NexPlay berdasarkan fungsi, perilaku, dan kontrak produk yang sudah terbukti**.

---

## 1. Tujuan Migrasi

Migrasi dilakukan untuk menghasilkan NexPlay native Android yang:

- lebih dekat dengan Android Media Framework;
- memiliki kontrol lebih besar terhadap audio, video, codec, lifecycle, MediaSession, background playback, dan device compatibility;
- tetap mempertahankan behavior NexPlay yang sudah stabil;
- memiliki arsitektur modular sehingga setiap subsystem dapat dikembangkan, diuji, dan diperbaiki secara independen;
- dapat dikerjakan secara **hybrid**:
  - manual oleh developer;
  - oleh coding agent;
  - kombinasi keduanya;
- tidak kehilangan progress ketika perpindahan pengerjaan terjadi antara manusia dan agent.

---

## 2. Prinsip Migrasi

### 2.1. Flutter adalah Living Specification

Selama migrasi, `nexplay_flutter` **tidak langsung dihapus**.

Fungsinya menjadi:

- referensi UI/UX;
- referensi behavior;
- referensi acceptance criteria;
- pembanding regression;
- baseline sebelum Kotlin mencapai feature parity.

Struktur repository yang disarankan:

```text
nexplay/
├── nexplay_flutter/          # Existing production/reference implementation
├── nexplay_kotlin/           # Native Kotlin rewrite
├── docs/
├── prompt/
│   ├── tasks/
│   └── output/
└── ...
```

---

### 2.2. Bukan Translasi Dart → Kotlin

Migrasi tidak dilakukan dengan pola:

```text
Flutter Widget
→ Kotlin equivalent

Flutter Service
→ Kotlin equivalent

Flutter plugin
→ Kotlin wrapper
```

Tetapi:

```text
Existing Behavior
        ↓
Define Contract
        ↓
Implement Native Solution
        ↓
Verify Against Flutter Baseline
```

---

### 2.3. Functional Parity Sebelum Visual Perfection

Migrasi dibagi menjadi dua lapisan besar:

```text
PASS 1
Functional Parity

PASS 2
Visual / UX Parity + Native Improvements
```

Jangan menghabiskan terlalu banyak waktu pada pixel-perfect UI ketika core subsystem belum stabil.

---

### 2.4 Repository Model — Development dan Release

NexPlay menggunakan contract satu project dengan dua repository yang memiliki tanggung jawab berbeda.

Current development contract:

```
Development repository
fahmirizrev/NexPlay-workspace

Local development root
C:\laragon\www\nexplay

Git remote
origin → NexPlay-workspace
```

Target release contract:

```
Release repository
fahmirizrev/NexPlay

Git remote
release → NexPlay
```

Rules:

- NexPlay-workspace adalah development source of truth;
- local root tetap C:\laragon\www\nexplay dan tidak wajib mengikuti nama repository GitHub;
- origin tetap menunjuk ke NexPlay-workspace;
- NexPlay adalah target repository release dan tidak menggantikan origin;
- release repository baru digunakan setelah Production Migration Gate dan Production Cutover mengizinkannya;
- application identity tetap NexPlay;
- applicationId tetap com.nexplay.app;
- nama repository tidak mengubah package, application identity, atau runtime architecture;
- Flutter reference tetap berada di development workspace sampai cutover selesai.

## 3. Stack Utama Kotlin

Stack awal yang direkomendasikan:

```text
Kotlin
Jetpack Compose
Compose state and animation primitives
Android Media3
ExoPlayer
MediaSession
MediaSessionService
MediaStore
ContentResolver
Room
DataStore
Coroutines
StateFlow
ViewModel
```

Tambahan library hanya boleh dimasukkan jika:

1. kebutuhan sudah jelas;
2. Android native API tidak cukup;
3. manfaatnya lebih besar daripada biaya dependency dan maintenance.

---

# 4. Bedahan NexPlay Menjadi Subsystem

NexPlay dibagi menjadi **12 subsystem utama**.

```text
NEXPLAY
│
├── 01. App Shell & UI/UX
├── 02. Media Scanner
├── 03. Media Library / Repository
├── 04. Playback Engine
├── 05. Queue Engine
├── 06. Audio Player
├── 07. Video Player
├── 08. Folders
├── 09. PlayHub
├── 10. Search
├── 11. Playlist
└── 12. Android Media Integration
```

Hubungan antarsubsystem:

```text
                     ┌──────────────┐
                     │ MediaScanner │
                     └──────┬───────┘
                            ↓
                  ┌───────────────────┐
                  │   Media Library   │
                  │    Repository     │
                  └────────┬──────────┘
                           │
           ┌───────────────┼────────────────┐
           ↓               ↓                ↓
        Folders          Search          PlayHub
           │               │                │
           └───────────────┬────────────────┘
                           ↓
                      Queue Engine
                           ↓
                    Playback Engine
                     ↙            ↘
               Audio Player     Video Player
                     │            │
                     └─────┬──────┘
                           ↓
                 Android MediaSession
                           ↓
                 Notification / System
```

---

# 5. Subsystem 01 — App Shell & UI/UX

## Tanggung Jawab

Subsystem ini menangani tampilan dan navigasi aplikasi.

Mencakup:

- theme;
- typography;
- color system;
- spacing;
- iconography;
- top bar;
- bottom navigation;
- sheets;
- dialogs;
- loading state;
- empty state;
- error state;
- presentation state;
- root/nested/overlay/vertical presentation layers;
- bottom navigation;
- transitions;
- gesture-ready motion boundaries;
- reusable Compose components.

## Tidak Boleh Menangani

UI tidak boleh:

- membaca `MediaStore` langsung;
- mengontrol `ExoPlayer` langsung;
- menentukan queue sendiri;
- menyimpan playlist langsung ke database;
- menangani codec;
- menjalankan logic scanner.

## Contract

UI menerima state dan mengirim event.

Contoh:

```text
UI
↓
ViewModel
↓
Use Logic / Repository / Controller
```

## Acceptance Criteria

- semua screen utama dapat dinavigasikan;
- back behavior konsisten;
- root, nested, overlay, vertical foreground, dan bottom bar memiliki motion ownership yang jelas;
- bottom bar muncul/hilang tanpa mengubah geometry root content;
- theme konsisten;
- visual direction tetap flat, clean, minimalist, dan restrained;
- reusable component tersedia;
- tidak ada direct Android media access dari UI.

## Current Native Presentation Contract (PHASE 07 + PHASE 17 UI/UX Refinement)

PHASE 07 menetapkan presentation shell native dengan independent motion ownership.

Current structure:

- Root Layer:
  - Folders;
  - Playhub.
- Nested Layer:
  - Folder Detail.
- Overlay Layer:
  - Search.
- Vertical Layer:
  - Playlist;
  - Player.
- Bottom Bar Layer.

Prinsip yang berlaku:

- Folders dan Playhub merupakan peer root tabs dan tetap composed selama root transition;
- root-tab transition menggunakan shared progress;
- Folder Detail memiliki hierarchical motion sendiri;
- Search memiliki reversible overlay motion sendiri;
- bottom bar bergerak independen tanpa mengubah geometry root content;
- presentation state tidak lagi membutuhkan Navigation Compose;
- motion architecture harus tetap gesture-ready tanpa menambah motion engine atau abstraction layer yang tidak perlu;
- visual native menggunakan flat surfaces, restrained tonal color, restrained shapes, dan minimal elevation;
- Folders dan Folder Detail menggunakan shared list-row geometry;
- leading media/folder visual dan trailing action menggunakan balanced optical edge contract;
- Chevron, MoreVert, dan header trailing action berada pada shared trailing optical axis.

### PHASE 17 UI/UX Refinement #1

PHASE 17 tidak lagi digunakan untuk History / Recently Played.

Product direction PHASE 17 sekarang adalah iterative UI/UX refinement sebelum tahap parity audit dan broad polish berikutnya.

Checkpoint #1 mencakup:

- active typography kembali menggunakan default Android / Compose typography dan tidak lagi memaksa bundled Sora FontFamily;
- System / Light / Dark menjadi explicit theme choices pada Folders overflow;
- Audio Player memperoleh refined visual hierarchy tanpa mengubah playback architecture;
- Audio Player mempertahankan canonical QueueState dan PlaybackState;
- Audio Player title menggunakan one-line running marquee;
- artwork fallback tetap digunakan dan dipusatkan pada player;
- Control Deck menggunakan circular controls dengan caption;
- main transport menggunakan refined Previous / Play-Pause / Next presentation;
- Audio Player identity dan footer ditambahkan sebagai presentation-only elements;
- Playlist Detail Play All menggunakan white foreground content;
- Playlist Detail menggunakan dedicated drag handle untuk reorder;
- playlist reorder menggunakan native frame-driven interaction tanpa third-party reorder dependency;
- reorder tetap dipersist melalui existing PlaylistRepository;
- Room tidak ditulis pada setiap drag frame; final reorder dipersist setelah drop;
- stale drag callback setelah item berpindah index dihindari dengan current callback state;
- playlist item dapat di-drag kembali setelah reorder sebelumnya;
- playlist-row direct delete action diganti menjadi 3-dots menu;
- playlist-row 3-dots menyediakan Delete dan Add to Playlist;
- playlist-row 3-dots mengikuti shared right-edge optical axis yang sama dengan header dan Folder Detail;
- Playlist Detail menggunakan dedicated 320 ms entry/exit motion sehingga perceived transition lebih proporsional;
- Queue Engine, Playback Engine, PlaylistContext audio-first policy, Room schema, dan PlaylistRepository ownership tetap tidak berubah.

Verification checkpoint #1:

```text
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] git diff --check PASS
[x] Audio Player refinement diterima pada runtime
[x] Playlist drag reorder diterima pada runtime
[x] Reorder kedua setelah previous reorder bekerja
[x] Playlist row 3-dots alignment diterima
[x] Playlist Detail motion refinement diterima
```

### PHASE 17 UI/UX Refinement #2

Checkpoint #2 melanjutkan refinement presentation, interaction, dan native application branding tanpa mengubah canonical playback, queue, playlist persistence, atau media architecture.

Checkpoint #2 mencakup:

- Bottom Navigation menggunakan floating overlay presentation;
- visible Bottom Navigation surface menggunakan height 64dp;
- floating footprint mempertahankan vertical spacing sehingga total footprint tetap 80dp;
- root viewport tidak lagi di-resize oleh Bottom Navigation;
- Folders dan PlayHub content dapat scroll di belakang floating Bottom Navigation;
- trailing list clearance menjaga item terakhir tetap dapat diakses;
- Audio Mini Player dan Bottom Navigation menggunakan shared 64dp floating-surface geometry;
- transparency eksperimen pada floating surfaces dihapus;
- Bottom Navigation dan Audio Mini Player menggunakan opaque application background;
- kedua floating surfaces menggunakan 2dp outline;
- floating surfaces tetap tanpa shadow dan tanpa blur;
- dark application background diselaraskan menjadi #111317;
- light application background tetap mempertahankan existing light presentation;
- PlayHub playlist row sekarang memiliki full-width tap / pressed surface;
- Playlist Detail media row sekarang memiliki full-width tap / pressed surface;
- horizontal icon dan text alignment tetap dipertahankan dengan screen inset di dalam clickable row;
- Folders dan Folder Detail tetap menjadi reference full-width list-row interaction;
- Playlist Detail drag handle dan reorder behavior tidak berubah;
- PlaylistRepository tetap menjadi persistence boundary;
- current NexPlay Flutter launcher branding digunakan sebagai identity / parity reference;
- native Kotlin launcher icon menggunakan branded density assets;
- adaptive launcher icon tersedia;
- round launcher icon tersedia;
- monochrome / themed launcher reference tersedia;
- Play Store launcher source asset tersedia;
- Android Studio default launcher artwork tidak lagi menjadi active launcher presentation;
- pre-Android-12 branded launch background tersedia;
- Android 12+ menggunakan native system splash configuration;
- splash background menggunakan current Kotlin dark background #111317;
- MainActivity berpindah dari launch theme ke normal NexPlay theme saat startup;
- tidak ada fake startup delay;
- tidak ada second-stage Compose bootstrap splash;
- tidak ada dependency icon atau splash generator baru.

Architecture guardrails:

```text
MediaStore
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
Single ExoPlayer
    ↓
Audio / Video Player / MediaSession
```

Checkpoint #2 tidak mengubah:

- Queue Engine ownership;
- Playback Engine ownership;
- ExoPlayer ownership;
- MediaSession ownership;
- MediaLibraryRepository ownership;
- PlaylistRepository ownership;
- Room schema;
- PlaylistContext audio-first policy;
- persistent History / Recently Played scope.

Verification checkpoint #2:

```text
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] git diff --check PASS
[x] Floating Bottom Navigation accepted in dark mode
[x] Floating Bottom Navigation accepted in light mode
[x] Audio Mini Player floating-surface treatment accepted
[x] PlayHub full-width playlist-row interaction accepted
[x] Playlist Detail full-width media-row interaction accepted
[x] Flutter to Kotlin branding asset hash checks PASS
[x] APK install PASS
[x] MainActivity cold launch PASS
[x] NexPlay launcher icon visually verified on physical Android device
[x] Android Studio default launcher icon no longer active
```

Native splash resources dan cold-start path telah terverifikasi melalui build, install, dan runtime launch.

Broad OEM-specific launcher mask dan splash rendering matrix belum menjadi bagian checkpoint ini.

PHASE 17 current refinement pass ditutup pada checkpoint #2. Functional parity, additional native UX polish, dan cleanup audit selanjutnya digabungkan ke PHASE 19; PHASE 20 tidak dijalankan sebagai phase terpisah.

### PHASE 18 — Settings dan DataStore

PHASE 18 memindahkan application-owned preferences ke AndroidX Preferences DataStore tanpa mengambil alih state yang dimiliki subsystem lain.

Persistence boundary:

```text
NexPlayApplication
        ↓
NexPlayPreferencesRepository
        ↓
Preferences DataStore
```

Preference yang dipersist:

- theme mode;
- Folder sort field;
- Folder sort direction;
- Folder Detail media sort field;
- Folder Detail media sort direction.

Default values:

```text
Theme
SYSTEM

Folder sort
NAME + ASCENDING

Folder Detail media sort
NAME + ASCENDING
```

Existing theme preference dimigrasikan dari legacy SharedPreferences nexplay_theme_preferences dengan key theme_mode menggunakan SharedPreferencesMigration.

Current System / Light / Dark control tetap menjadi producer theme preference.

Current Folders sort menu tetap menjadi producer Folder sort preference.

Current Folder Detail sort menu tetap menjadi producer media sort preference.

DataStore hanya menyimpan small application preference values dan tidak menyimpan canonical media, playlist content, queue items, playback position, atau media metadata besar.

Persistence classification:

```text
PERSISTED IN DATASTORE
Theme mode
Folder sort field + direction
Folder Detail media sort field + direction

KEEP RUNTIME-ONLY
Repeat mode
Shuffle
Current root tab
Current queue
Playback state

KEEP SEPARATE
Child Lock PIN

DEFERRED
Playback speed
Centralized Settings screen
```

QueueEngine tetap authoritative untuk shuffle dan repeat runtime state.

Root tab tetap presentation/navigation session state dan startup tetap dimulai dari Folders.

Child Lock unlock pattern tetap menggunakan dedicated Child Lock preference storage dan tidak dipindahkan ke general application settings pada PHASE 18.

Playback speed belum diimplementasikan sehingga tidak dibuat preference prematur.

Startup flow juga diperbaiki agar gesture-navigation system area tetap menggunakan dark launch appearance ketika menunggu first DataStore emission.

MainActivity sekarang menerapkan dark edge-to-edge startup state sebelum DataStore selesai resolve, kemudian existing Compose SideEffect menerapkan actual System / Light / Dark preference.

Architecture guardrails:

- QueueEngine ownership tidak berubah;
- PlaybackEngine ownership tidak berubah;
- ExoPlayer ownership tidak berubah;
- MediaSession ownership tidak berubah;
- PlaylistRepository ownership tidak berubah;
- Room schema tidak berubah;
- Child Lock behavior tidak berubah;
- tidak ada media data besar di DataStore.

Verification PHASE 18:

```text
[x] Theme preference persists across app restart
[x] Folder sort field and direction persist across app restart
[x] Folder Detail media sort field and direction persist across app restart
[x] Explicit default values are available
[x] Legacy theme preference migration path is implemented
[x] Repeat and shuffle remain QueueState runtime ownership
[x] Root startup remains Folders
[x] Child Lock preference remains separately owned
[x] Startup gesture-navigation light flash regression fixed
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] assembleDebugAndroidTest PASS
[x] git diff --check PASS
```

PHASE 18 selesai tanpa perubahan pada Room, media library persistence, queue architecture, atau playback architecture.

### PHASE 19 — Functional Parity + Native UX Polish + Cleanup Audit

**Status: COMPLETED + VERIFIED — 2026-09-06**

PHASE 19 menggabungkan functional parity audit, native UX polish, dan targeted dead-code/resource cleanup.

Flutter tetap digunakan sebagai behavioral/reference baseline. Kotlin tidak dipaksa mengikuti Flutter secara pixel-perfect dan tidak wajib mempertahankan behavior yang secara eksplisit telah diputuskan tidak diperlukan.

Prinsip classification:

```text
MATCHED
IMPROVED
MISSING
INTENTIONALLY CHANGED
DEFERRED
BLOCKED
```

Checkpoint yang selesai:

1. **Dead Resource Cleanup**
   - unused Android Studio launcher template resources dihapus;
   - bundled Sora fonts yang tidak lagi digunakan dihapus;
   - Sora OFL resource yang tidak lagi diperlukan dihapus.

2. **Mixed Folder / Search Queue Parity**
   - Folder Detail menggunakan full mixed audio/video sorted queue;
   - Search membangun queue dari full mixed source folder;
   - QueueEngine dan QueuePlaybackCoordinator tetap canonical queue path.

3. **Media Library Lifecycle**
   - cold launch tetap menggunakan existing initial scan;
   - background → foreground melakukan fresh MediaStore refresh;
   - permission refresh dan pull-to-refresh tetap bekerja;
   - persistent cached-first media index tidak ditambahkan.

4. **Android Open With / ACTION_VIEW**
   - audio/video external ACTION_VIEW didukung;
   - cold-start external intent didukung;
   - warm-start onNewIntent didukung;
   - external media dipetakan ke canonical MediaItem;
   - external media menggunakan SingleItemContext;
   - playback tetap melalui QueuePlaybackCoordinator dan single ExoPlayer runtime.

5. **Final Parity Classification + Cleanup**
   - tidak ada parity-critical gap yang tersisa;
   - obsolete PLAYLIST / PLAYER vertical states dihapus;
   - unreachable vertical placeholder layer dihapus;
   - unreachable placeholder screen dihapus.

Final product decisions:

```text
INTENTIONALLY CHANGED

Cached-first startup
→ tidak direplikasi di Kotlin

Picture-in-Picture
→ tidak diterapkan di Kotlin

Floating video mini player
→ tidak diterapkan di Kotlin

Video Player direct close
→ menghentikan playback runtime

PlayHub
→ Queue + Playlists only

PHASE 20 standalone UI/UX polish
→ merged into PHASE 19
→ no standalone execution
```

Deferred:

```text
Playback speed
Persistent Recently Played / History
Playback queue snapshot / relaunch resume
Share
Media Details
Real artwork
Dedicated Settings screen
Broad codec / old-device hardening
```

Architecture tetap:

- QueueEngine = canonical queue owner;
- QueuePlaybackCoordinator = queue/playback integration boundary;
- PlaybackEngine = single ExoPlayer runtime owner;
- MediaSession menggunakan playback runtime yang sama;
- PlaylistRepository / Room tetap persistence boundary untuk playlist;
- Preferences DataStore tetap untuk small application-owned preferences;
- Child Lock persistence tetap terpisah;
- tidak ada dependency baru;
- tidak ada Room schema change;
- tidak ada playback architecture replacement.

Final verification:

```text
[x] Checkpoint #1 resource cleanup verified
[x] Checkpoint #2 mixed Folder/Search runtime accepted
[x] Checkpoint #3 media resume refresh runtime accepted
[x] Checkpoint #4 Android Open With runtime accepted
[x] Checkpoint #5 placeholder cleanup completed
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] assembleDebugAndroidTest PASS
[x] git diff --check PASS
```

Implementation commit:

```text
526c78d feat(parity): complete native parity and UX pass
```

PHASE 20 resmi digabungkan ke PHASE 19 dan tidak memiliki standalone implementation pass.
---

### PHASE 22 — Performance, Media Visuals, dan Mini Player Surface Contract

PHASE 22 ditutup sebagai practical performance dan responsiveness validation phase sekaligus final UI hardening pass sebelum Production Migration Gate.

Performance acceptance menggunakan real user-facing flows, bukan synthetic benchmark target.

Primary runtime acceptance:

```
Redmi Note 7
→ legacy-device acceptance

Infinix Hot 10 Play
→ low-end / vendor-diversity acceptance

Poco F5
→ modern regression control
```

Accepted runtime behavior:

- startup tidak terasa blocking pada Redmi Note 7;
- startup tidak terasa blocking pada Infinix Hot 10 Play;
- Folders scrolling responsive;
- Folder Detail scrolling responsive;
- Search typing/filtering responsive;
- PlayHub, Queue, dan Playlist responsive;
- Audio Player open/close responsive;
- Video Player open/close responsive;
- mixed Audio ↔ Video transition stabil;
- background/resume tidak membuat UI freeze;
- tidak ada obvious memory/resource issue pada tested flows;
- playback tetap stabil;
- architecture ownership tidak berubah;
- Poco F5 tidak menunjukkan regression.

Final media-visual hardening menutup presentation gap yang tersisa sebelum Production Migration Gate.

Implemented shared media visual path:

```
Canonical MediaItem
        ↓
MediaVisualLoader
        ↓
bounded in-memory bitmap cache
        ↓
MediaVisualBox / MediaThumbnailBox
        ↓
UI surfaces
```

Media visual contract:

- media visual dimuat lazy ketika UI membutuhkannya;
- decode/load tidak dimasukkan ke MediaStore scan;
- audio menggunakan embedded artwork ketika tersedia;
- video menggunakan Android media thumbnail/frame path;
- memory cache dibatasi;
- concurrent load dibatasi;
- tidak ada disk artwork cache baru;
- tidak ada dependency image-loader baru;
- missing atau invalid visual menggunakan existing safe fallback;
- canonical MediaItem tidak diubah;
- Room schema tidak diubah;
- Queue Engine tidak diubah;
- Playback Engine tidak diubah;
- ExoPlayer ownership tidak diubah.

Real media visual sekarang digunakan pada:

- Folder Detail media rows;
- Search media rows;
- Playlist Detail media rows;
- PlayHub current Queue card;
- dedicated Queue Screen rows;
- Full Audio Player artwork;
- Audio Mini Player artwork.

Playlist collection rows tetap menggunakan playlist identity icon karena row tersebut merepresentasikan playlist, bukan satu media item.

Queue card presentation sekarang menggunakan stable card height yang sama untuk empty maupun populated state sehingga PlayHub layout tidak berubah secara mencolok ketika queue berpindah state.

Audio Mini Player visibility contract:

```
SHOW
Folders
Folder Detail
Queue Screen
Playlist Detail

HIDE
PlayHub
Search
Full Audio Player
Video Player
```

Mini Player visibility ditentukan oleh foreground presentation state, bukan hanya root tab asal.

Queue dan Playlist yang dibuka dari PlayHub tetap dapat menampilkan Audio Mini Player.

Search tidak menampilkan Audio Mini Player.

Full Audio Player dan Video Player tetap menjadi foreground player surface dan tidak menampilkan Mini Player di atas dirinya sendiri.

Android system media artwork belum menjadi bagian dari media-visual UI pass ini; system media surfaces tetap menggunakan existing MediaSession metadata/fallback contract.

Verification PHASE 22 closeout:

```
[x] testDebugUnitTest PASS
[x] lintDebug PASS
[x] assembleDebug PASS
[x] assembleDebugAndroidTest PASS
[x] git diff --check PASS
[x] Redmi Note 7 practical performance acceptance PASS
[x] Infinix Hot 10 Play practical performance acceptance PASS
[x] Poco F5 regression acceptance PASS
[x] Media thumbnails/artwork accepted at runtime
[x] Queue empty/populated card geometry accepted
[x] Audio Mini Player surface policy accepted
[x] Playback behavior regression check accepted
[x] Queue/Playback architecture ownership preserved
```

PHASE 22 tidak mengklaim universal benchmark, exhaustive profiler coverage, atau performance guarantee untuk seluruh Android/OEM combination.

Future reproducible performance regression harus diprofilkan dan diperbaiki secara targeted berdasarkan actual evidence.

---
# 6. Subsystem 02 — Media Scanner

## Tanggung Jawab

Media Scanner bertugas **menemukan media lokal yang tersedia di device**.

Sumber utama:

```text
MediaStore
+
ContentResolver
```

## Audio Data

Minimal:

```text
id
uri
displayName
title
artist
album
duration
mimeType
folder
size
dateAdded
dateModified
```

## Video Data

Minimal:

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
width
height
resolution
```

## Output

Scanner menghasilkan data mentah terstruktur:

```text
List<MediaItem>
```

## Tidak Boleh Menangani

Scanner tidak boleh:

- memiliki UI;
- menentukan queue;
- melakukan playback;
- menyimpan playlist;
- menentukan search result presentation.

## Acceptance Criteria

- audio terbaca;
- video terbaca;
- folder dikenali;
- media type dapat dibedakan;
- broken/invalid media tidak membuat aplikasi crash;
- refresh library bekerja;
- permission Android modern ditangani benar.

---

# 7. Subsystem 03 — Media Library / Repository

## Fungsi

Media Library adalah **canonical representation** seluruh media di NexPlay.

Scanner hanya mencari file.  
Library menentukan bagaimana seluruh NexPlay mengakses media.

## API Konseptual

```text
getAllMedia()
getAudio()
getVideos()
getFolders()
getMediaInFolder()
getRecentlyAdded()
getByUri()
search()
refresh()
```

## Prinsip

```text
UI
↓
MediaRepository
↓
MediaScanner
↓
Android MediaStore
```

UI tidak mengakses `MediaStore` secara langsung.

## Model Dasar

Contoh konsep:

```kotlin
sealed interface MediaItem {
    val id: Long
    val uri: Uri
    val title: String
    val durationMs: Long
}
```

Kemudian:

```text
AudioItem
VideoItem
```

## Acceptance Criteria

- satu canonical media model digunakan seluruh aplikasi;
- scanner terisolasi dari UI;
- folders/search/playhub memakai repository yang sama;
- media refresh tidak menghasilkan duplicate;
- stale/deleted item dapat ditangani.

---

# 8. Subsystem 04 — Playback Engine

## Fungsi

Playback Engine adalah **mesin pemutar media**.

Teknologi utama:

```text
Android Media3
ExoPlayer
```

## Tanggung Jawab

- prepare;
- play;
- pause;
- stop;
- seek;
- current position;
- duration;
- buffering state;
- playback state;
- playback error;
- playback speed;
- repeat-mode integration;
- source preparation;
- decoder / playback diagnostics.

## Tidak Boleh Menangani

Playback Engine tidak boleh:

- menentukan item berikutnya secara independen;
- membuat screen player;
- menyimpan playlist;
- melakukan media discovery.

## State Konseptual

```text
currentMedia
isPlaying
isBuffering
positionMs
durationMs
playbackError
```

## Acceptance Criteria

- audio dapat diputar;
- video dapat diputar;
- seek bekerja;
- pause/resume stabil;
- player state observable;
- playback error terpapar sebagai state, bukan crash;
- player lifecycle jelas.

## Current Native Playback Contract (PHASE 09)

PHASE 09 menetapkan native Playback Engine berbasis Android Media3 / ExoPlayer sebagai runtime boundary yang terpisah dari Queue Engine dan UI.

Current native contract:

- `PlaybackEngine` memiliki satu private `ExoPlayer`;
- raw `ExoPlayer` tidak diekspos ke Compose atau Player UI;
- engine menerima satu canonical `MediaItem` pada satu waktu;
- audio dan video menggunakan runtime ExoPlayer yang sama;
- playback state diekspos melalui `StateFlow<PlaybackState>`;
- state mencakup:
  - current media;
  - playback status;
  - playing state;
  - current position;
  - duration;
  - playback error;
- runtime status mencakup:
  - IDLE;
  - PREPARING;
  - BUFFERING;
  - READY;
  - ENDED;
  - ERROR;
- `prepare()` memasang source tanpa autoplay;
- `play()` memulai atau melanjutkan playback;
- `pause()` menghentikan playback sementara tanpa menghapus source;
- `stop()` menghentikan playback dan membersihkan current runtime source;
- `seek()` melakukan clamped seek berdasarkan duration yang tersedia;
- position state dipublikasikan secara periodik hanya ketika playback aktif;
- canonical MediaStore `content://` URI dapat digunakan langsung oleh Media3;
- URI berscheme lain tetap didukung;
- raw file path tanpa scheme dikonversi menjadi file URI;
- invalid source menjadi `PlaybackErrorKind.SOURCE`;
- Media3 runtime failure menjadi `PlaybackErrorKind.RUNTIME`;
- playback error dipublikasikan sebagai state dan tidak dijadikan application crash;
- `release()` menjadi explicit lifecycle boundary untuk ExoPlayer;
- Playback Engine tidak memiliki:
  - queue list;
  - current queue index;
  - next/previous policy;
  - shuffle policy;
  - repeat completion policy;
  - MediaStore discovery;
  - MediaSession;
  - Player UI ownership.

PHASE 10 menetapkan integration boundary untuk hubungan:

```text
Queue selects item
↓
Playback prepares/plays item

Playback reaches end
↓
Queue resolves next item
↓
Playback plays next item
```

Repeat completion behavior, queue synchronization, dan end-of-track integration tidak dipindahkan ke Playback Engine.

Video rendering surface juga belum menjadi responsibility PHASE 09. PHASE 09 hanya menyediakan engine-level video playback capability; native Video Player presentation tetap dikerjakan pada phase player terkait.

---

# 9. Subsystem 05 — Queue Engine

## Fungsi

Queue menentukan **apa yang dimainkan dan dalam urutan apa**.

Playback Engine memainkan media.  
Queue Engine mengelola konteks playback.

## Tanggung Jawab

- queue list;
- current index;
- current item;
- previous;
- next;
- append;
- insert next;
- remove;
- reorder;
- replace queue;
- shuffle;
- repeat;
- queue restoration bila dibutuhkan.

## Queue ≠ Playlist

### Queue

Temporary playback context:

```text
A
B ← CURRENT
C
D
```

### Playlist

Persistent user collection:

```text
WORKOUT
├── A
├── F
└── K
```

Saat playlist dimainkan:

```text
Playlist
↓
Create Queue
↓
Playback
```

## Playback Context

Disarankan ada konsep eksplisit:

```text
PlaybackContext
```

Contoh:

```text
FolderContext
PlaylistContext
SearchContext
PlayHubContext
SingleItemContext
```

Dengan ini behavior Prev/Next dapat mempertahankan konteks asal.

## Acceptance Criteria

- next/previous konsisten;
- queue dapat diganti;
- queue dapat dimodifikasi;
- shuffle tidak merusak current item;
- repeat bekerja;
- context asal media tetap diketahui;
- search/folder/playlist dapat membuat queue dengan benar.

## Current Native Queue Contract (PHASE 08)

PHASE 08 menetapkan Queue Engine sebagai pure domain boundary sebelum Media3 playback diimplementasikan.

Current native contract:

- `QueueEngine` menjadi satu-satunya pemilik queue state;
- queue state diekspos melalui `StateFlow<QueueState>`;
- `currentItem` diturunkan dari `items` dan `currentIndex`, bukan disimpan sebagai state terpisah;
- queue mendukung mixed canonical `MediaItem` audio dan video;
- manual next/previous mengikuti queue order;
- `RepeatMode.ALL` melakukan wrap pada manual next/previous;
- `RepeatMode.ONE` tetap tidak memengaruhi manual navigation dan completion behavior sekarang diselesaikan melalui PHASE 10 integration boundary;
- shuffle mempertahankan current item;
- disable shuffle mengembalikan original queue order ketika restore snapshot masih valid;
- explicit queue mutation saat shuffle aktif membatalkan stale pre-shuffle restore order;
- queue mutation mempertahankan current item bila item tersebut masih tersedia;
- `clear()` menghapus queue/current/context tetapi mempertahankan shuffle/repeat preference;
- playback origin direpresentasikan secara eksplisit melalui:
  - `FolderContext`;
  - `PlaylistContext`;
  - `SearchContext`;
  - `PlayHubContext`;
  - `SingleItemContext`;
- Queue Engine tidak memiliki dependency ke Android UI, Media3, ExoPlayer, MediaSession, persistence, atau MediaStore;
- Folders/Search/Playlist tetap menjadi consumer yang membentuk queue; Queue Engine tidak mengetahui UI producer tersebut.

## Current Native Queue + Playback Integration Contract (PHASE 10)

PHASE 10 menetapkan explicit integration boundary antara pure domain `QueueEngine` dan runtime `PlaybackEngine`.

Current native contract:

- `QueuePlaybackCoordinator` menjadi integration boundary antara queue dan playback;
- coordinator memiliki satu `QueueEngine` dan satu `PlaybackEngine`;
- Queue Engine tetap menjadi satu-satunya owner:
  - queue items;
  - current index;
  - current item;
  - shuffle;
  - repeat mode;
  - playback context;
- Playback Engine tetap menjadi owner:
  - ExoPlayer;
  - runtime prepare/play/pause/seek;
  - playback position;
  - duration;
  - buffering;
  - playback status;
  - playback error;
- coordinator tidak menyimpan second atau hidden queue;
- queue selection disinkronkan ke Playback Engine melalui canonical `MediaItem`;
- manual `playAt()`, `next()`, dan `previous()` mengubah queue terlebih dahulu, kemudian playback mengikuti selected item;
- structural queue mutation yang mempertahankan current media tidak memaksa restart playback;
- `clear()` mengosongkan queue dan menghentikan current playback;
- mixed audio/video queue tetap menggunakan canonical queue order;
- completion policy tetap dimiliki Queue Engine melalui `QueueCompletionAction`:
  - `STOP`;
  - `REPLAY_CURRENT`;
  - `ADVANCED`;
- `RepeatMode.OFF`:
  - advance ketika masih ada next item;
  - stop pada final queue boundary;
- `RepeatMode.ONE`:
  - replay current item;
  - current index tidak berubah;
- `RepeatMode.ALL`:
  - wrap ke item pertama pada final boundary;
  - replay current item untuk single-item queue;
- Playback Engine tidak mengetahui repeat mode atau queue boundary;
- coordinator mengamati `PlaybackStatus.ENDED` dan meminta Queue Engine menentukan completion action;
- completion hanya diproses ketika media dari playback event masih sama dengan `QueueState.currentItem`;
- stale `ENDED` callback dari source sebelumnya diabaikan;
- stale-completion guard mencegah navigation baru dibatalkan oleh callback runtime lama saat source sedang diganti;
- coordinator me-release playback runtime secara eksplisit melalui `release()`;
- coordinator tidak memiliki responsibility untuk:
  - Compose Player UI;
  - MediaSession;
  - background playback;
  - persistence;
  - MediaStore discovery.

Verified PHASE 10 flow:

```text
Queue selects item
↓
QueuePlaybackCoordinator
↓
PlaybackEngine prepares/plays item

Playback reaches ENDED
↓
QueuePlaybackCoordinator validates current media
↓
QueueEngine resolves completion action
↓
STOP / REPLAY_CURRENT / ADVANCED
↓
Playback runtime follows queue decision
```

User-facing Audio Player UI selesai dan terverifikasi pada PHASE 11.

Native Video Player UI selesai dan terverifikasi pada PHASE 13.

MediaSession dan background playback selesai dan terverifikasi pada PHASE 12.

---

# 10. Subsystem 06 — Audio Player

## Fungsi

Audio Player adalah **presentation layer** untuk audio playback.

## Elemen

- artwork;
- title;
- artist;
- album;
- seek bar;
- elapsed time;
- duration;
- play/pause;
- previous;
- next;
- shuffle;
- repeat;
- queue access;
- add to playlist;
- optional playback speed.

## Prinsip

Audio Player tidak memiliki `ExoPlayer`.

Contoh:

```text
Play button
↓
PlaybackController.play()

Next button
↓
QueueController.next()
```

## Acceptance Criteria

- player UI mengikuti state nyata;
- seek sinkron;
- next/previous sinkron dengan queue;
- artwork fallback tersedia;
- UI tidak memiliki playback lifecycle sendiri.

---

# 11. Subsystem 07 — Video Player

## Fungsi

Video Player menangani presentation dan interaction untuk canonical VideoItem tanpa memiliki playback runtime sendiri.

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

## Current Native Video Player Contract (PHASE 13 + PHASE 15 Queue Refinement)

Native Video Player berbasis Media3 PlayerView tetap berada di atas canonical playback architecture.

Current native contract:

- Video Player menggunakan Media3 Player yang berasal dari QueuePlaybackCoordinator;
- Video Player tidak membuat ExoPlayer kedua;
- Video Player tidak memiliki queue kedua atau hidden playback state;
- video yang dipilih dari Folder Detail membentuk canonical video queue dengan FolderContext;
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
- Audio Only untuk VideoItem dapat diminimize menjadi Audio Mini Player;
- Audio Only → Video mengembalikan presentation ke Video Player tanpa membuat playback baru;
- direct Video Player exit menghentikan runtime playback melalui stopPlayback() tanpa menghapus retained QueueState;
- stop playback tidak lagi identik dengan clear queue;
- playback error dari Playback Engine ditampilkan sebagai Video Player error state;
- Child Lock tersedia dari menu Video Player;
- Child Lock memblokir player tap, double tap, transport controls, dan Android Back di dalam player;
- Child Lock menggunakan Android startLockTask() / Screen Pinning sebagai OS-level protection untuk aplikasi biasa;
- Child Lock bukan Device Owner / managed-device kiosk mode;
- Child Lock menggunakan persistent local 4-digit PIN;
- first use meminta Set PIN dan Confirm PIN;
- penggunaan berikutnya memakai PIN yang sudah tersimpan;
- unlock affordance membutuhkan hold sekitar 2 detik sebelum PIN verification muncul;
- PIN salah mempertahankan Child Lock;
- PIN benar keluar dari Child Lock dan menghentikan Lock Task jika sedang aktif;
- Change / Reset Child Lock PIN UI ditunda sampai Settings surface yang tepat tersedia;
- practical codec/device compatibility validation diselesaikan pada PHASE 21 tanpa menambah decoder/playback path kedua.

## Acceptance Criteria

```
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
[x] Video exit menghentikan runtime tanpa menghapus retained queue
[x] System-bar presentation menyatu dengan Video Player
[x] Playback error dapat ditampilkan
[x] Child Lock tersedia
[x] Persistent 4-digit Child Lock PIN tersedia
[x] Long-hold + PIN unlock tersedia
[x] Video Player bukan pemilik ExoPlayer
[x] Video Player bukan pemilik queue
[ ] Broad codec / old-device compatibility matrix
```

Broad codec compatibility bukan blocker PHASE 13/15. PHASE 21 kemudian menutup practical old-device compatibility validation tanpa mengklaim exhaustive universal codec, profile, atau level certification.
# 12. Subsystem 08 — Folders

## Fungsi

Folders adalah **view terhadap Media Library**, bukan media scanner baru.

Konsep:

```text
Media Library
↓
groupBy(folder)
↓
Folders UI
```

## Behavior Penting

Saat user membuka folder lalu memilih item:

```text
Folder Contents
↓
Build Queue from Folder
↓
Set Selected Item as Current
↓
Playback
```

Dengan demikian:

```text
Prev / Next
```

mengikuti konteks folder asli.

## Acceptance Criteria

- folder list benar;
- isi folder benar;
- audio/video dibedakan sesuai kebutuhan UX;
- tapping media membentuk playback context yang benar;
- back navigation kembali ke folder, bukan kehilangan context.

---

## PHASE 16 Folder Detail Selection Refinement

PHASE 16 menambahkan selection mode pada Folder Detail sebagai entry point utama untuk membuat dan menambah media ke playlist.

Current contract:

- long press media row memasuki selection mode;
- media yang di-long-press langsung menjadi selected;
- ketika selection mode aktif, tap media hanya toggle selection dan tidak memulai playback;
- selected media ditentukan berdasarkan canonical URI;
- selected row menggunakan muted selected background;
- selected row menampilkan selected indicator;
- unselected row menampilkan unchecked indicator;
- header action berubah menjadi Select All / Deselect All;
- Android Back ketika selection mode aktif membersihkan selection terlebih dahulu dan tetap berada di Folder Detail;
- fixed selection action bar muncul di bagian bawah Folder Detail;
- selection action bar menampilkan jumlah media terpilih;
- Add membuka shared Add to Playlist flow;
- Clear keluar dari selection mode;
- Audio Mini Player bergeser ke atas ketika selection action bar aktif agar tidak overlap;
- multiple selected media dapat ditambahkan sekaligus ke existing playlist;
- multiple selected media dapat menjadi initial items ketika membuat playlist baru;
- selection dibersihkan setelah Add to Playlist flow selesai;
- selection state tetap presentation-local dan tidak dipindahkan ke Queue Engine atau PlaylistRepository.

Selection mode tidak mengubah canonical playback queue sampai user benar-benar memilih playback action.

---
# 13. Subsystem 09 — PlayHub

## Fungsi

PlayHub adalah presentation layer untuk active playback session dan user-owned playlist collection.

PHASE 15 menetapkan Queue sebagai playback-session surface.

PHASE 16 mengaktifkan persistent Playlists melalui Room dan memperhalus interaction contract Queue card.

Persistent playback History dan Recently Played tetap deferred dan tidak lagi menjadi scope PHASE 17 setelah product direction berubah menjadi UI/UX Refinement.

## Current Native PlayHub Contract

PlayHub menggunakan:

- canonical QueueState sebagai authoritative active queue source;
- existing PlaybackState untuk transport dan progress;
- Room-backed PlaylistRepository untuk persistent playlist summaries.

PlayHub tidak membuat:

- Queue Engine kedua;
- Playback Engine kedua;
- MediaSession kedua;
- ExoPlayer kedua;
- MediaStore scanner kedua;
- MediaLibraryRepository kedua.

Current hierarchy:

```
PlayHub
├── Queue
└── Playlists
```

Layout contract:

- Queue card tetap fixed;
- Playlists heading tetap fixed;
- hanya playlist rows area yang scrollable;
- Queue empty state dan filled state menggunakan media-preview geometry yang konsisten;
- Playlists tidak memiliki standalone New action pada heading;
- playlist dibuat melalui Add to Playlist flow dari Folder Detail selection atau Audio Player.

### Queue Card Interaction

Queue card memiliki interaction hierarchy terpisah:

```
Tap Queue card background / Queue header
→ Queue Screen

Tap current media information area
→ current player presentation

Tap Previous / Play-Pause / Next
→ transport only
```

Current media information area mencakup:

- media preview;
- title;
- artist;
- album/source;
- progress;
- current position;
- duration.

Clickable media area tidak menggunakan rounded clipping yang memotong content pada sisi kanan.

Opening current player dari Queue card:

- tidak memanggil setQueue();
- tidak memanggil playAt();
- tidak rebuild queue;
- tidak restart playback;
- hanya mengubah presentation layer.

Presentation resolution:

```
Non-PlaylistContext
AudioItem → Audio Player
VideoItem → Video Player

PlaylistContext
AudioItem → Audio Player
VideoItem → Audio Player
```

### Queue Screen

- Queue Screen membaca canonical QueueState;
- current item mengikuti canonical currentIndex;
- current title menggunakan one-line running marquee;
- non-current title menggunakan one-line ellipsis;
- item tap menggunakan playAt(index);
- tidak membuat hidden queue;
- PlaylistContext tetap mempertahankan audio-first presentation policy.

### Playlists Section

- playlist summaries berasal dari Room-backed PlaylistRepository;
- row menampilkan playlist name dan item count;
- row tap membuka Playlist Detail;
- playlist creation tidak dilakukan dari PlayHub heading;
- Recently Played tidak ditampilkan pada PHASE 16.

## Acceptance Criteria

```
[x] Queue card membaca canonical QueueState
[x] Queue card transport membaca existing PlaybackState
[x] Queue card background/header membuka Queue Screen
[x] Current media area membuka current player
[x] Transport controls tidak membuka player
[x] Opening current player tidak rebuild queue
[x] Queue empty/filled geometry konsisten
[x] Playlist summaries berasal dari persistent repository
[x] Playlist row membuka Playlist Detail
[x] Tidak ada standalone New Playlist action di PlayHub heading
[x] Recently Played tetap deferred di luar current PHASE 17 UI/UX Refinement
```
# 14. Subsystem 10 — Search

Status implementasi native: PHASE 14 selesai dan terverifikasi pada 2026-09-04.

## Fungsi

Search mencari media berdasarkan canonical library.

Field minimum:

```text
title
filename
artist
album
folder
```

Hasil:

```text
files only
```

## Prinsip

Search harus memisahkan:

```text
Search Query
Search Result
Playback Context
```

Tap result tidak otomatis berarti single-item queue.

NexPlay dapat mempertahankan original context sesuai kontrak behavior.

Native PHASE 14 implementation:

- Search membaca canonical `MediaLibraryRepository`;
- filtering berjalan live dan case-insensitive di memory;
- field native yang dicari adalah `title`, `displayName` sebagai filename, `artist`, `album`, dan folder name;
- result list hanya presentation dan bukan playback queue;
- tap result me-resolve original folder dan menggunakan existing `QueuePlaybackCoordinator`;
- `AudioItem` membuka Audio Player;
- `VideoItem` membuka Video Player;
- current native Folder/Search source queue tetap type-specific per media type agar PHASE 14 tidak diam-diam memperluas scope ke mixed-media presentation handoff.

## Acceptance Criteria

- live search;
- case-insensitive;
- title/filename/artist/album/folder didukung;
- hasil hanya media file;
- tap membuka player;
- prev/next tetap mengikuti playback context yang disepakati.

---

# 15. Subsystem 11 — Playlist

## Fungsi

Playlist adalah **persistent user-owned media collection**.

PHASE 16 menggunakan Room sebagai persistence boundary.

Playlist bersifat media-agnostic pada storage dan queue, tetapi **audio-first pada presentation**.

## Persistence Architecture

```
Room
├── PlaylistEntity
├── PlaylistItemEntity
├── PlaylistDao
├── NexPlayDatabase
└── PlaylistRepository
```

Storage contract:

- Room hanya menyimpan NexPlay-owned playlist data;
- MediaStore metadata tidak diduplikasi secara penuh ke Room;
- playlist memiliki stable String ID;
- playlist item menyimpan stable item ID;
- media membership disimpan menggunakan canonical media URI;
- playlist item memiliki persistent position;
- playlist deletion melakukan cascade terhadap playlist items;
- duplicate media URI pada playlist yang sama diabaikan;
- playlist ordering dipertahankan secara persistent;
- mutation playlist memperbarui updated timestamp.

Canonical media metadata tetap berasal dari MediaLibraryRepository.

Resolution flow:

```
PlaylistItem mediaUri
        ↓
MediaLibraryRepository.getByUri()
        ↓
Canonical MediaItem?
```

Jika media sudah tidak tersedia:

- playlist membership tidak dihapus diam-diam;
- UI menampilkan unavailable state;
- unavailable item tidak dapat dimainkan;
- unavailable item dilewati ketika membentuk playback queue;
- user tetap dapat menghapus membership tersebut secara eksplisit.

## Repository Operations

Native PlaylistRepository menyediakan:

```
Create
Rename
Delete
Clear
Add
Remove
Reorder
Observe Playlists
Observe Playlist
```

Create menggunakan unique-name resolution.

Multiple selected media dapat ditambahkan dalam satu operation.

Reorder disimpan melalui Room transaction dan tidak memodifikasi active QueueState secara otomatis.

## Playlist Entry Points

Playlist creation dan Add to Playlist tersedia dari:

1. Folder Detail selection mode;
2. Audio Player Control Deck.

PlayHub heading tidak memiliki standalone New action.

### Folder Detail

```
Long press media
→ Selection Mode
→ select one or multiple media
→ Add
→ choose existing playlist
   OR create playlist
```

### Audio Player

```
Current Media
→ Add to Playlist
→ choose existing playlist
   OR create playlist
```

Shared Add to Playlist flow menerima List<MediaItem> sehingga single-item dan multi-item add menggunakan presentation path yang sama.

## Playlist Detail

Playlist Detail menyediakan:

- playlist title;
- item count;
- Play All;
- Shuffle;
- Rename;
- Clear;
- Delete;
- ordered media rows;
- per-row 3-dots actions untuk Delete dan Add to Playlist;
- dedicated drag handle;
- native drag-and-drop reorder dengan persistent final order;
- unavailable-media state.

Playlist Detail menggunakan nested presentation layer.

Back navigation menggunakan null-safe animated layer sehingga clearing selected playlist ID selama exit transition tidak memicu crash.

## Playlist → Queue Contract

Playlist playback selalu menggunakan existing canonical Queue Engine dan Playback Engine.

```
Playlist
   ↓
Resolve available canonical MediaItem
   ↓
QueuePlaybackCoordinator.setQueue()
   ↓
PlaylistContext
   ↓
Canonical QueueState
   ↓
Existing Playback Engine
```

Playlist tidak membuat:

- queue kedua;
- player kedua;
- playback engine kedua;
- MediaSession kedua.

Stored playlist order tidak mengubah active QueueState secara diam-diam.

Memainkan playlist kembali membentuk queue snapshot terbaru dari persistent playlist order.

## NexPlay Audio-First Playlist Contract

Ini merupakan deliberate product behavior NexPlay.

> Playlist di NexPlay bersifat media-agnostic tetapi audio-first.

Semua item playlist dipresentasikan melalui Audio Player secara default, termasuk canonical VideoItem.

```
Audio-only Playlist
[A1, A2, A3]
→ Audio Player

Video-only Playlist
[V1, V2, V3]
→ Audio Player
→ audio track dari video

Mixed Playlist
[A1, V1, A2, V2]
→ Audio Player continuously
```

Rule penting:

- VideoItem tetap VideoItem;
- VideoItem tidak dikonversi menjadi AudioItem;
- queue tetap dapat berisi mixed canonical MediaItem;
- QueueState tetap mempertahankan PlaylistContext;
- presentation Audio Player tidak rebuild queue;
- video track dimainkan sebagai audio-only selama playlist playback;
- next/previous pada mixed playlist tidak menyebabkan automatic Audio Player ↔ Video Player switching.

User tetap dapat memilih explicit Switch to Video ketika current playlist item merupakan VideoItem.

```
Playlist VideoItem
→ Audio Player / Audio Only
→ explicit Switch to Video
→ Video Player
```

Explicit Switch to Video:

- tidak mengganti current media identity;
- tidak mengganti PlaylistContext;
- tidak rebuild queue;
- tidak membuat playback runtime baru.

Ketika playlist berpindah ke item lain setelah explicit Video presentation, presentation kembali ke Audio Player.

Policy ini khusus PlaylistContext.

Direct playback tetap mengikuti media type:

```
Folder/Search AudioItem
→ Audio Player

Folder/Search VideoItem
→ Video Player
```

## Acceptance Criteria

```
[x] Room-backed playlist persistence tersedia
[x] Create
[x] Rename
[x] Delete
[x] Clear
[x] Add
[x] Remove
[x] Reorder
[x] Multi-item Add from Folder Detail
[x] Single-item Add from Audio Player
[x] Playlist summaries tampil di PlayHub
[x] Playlist Detail tersedia
[x] Playlist → canonical Queue bekerja
[x] Playlist playback membentuk PlaylistContext
[x] Video-only playlist dipresentasikan sebagai audio
[x] Mixed audio/video playlist tetap pada Audio Player
[x] VideoItem mempertahankan canonical media identity
[x] Explicit Switch to Video tetap tersedia
[x] Missing media tidak membuat playlist playback crash
[x] Playlist Back tidak crash selama animated exit
```

## Known Verification Limitation

Targeted Room instrumentation test telah disiapkan.

Latest execution pada physical Android device tidak menjalankan test karena installation ditolak oleh device:

```
INSTALL_FAILED_USER_RESTRICTED
Starting 0 tests
Finished 0 tests
```

Ini adalah device/test-APK installation restriction dan bukan bukti kegagalan PlaylistRepository logic.

Dedicated Room instrumentation tetap perlu dijalankan ulang ketika device mengizinkan test APK installation.
# 16. Subsystem 12 — Android Media Integration

## Fungsi

Subsystem ini menghubungkan NexPlay dengan Android OS.

Mencakup:

```text
MediaSession
MediaSessionService
Audio Focus
Notification
Lockscreen controls
Bluetooth controls
Headset buttons
Background playback
Lifecycle
Noisy-audio handling
```

## Alur

```text
Bluetooth / Notification / Lockscreen
                ↓
           MediaSession
                ↓
    QueuePlaybackCoordinator
                ↓
        Playback Engine
                ↓
          Same ExoPlayer
```

## Acceptance Criteria

- audio tetap play saat app background;
- notification controls bekerja;
- lockscreen controls bekerja;
- Bluetooth controls bekerja;
- audio focus benar;
- unplug headset dapat ditangani;
- service lifecycle stabil.

## Current Native Android Integration Contract (PHASE 12)

PHASE 12 menetapkan Android MediaSession dan background-audio integration tanpa membuat queue atau playback runtime kedua.

Current native contract:

- `NexPlayApplication` menjadi process-level owner satu `QueuePlaybackCoordinator`;
- Compose tidak lagi membuat atau me-release playback coordinator sendiri;
- `QueuePlaybackCoordinator` tetap menjadi explicit integration boundary;
- `QueueEngine` tetap menjadi satu-satunya owner canonical queue state dan queue policy;
- `PlaybackEngine` tetap menjadi owner satu ExoPlayer;
- `NexPlayPlaybackService` menggunakan `MediaSessionService`;
- service menggunakan coordinator yang sama dari application runtime;
- MediaSession tidak membuat `QueueEngine`, `PlaybackEngine`, atau ExoPlayer kedua;
- `NexPlaySessionPlayer` menjadi non-authoritative OS adapter untuk MediaSession;
- session playlist diturunkan dari canonical `QueueState`;
- OS transport commands diteruskan kembali ke `QueuePlaybackCoordinator`;
- play/pause, previous, next, seek, repeat, dan shuffle tetap mengikuti canonical playback/queue ownership;
- MediaSession tidak memiliki hidden queue atau duplicate authoritative playback state;
- media notification tersedia melalui MediaSessionService;
- notification menyediakan Previous / Play-Pause / Next;
- lockscreen menyediakan Previous / Play-Pause / Next dan seek;
- title, artist, duration, dan playback progress tersedia untuk Android system surfaces;
- foreground media playback service menggunakan Android media-playback service contract;
- ExoPlayer menangani audio focus melalui Media3 audio attributes;
- ExoPlayer menangani audio-becoming-noisy untuk safe pause ketika output audio terputus;
- headset dan Bluetooth transport controls dirutekan melalui MediaSession;
- background playback tetap berjalan ketika Activity tidak berada di foreground;
- playback tetap berjalan ketika layar dimatikan;
- playback yang sedang aktif tetap berjalan ketika NexPlay dihapus dari Recents sesuai media-service lifecycle;
- membuka NexPlay kembali menggunakan runtime queue/playback yang sama, bukan membuat playback baru.

Verified PHASE 12 flow:

```text
NexPlay UI
        │
        ├─────────────────────────────┐
        │                             │
        ↓                             ↓
QueuePlaybackCoordinator ← MediaSessionService
        │
        ├── QueueEngine
        │
        └── PlaybackEngine
                │
                ↓
           Same ExoPlayer
```

PHASE 12 selesai dan terverifikasi pada physical Android device.

Native Video Player UI selesai dan terverifikasi pada PHASE 13.
---

# 17. Empat Layer Besar NexPlay Kotlin

Dua belas subsystem di atas dapat dipadatkan menjadi empat layer:

```text
┌─────────────────────────────────────────────┐
│                PRESENTATION                 │
│                                             │
│ UI / Folders / PlayHub / Search / Players   │
└──────────────────────┬──────────────────────┘
                       ↓
┌─────────────────────────────────────────────┐
│                   DOMAIN                    │
│                                             │
│ Queue / Playback Context / Search Behavior  │
└──────────────────────┬──────────────────────┘
                       ↓
┌─────────────────────────────────────────────┐
│                    DATA                     │
│                                             │
│ Media Library / Playlist / History / Prefs  │
└──────────────────────┬──────────────────────┘
                       ↓
┌─────────────────────────────────────────────┐
│              ANDROID PLATFORM               │
│                                             │
│ MediaStore / Media3 / MediaSession / Room   │
└─────────────────────────────────────────────┘
```

---

# 18. Enam Mesin Utama

Secara engineering, NexPlay dapat dipahami sebagai enam mesin utama:

```text
ENGINE 01 — Media Discovery
ENGINE 02 — Media Library
ENGINE 03 — Queue
ENGINE 04 — Playback
ENGINE 05 — Persistence
ENGINE 06 — Android Integration
```

Sedangkan fitur seperti:

```text
Folders
PlayHub
Search
Audio Player
Video Player
Playlist
```

adalah feature layer yang menggunakan mesin tersebut.

---

# 19. Rules Arsitektur NexPlay Kotlin

Rules berikut menjadi guardrail utama:

```text
RULE 01
UI tidak boleh membaca MediaStore langsung.

RULE 02
MediaScanner tidak boleh mengenal UI.

RULE 03
Playback Engine tidak menentukan queue.

RULE 04
Queue tidak melakukan playback secara langsung.

RULE 05
Playlist bukan Queue.

RULE 06
Audio Player dan Video Player bukan pemilik ExoPlayer.

RULE 07
ExoPlayer memiliki lifecycle yang jelas dan terpusat.

RULE 08
Folders, Search, dan PlayHub menggunakan canonical Media Library.

RULE 09
MediaSession mengontrol Playback Engine, bukan screen.

RULE 10
Android-specific behavior tetap berada di platform/playback layer.

RULE 11
Database hanya menyimpan data milik NexPlay, bukan menduplikasi MediaStore.

RULE 12
Setiap subsystem harus memiliki acceptance criteria sebelum dianggap selesai.
```

---

# 20. Struktur Package Awal yang Direkomendasikan

```text
com.nexplay.media.app
│
├── app/
│   ├── NexPlayApp.kt
│   └── MainActivity.kt
│
├── navigation/
│
├── core/
│   ├── model/
│   ├── ui/
│   └── util/
│
├── media/
│   ├── scanner/
│   ├── library/
│   └── metadata/
│
├── playback/
│   ├── engine/
│   ├── queue/
│   ├── service/
│   └── session/
│
├── playlist/
│   ├── data/
│   ├── database/
│   └── repository/
│
├── history/
│
├── feature/
│   ├── home/
│   ├── folders/
│   ├── playhub/
│   ├── search/
│   ├── audioplayer/
│   ├── videoplayer/
│   └── playlists/
│
└── settings/
```

Tidak perlu membuat multi-module Gradle di awal.  
Package separation cukup sampai codebase benar-benar membutuhkan modularisasi lebih jauh.

---

# 21. Hybrid Development: Manual + Agent

Migrasi harus aman dikerjakan bergantian oleh manusia dan agent.

## Source of Truth

Dokumen minimum:

```text
MIGRATION_PLAN.md
MIGRATION_ROADMAP.md
MIGRATION_STATUS.md
CHANGELOG.md
```

Opsional:

```text
docs/ARCHITECTURE.md
docs/DECISIONS.md
prompt/tasks/
prompt/output/
```

## Aturan Hybrid

Setiap task wajib:

1. memiliki scope terbatas;
2. menyebut subsystem yang disentuh;
3. memiliki acceptance criteria;
4. mencatat file yang diubah;
5. tidak mengubah subsystem lain tanpa alasan;
6. menjalankan verification;
7. menghasilkan commit yang jelas;
8. memperbarui `MIGRATION_STATUS.md`.

## Agent Tidak Boleh

- melakukan rewrite massal tanpa task;
- mengganti arsitektur diam-diam;
- menghapus Flutter baseline;
- menambah dependency besar tanpa alasan;
- mengubah contract subsystem lain tanpa update dokumentasi.

## Developer Manual Tidak Boleh

- membuat perubahan lokal besar tanpa commit checkpoint;
- melewati acceptance criteria;
- membuat hotfix arsitektural tanpa mencatat rationale;
- mengubah behavior final Flutter tanpa keputusan eksplisit.

---

# 22. Definisi Done per Subsystem

Sebuah subsystem dianggap selesai hanya jika:

```text
[ ] Functional behavior selesai
[ ] Error handling tersedia
[ ] State management jelas
[ ] Tidak melanggar architecture rules
[ ] Verification dijalankan
[ ] Manual test dilakukan bila relevan
[ ] Acceptance criteria terpenuhi
[ ] Dokumentasi/status diperbarui
[ ] Commit checkpoint dibuat
```

---

# 23. Feature Parity Gate

Sebelum Flutter dipensiunkan, Kotlin minimal harus mencapai:

## Media Discovery

```text
[ ] Audio
[ ] Video
[ ] Folder
[ ] Metadata dasar
```

## Audio

```text
[ ] Play
[ ] Pause
[ ] Seek
[ ] Previous
[ ] Next
[ ] Queue
[ ] Shuffle
[ ] Repeat
[ ] Background playback
[ ] Notification
[ ] Bluetooth / headset control
```

## Video

```text
[ ] Playback
[ ] Seek
[ ] Fullscreen
[ ] Orientation
[ ] Error handling
[ ] Codec/device compatibility baseline
```

## Library

```text
[ ] Folders
[ ] PlayHub
[ ] Search
[ ] Playlist
```

## UX

```text
[ ] Navigation
[ ] Back behavior
[ ] Bottom bar behavior
[ ] Player transitions
[ ] Empty/loading/error states
```

## Stability

```text
[ ] New Android device
[ ] Mid-range Android device
[ ] Old Android device
[ ] No critical regression
```

---

## PHASE 23 Production Migration Gate Closure

PHASE 23 Production Migration Gate ditutup pada 2026-09-07 setelah final pre-cutover product hardening, static verification gate, dan targeted physical-device runtime acceptance.

Current migration-critical contract setelah PHASE 23:

- Android 12+ splash menggunakan launcher foreground yang aman terhadap system splash masking/scaling; pre-Android-12 branded launch behavior tetap dipertahankan;
- Add to Playlist menggunakan shared Material 3 bottom sheet, bukan centered dialog;
- Child Lock hanya ditawarkan pada fullscreen landscape Video Player;
- Child Lock menggunakan persistent local 3x3 ordered unlock pattern dengan minimum 4 unique nodes;
- legacy Child Lock PIN tidak lagi menjadi active unlock contract;
- successful long-hold unlock memberikan haptic feedback sebelum pattern verification;
- Android startLockTask / Screen Pinning tetap digunakan sebagai best-effort OS protection untuk aplikasi biasa;
- Audio dan Video 3-dots menyediakan Share dan Details menggunakan canonical MediaItem;
- Full Audio Player membuka Queue dengan swipe left;
- Queue yang dibuka dari Full Audio Player kembali ke Audio Player dan mempertahankan playback/queue state;
- Queue → Audio Player menggunakan contextual horizontal return motion;
- Audio Mini Player mendukung tap dan swipe up untuk membuka Full Audio Player;
- purple primary/secondary surfaces menggunakan white foreground melalui theme onPrimary/onSecondary contract;
- About NexPlay menggunakan real NexPlay logo;
- About tersedia konsisten dari Folders dan PlayHub dengan icon treatment yang sama;
- Folders theme chooser menggunakan horizontal icon-only System / Dark / Light selector;
- Folder Detail media sorting menggunakan Title, Album, Track, Duration, Date Modified;
- default Folder Detail media sort adalah Title ascending;
- right-side NexPlay scrollbar tersedia untuk relevant overflowing vertical lists;
- scrollbar memiliki visual track/gutter, mendukung drag fast-scroll, muncul ketika user scroll/drag, lalu fade out ketika idle;
- Full Audio Player menggunakan dedicated high-quality audio artwork path;
- high-quality Audio Player artwork memprioritaskan embedded artwork asli dan menggunakan bounded target hingga 1024 px;
- thumbnail/list/Mini Player tetap menggunakan lightweight normal media-visual path;
- shared MediaVisualLoader tetap bounded dan tidak memindahkan artwork extraction ke MediaStore scan;
- QueueEngine tetap authoritative untuk queue;
- PlaybackEngine tetap authoritative untuk single ExoPlayer/runtime playback;
- tidak ada dependency baru, playback engine baru, queue baru, persistence schema baru, atau state-management replacement pada PHASE 23.

PHASE 23 approval berarti current Kotlin candidate boleh masuk PHASE 24 Production Cutover. Approval ini bukan klaim bahwa signed production release, upgrade/install migration, atau public release sudah selesai; pekerjaan tersebut tetap menjadi scope PHASE 24.

# 24. Post-Migration Expansion

Setelah Kotlin mencapai parity dan production migration selesai, NexPlay dapat masuk ke fase enrichment.

Fitur prioritas:

```text
01. Tempo Detection
02. Lyrics Discovery
03. Metadata Discovery / Enrichment
04. Artwork Discovery / Enrichment
```

Fitur-fitur ini **tidak menjadi blocker migrasi dasar**.

Tujuannya adalah menjaga migration scope tetap terkendali.

---

# 25. Kesimpulan

NexPlay Kotlin tidak diperlakukan sebagai satu aplikasi besar yang harus di-rewrite sekaligus.

Ia dibangun sebagai kombinasi subsystem:

```text
Media Discovery
+
Media Library
+
Queue
+
Playback
+
Persistence
+
Android Integration
+
Feature UI
```

Dengan boundary yang jelas, setiap subsystem dapat:

- dibangun sendiri;
- diuji sendiri;
- diperbaiki sendiri;
- dikerjakan manual;
- didelegasikan ke agent;
- dilanjutkan tanpa kehilangan progress.

Dokumen ini menjadi **arsitektur konseptual dan batas tanggung jawab** selama seluruh proses migrasi.
