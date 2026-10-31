# Spesifikasi Desain & Perbaikan UI Aplikasi AnimeIn v1.5.0

Dokumen ini mendokumentasikan spesifikasi perbaikan total untuk Halaman Search / Cari (`CARI`), Halaman Streaming (Video Player), dan Halaman Home pada aplikasi Android AnimeIn di `/storage/emulated/0/termux/Workspace_uiApp/` berdasarkan referensi visual asli dan aturan anti-slop visual.

---

## 1. Halaman Search / Cari (`SearchScreen`)

### A. Kartu `KATEGORI` & `TAHUN` (Full Image Banner)
* Menggunakan gambar ilustrasi resmi dari API AnimeIn sebagai **Full Background Card** (`Modifier.fillMaxSize()`, `ContentScale.Crop`).
* **Dimensi Kartu:** Tinggi tetap `96.dp`, sudut membulat `12.dp`, dengan clipping bersih.
* **Tipografi & Tata Letak:**
  * **Kategori (`KATEGORI`):**
    * Label kategori/kelompok di atas: `Theme` / `Genre` / `Demographic` (ukuran `12.sp`, SF Medium, abu-abu `#6B7280`).
    * Nama kategori di bawahnya: ukuran besar `22.sp`, SF Bold, warna gelap `#222222` di sisi kiri dengan padding `start = 16.dp`.
  * **Tahun (`TAHUN`):**
    * Teks tahun ukuran besar `22.sp`, SF Bold, warna gelap `#222222` di sisi kanan dengan padding `end = 18.dp`.

### B. Kartu `STUDIO` & `TIPE`
* **Dimensi Kartu:** Tinggi `58.dp`, padding horizontal `20.dp`, sudut membulat `12.dp`.
* **Warna Latar:** Gelap navy `#18202C`.
* **Tipografi:** SF Bold ukuran `17.5.sp` warna putih bersih `#FFFFFF`.

### C. Kartu `KARAKTER` (Full Height Card + Horizontal Gradient Fade)
* **Dimensi Kartu:** Tinggi tetap `96.dp`, sudut membulat `12.dp`.
* **Warna Latar:** Gelap navy `#18202C`.
* **Nama Karakter:** Ditampilkan di sisi kiri dengan padding `start = 18.dp`, font SF Bold ukuran `21.sp` warna putih `#FFFFFF`.
* **Gambar Karakter di Sisi Kanan:**
  * Tinggi memenuhi tinggi kartu (`96.dp`), `ContentScale.Crop`.
  * **Gradient Fade Overlay:** Di atas gambar diterapkan brush horizontal gradient dari warna `#18202C` padat di sisi kiri gambar (100% solid `#18202C`) memudar ke transparan penuh (`Color.Transparent`) di sisi kanan gambar. Efek ini menghilangkan garis batas potongan gambar secara sempurna sehingga gambar menyatu alami dengan background kartu `#18202C`.

### D. Header Seksi & Tombol Navigasi `>`
* Header teks: SF Bold `14.sp`, putih `#FFFFFF`.
* Tombol panah `>` di sisi kanan: Ikon chevron putih di dalam tombol lingkaran gelap kecil (`size = 26.dp`, background `#1E2430`, `CircleShape`).

---

## 2. Halaman Streaming & Custom Video Player (`StreamingScreen`)

### A. Kontrol Video Player
* **Ikon Header Atas:**
  * Tombol Back (`<-`) di kiri atas dan tombol Gerigi Settings (`⚙`) di kanan atas ditampilkan sebagai ikon putih bersih di atas gradasi hitam halus tanpa background lingkaran/kotak glassmorphism.
* **Bar Durasi Bawah:**
  * Format waktu kiri `00:11`, total durasi kanan `23:49`, tombol Full Screen di ujung kanan.
  * Indikator/tombol kecepatan (`1.0x`) dihapus dari bar bawah.
* **Glassmorphism Selektif:**
  * Efek glassmorphism putih semi-transparan diterapkan HANYA pada tombol kontrol tengah (Play/Pause/Seek 10s) dan pada slider seekbar durasi (warna putih bersih `#FFFFFF`).

### B. Floating In-Place Settings Menu (Gaya PiuPiu)
* Menghilangkan Modal Bottom Sheet besar yang muncul dari bawah layar.
* Menggantinya dengan **Floating Popup Menu** yang mengambang tepat di pojok kanan atas player:
  * Dimensi: Kompak (lebar `200.dp`), sudut membulat `12.dp`, latar gelap semi-transparan `#141414E6` dengan border halus `0.5.dp` semi-transparan.
  * **Menu Utama:**
    * `Resolusi` / `Kualitas` (Ikon kualitas, chevron `>`)
    * `Kecepatan` (Ikon speed, teks kecepatan saat ini seperti `1.0x`, chevron `>`)
    * `Auto Next Episode` (Switch On/Off)
    * `Kunci Layar (Lock)` (Switch On/Off, aktif di mode Full Screen)
  * **In-Place Sub-View:**
    * Saat item `Kecepatan` ditekan, konten floating menu berganti di tempat yang sama menjadi daftar kecepatan vertikal: `0.25x`, `0.5x`, `0.75x`, `✓ Normal`, `1.25x`, `1.5x`, `1.75x`, `2.0x`.
    * Saat item `Resolusi` ditekan, konten floating menu berganti di tempat yang sama menjadi daftar server/resolusi vertikal: `360p`, `480p`, `720p`, `1080p` dengan tanda centang `✓` pada resolusi yang aktif.
    * Posisi durasi video wajib tetap sinkron tanpa ter-reset saat pengguna mengganti resolusi/server.

### C. Info Episode, Judul Anime & Sinopsis
* Mengambil data asli dari detail anime terkait:
  * Baris 1: `Episode [Nomor]` (ukuran `15.sp`, SF SemiBold, abu-abu `#9CA3AF`).
  * Baris 2: **Judul Asli Anime** (ukuran `22.sp`, SF Bold, putih bersih `#FFFFFF`).
  * Baris 3: **Sinopsis Asli Anime** (ukuran `12.5.sp`, maksimal 3 baris dengan `TextOverflow.Ellipsis`, warna abu-abu `#9CA3AF`).

### D. Seksi `COVER & POSTER` Selang-Seling (Interleaved)
* Menggabungkan daftar Poster dan Cover secara selang-seling:
  * Item 1: **Poster** (rasio `2:3`)
  * Item 2: **Cover** (rasio `16:9`)
  * Item 3: **Poster** (rasio `2:3`)
  * Item 4: **Cover** (rasio `16:9`)
* Ditampilkan di dalam `LazyRow` dengan tinggi tetap yang sama persis (`height = 195.dp`) sehingga sisi atas dan bawah sejajar rata sempurna.
* Gambar bersih tanpa badge angka upvote/poin. Mengetuk gambar membuka preview layar penuh.

### E. Aksi Komentar: Like, Dislike & Balasan
* Menggunakan baris `Row(modifier = Modifier.fillMaxWidth())` di bawah setiap teks komentar:
  * **Kiri:** Ikon jempol Like + angka (klik toggle like terproteksi login).
  * **Tengah:** Ikon jempol Dislike + angka (klik toggle dislike terproteksi login).
  * **Kanan:** Tombol Balasan berupa **ikon pesan balasan oranye (`#FF5722`)** di sebelah kiri teks `[Jumlah] Balasan` (warna abu-abu `#9CA3AF`) tanpa background pill dan tanpa kotak border.

---

## 3. Halaman Home (`HomeScreen`)

### A. Infinite Scroll / Pagination Horizontal
* Pada setiap `LazyRow` horizontal di Home (`Sedang Hangat`, `Jat For Yu`, `Episode Baru`, `Judul Baru`, `Paling Populer`, `Paling Dinanti`):
  * Memantau posisi scroll `LazyListState`. Ketika pengguna menggeser mendekati 3 item terakhir di sisi kanan, panggil halaman berikutnya (`page + 1`) dari API AnimeIn secara otomatis dan gabungkan datanya secara mulus tanpa interupsi.

### B. Navigasi Tombol Panah `>` ke Halaman Daftar Lengkap
* Seluruh header section Home yang memiliki tombol `>` (`Sedang Hangat >`, `Episode Baru >`, `Judul Baru >`, `Jat For Yu >`, `Paling Dinanti >`, `Paling Populer >`) dapat diklik.
* Saat diklik, membuka tampilan **Daftar Lengkap Sub-Halaman**:
  * Top bar dengan tombol kembali (`<-`) dan judul seksi.
  * Daftar vertikal `LazyColumn` dengan tata letak kartu yang identik dengan daftar anime di Halaman Jadwal:
    * Poster vertikal `2:3` di kiri (`width = 115.dp`, rounded `10.dp`).
    * Metadata di kanan: Genre oranye, Judul SF Bold putih, Sinopsis 2 baris, Views merah, dan Favorites kuning.
  * Mendukung infinite scroll vertikal ke bawah (load more page berikutnya).

### C. Lanjut Nonton Nyata (Watch History)
* Menghapus seluruh data dummy pada `Lanjut Nonton`.
* Terintegrasi dengan database lokal:
  * Menyimpan entri riwayat saat video diputar di Streaming Screen (ID anime, judul anime, episode index, episode ID, poster/thumbnail URL, posisi playback Ms, durasi total Ms, dan timestamp update).
  * **Penyembunyian Otomatis:** Jika riwayat tontonan kosong (`historyList.isEmpty()`), seksi `Lanjut Nonton` disembunyikan sepenuhnya dari layar Home.
  * Jika riwayat ada, tampilkan kartu anime yang belum selesai ditonton dengan progress bar tipis di bawah poster.
  * Saat kartu `Lanjut Nonton` diklik, langsung membuka `StreamingScreen` pada episode dan detik terakhir yang ditonton.
