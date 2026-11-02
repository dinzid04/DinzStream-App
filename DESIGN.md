# Spesifikasi Desain & Panduan Pembaruan Yuranime (Single Source of Truth)

Dokumen ini adalah acuan spesifikasi desain dan teknis aplikasi **Yuranime (`com.yuranime.dinz`)** di `/storage/emulated/0/termux/Workspace_uiApp/` berdasarkan gambar referensi terbaru pada `/storage/emulated/0/termux/design/reff/` dan kepatuhan penuh terhadap aturan `.agents/skills/anti-slop/SKILL.md`.

---

## 1. Identitas Brand, Tipografi & Parameter Desain (Anti-Slop Direction)

- **Design Read:** Platform streaming anime mobile native Android untuk penonton Indonesia bergaya dark sinematik premium dengan sentuhan iOS Dark Glassmorphism, tanpa elemen template murahan (anti-slop). Dial parameter: `ENERGY 2 / RHYTHM 2 / MOTION 2`.
- **Palet Warna Inti (R-01, R-29):**
  - Background Utama: `#0B0B0B` (Home, Splash, Video Player) & `#0E0F12` (Profile Screen)
  - Card Surface / Menu Box: `#1A1C20` / `#161B26`
  - Kontainer iOS Frosted Glassmorphism: `#CC1C1C1E` / `#D9242426` dengan border tipis `0.5.dp` `#26FFFFFF`
  - Aksen Utama: Crimson Red `#E11D48` / `#EF4444`, iOS Blue `#0A84FF` / `#3B82F6`, Orange `#F97316`
  - Teks Utama: `#FFFFFF` (Putih Bersih)
  - Teks Sekunder: `#CBD5E1` / `#94A3B8` / `#E5E5EA`
- **Tipografi Resmi (R-06):**
  - Brand Wordmark: **`Yuranime-logo-font.ttf`** (**Calistoga-Regular**)
  - UI Umum, Navigasi, Menu & Markdown: **SF Pro Display** (`Regular`, `Medium`, `SemiBold`, `Bold`)

---

## 2. SPESIFIKASI FITUR 1: Strict SemVer Update Checker & In-App Update Dialog (Anti-False-Positive)

Mengacu pada arsitektur pembaruan aplikasi native yang bersih dan tahan terhadap false-positive rilis GitHub Actions:

### A. Sinkronisasi Hulu ke Hilir (Gradle & GitHub Actions)
1. **Injeksi Versi Dinamis:**
   - `app/build.gradle.kts` mendukung properti `versionName` dan `versionCode` dari Gradle CLI (`-PversionName=...` dan `-PversionCode=...`), dengan fallback default yang valid (contoh: `versionName = "1.9.2"`).
   - `.github/workflows/android-release.yml` menyuntikkan nilai versi rilis secara konsisten saat `./gradlew assembleRelease` sehingga `BuildConfig.VERSION_NAME` dalam file `.apk` yang dihasilkan 100% identik dengan `tag_name` rilis GitHub.
2. **Pembersihan String Versi (Normalisasi):**
   - Hapus semua awalan non-angka seperti `"v"`, `"V"`, `"release-"`, `"Release-"`.
   - Normalisasikan pemisah build seperti `"-build-"` atau `"-b"` menjadi titik (`.`) atau pecah menjadi segmen angka murni (`major`, `minor`, `patch`, `build`).

### B. Algoritma Strict SemVer Comparison (`isNewerVersion(remote, local)`)
1. **Aturan Evaluasi Numerik Murni:**
   - Tidak menggunakan perbandingan string mentah (`!=`) dan tidak membandingkan timestamp `published_at`.
   - Mengonversi tiap segmen versi menjadi `Int` (contoh: `"1.9.2.19"` -> `[1, 9, 2, 19]`).
   - Melakukan perbandingan segmen demi segmen dari kiri ke kanan (`major` -> `minor` -> `patch` -> `build`). Jika salah satu segmen tidak tersedia, gunakan nilai default `0`.
   - Nilai `isNewerVersion(remote, local)` bernilai `true` **HANYA JIKA** remote secara numerik lebih besar dari local (`remote > local`).
   - Jika remote sama dengan local (`remote == local`) atau remote lebih kecil, atau request API gagal/kosong, hasil perbandingan **WAJIB** `false`.
2. **Popup Update Trigger:**
   - Dialog pembaruan otomatis **HANYA BOLEH MUNCUL** saat `info != null && info.isUpdateAvailable == true`.
   - Pada pengecekan manual di Profil, jika tidak ada versi baru, tampilkan toast informatif bahwa aplikasi sudah menggunakan versi terbaru, bukan memunculkan dialog pembaruan.

### C. Desain Popup Bergaya iOS Frosted Glassmorphism
1. **Dialog Overlay & Card:**
   - Card container berbentuk membulat elegan (`RoundedCornerShape(22.dp)`).
   - Latar belakang dark frosted glass (`#CC1C1C1E` / `#D9242426`) dengan border putih tipis `0.5.dp` (`#26FFFFFF`).
   - Lebar dialog proporsional (maksimal `360.dp` - `380.dp`) di tengah layar dengan background scrim semi-transparan buram.
2. **Struktur Konten:**
   - **Header Atas:** Ikon ilustrasi roket / update badge modern, teks judul **"Pembaruan Tersedia"** atau **"Update Notice"** (SF Bold Putih `18.sp`), teks versi rilis baru (pill oranye/biru), serta tombol silang (close) di pojok kanan atas.
   - **Area Konten Tengah (Scrollable):** Dapat di-scroll vertikal halus (`rememberScrollState`), menampilkan isi catatan rilis atau `README.md` repositori yang di-render secara rapi.
   - **Pembatas:** Garis horizontal tipis `0.5.dp` (`#26FFFFFF`) di atas tombol aksi.
   - **Tombol Aksi Bawah:** Tombol penuh bergaya iOS dengan teks **"Update Sekarang"** (SF Bold Putih), berlatar Crimson Red / iOS Blue (`#0A84FF`), yang membuka browser atau intent unduh file `.apk` terbaru secara langsung.

### D. Custom Markdown & README Renderer (Wajib Render Gambar, Simbol & Link Biru)
1. **Render Gambar Otomatis:**
   - Sintaks Markdown `![alt](url)` atau `<img src="url">` diproses dan di-render menjadi komponen `AsyncImage` Coil asli.
   - Sudut gambar membulat `10.dp`, scale fit/crop rapi, dengan fallback placeholder `ic_yuranime_monochrome`.
2. **Konversi Simbol Markdown Menjadi Visual Rapi:**
   - `##` / `###` (Heading): Teks judul tebal (**SF Bold Putih** `15.sp` - `17.sp`) dengan garis aksen tipis di bawahnya.
   - `*` atau `-` (Bullet list): Baris item rapi dengan titik bulat kecil (`CircleShape` aksen merah/biru) di sebelah kiri dan teks rapi di kanan.
   - `>` (Blockquote): Kotak kutipan dengan padding dalam, latar gelap semi-transparan, dan garis vertikal tebal di sisi kiri (aksen oranye `#F97316` atau biru `#0A84FF`) tanpa menampilkan karakter `>`.
   - `**teks**`: Dirender tebal (**SF Bold Putih**).
   - `---` / `***`: Dirender sebagai `HorizontalDivider(thickness = 0.5.dp, color = Color(0x26FFFFFF))`.
3. **Teks URL Berwarna Biru & Bisa Diklik:**
   - Semua format URL (`https://...` atau `[teks](url)`) dirender dengan warna Biru iOS (`#0A84FF`), bergaris bawah halus, dan dapat diklik untuk membuka tautan via browser.

---

## 3. SPESIFIKASI FITUR 2: Perbaikan Total Halaman Login

Mengacu pada screenshot evaluasi `/storage/emulated/0/termux/design/reff/Screenshot_20261101_220141_Yuranime.jpg`.

1. **Sembunyikan Bottom Navigation Bar:**
   - Saat pengguna belum login dan berada di layar Login (`!isLoggedIn`), **SEMBUNYIKAN sepenuhnya Bottom Navigation Bar**. Jangan ada ruang kosong maupun tab navbar yang terlihat.
2. **Ikon Panah Kembali di Pojok Kanan Atas:**
   - Di pojok kanan atas layar Login (dengan status bar padding yang aman), sediakan tombol ikon panah kembali (`Icons.AutoMirrored.Filled.ArrowForward` atau `Icons.Default.ArrowForward` / tombol `✕` / panah kembali bersih) yang saat diklik langsung mengarahkan pengguna kembali ke **Beranda Utama (`HomeScreen`)**.
3. **Perbaiki Ikon Rubah & Posisi Teks `YURANIME`:**
   - **Hapus Background Hitam:** Hilangkan background lingkaran hitam pada ikon rubah. Latar belakang ikon menjadi 100% transparan menyatu dengan poster overlay.
   - **Perbesar Ukuran Ikon:** Gunakan ukuran ikon rubah putih `84.dp` (menggunakan `ic_yuranime_monochrome` berkualitas tinggi).
   - **Geser Posisi ke Atas:** Posisikan keseluruhan grup (Ikon Rubah + Teks `YURANIME` + Subteks "Tempat Nonton Anime Subtitle Indonesia") di sekitar posisi 28% - 32% dari atas layar, menghasilkan komposisi visual yang seimbang dan tidak tenggelam ke tengah.
4. **Perbaiki Kolase Poster Bergerak:**
   - **Perlambat Kecepatan:** Kecepatan *auto-scroll* vertikal diperlambat secara signifikan agar pergerakan poster tenang, halus, dan sinematik.
   - **Rapatkan Jarak Poster:** Jarak horizontal dan vertikal antar poster dirapatkan menggunakan `Arrangement.spacedBy(8.dp)`.
   - **True Seamless Infinite Loop (Anti-Glitch):** Implementasikan infinite scroll menggunakan indeks modulo virtual berulang tanpa lompatan atau glitch saat loop berjalan.

---

## 4. SPESIFIKASI FITUR 3: Pembuatan Halaman Profile Setelah Login

Mengacu persis pada tangkapan layar desain `/storage/emulated/0/termux/design/reff/Screenshot_20261101_220757.jpg`.

1. **Header Banner, Avatar, Nama & Level:**
   - Bagian atas menampilkan **Cover/Banner Profil Landscape Anime** dengan gradasi halus menghitam ke warna background utama (`#0E0F12`) di batas bawah.
   - Sisi kiri bawah banner: **Foto Profil Bulat** berukuran `78.dp` dengan border oranye tipis (`#F97316`) dan **badge ikon kamera biru kecil** (`#3B82F6`) di pojok kanan bawah avatar.
   - Sisi kanan avatar: **Nama Pengguna** (font **SF Bold Putih** `20.sp`) diikuti **ikon pensil edit** di sebelahnya, serta teks status **`Lv. 1`** (warna abu-abu terang `#94A3B8`) tepat di bawah nama.
2. **Baris Tombol Media Sosial (Scrollable Row):**
   - Deretan kapsul (*pill buttons*) ber-border tipis dengan background `#161B22` transparan:
     - **WhatsApp** (ikon & teks hijau `#25D366`)
     - **Telegram** (ikon & teks biru `#38BDF8`)
     - **Instagram** (ikon & teks pink kemerahan `#E1306C`)
     - **TikTok** (ikon & teks putih bersih `#FFFFFF`)
     - **Discord** (ikon & teks ungu kebiruan `#5865F2`)
   - Setiap tombol dapat diklik untuk membuka tautan sosial media resmi Yuranime atau browser.
3. **Daftar Menu Profil (Hanya Menu Pilihan Sesuai Permintaan):**
   - **HAPUS / JANGAN TAMPILKAN:** `Berlangganan`, `Nonton Bareng`, `Squad`, `Giveaway`, dan `Event`.
   - **TAMPILKAN 5 MENU UTAMA + LOGOUT:**
     Desain menu menggunakan ikon dalam kotak membulat gelap (`#1A1C20`) di kiri, teks judul **SF SemiBold Putih** `15.sp` + deskripsi abu-abu di tengah, dan aksen/indikator di kanan:
     1. **`Edit Profil`** (Ikon Pensil) : Sub-teks *"Ganti Foto atau Cover"* (kanan: chevron `>`). Membuka dialog interaktif untuk mengganti nama pengguna, foto avatar, atau banner cover profil secara langsung dan tersimpan di database lokal.
     2. **`Unduh`** (Ikon Download) : Sub-teks *"Kelola konten yang diunduh"* (kanan: chevron `>`).
     3. **`Papan Peringkat`** (Ikon Trophy) : Sub-teks *"Lihat peringkat XP bulanan"* (kanan: badge pill biru **`TOP 100`**).
     4. **`Points & Rewards`** (Ikon Bintang) : Sub-teks *"Check-in harian & tukar poin"* (kanan: badge pill kuning emas **`★ 60`**).
     5. **`Info`** (Ikon Info) : Sub-teks *"Simak Cerita Kami"* (kanan: chevron `>`).
     6. **`Keluar dari Akun / Logout`** (Ikon Logout Merah) : Opsi keluar akun yang rapi di bagian bawah yang mengembalikan status sesi menjadi belum login dan berpindah ke layar Login.
4. **Bottom Navigation Bar Tetap Ditampilkan:**
   - Saat pengguna berada di `ProfileScreen` dalam keadaan sudah login, Bottom Navigation Bar tetap aktif di bagian bawah layar.

---

## 5. SPESIFIKASI FITUR 4: Streaming Episode Selector & Search Inline (Anti-Slop Design)

Komponen pemilihan episode horizontal yang diletakkan tepat di bawah kontainer Info Anime (`#12151C`) dan tepat di atas header `COVER & POSTER` pada `StreamingScreen`:

### A. Header Baris & Kolom Search Episode Ergonomis
1. **Header Kiri:**
   - Label judul bagian **`EPISODE`** (font **SF Bold** putih `14.sp`–`15.sp`, letter-spacing `0.5.sp`, konsisten dengan header `COVER & POSTER`).
   - Teks kecil jumlah total episode berwarna abu-abu netral (`#64748B`, contoh: `• 1180 Ep`).
2. **Kolom Inline Search Kanan:**
   - Pill pencarian kompak (*compact inline search pill*) dengan tinggi `32.dp`, sudut membulat `16.dp`, berlatar belakang gelap `#161922` tanpa border tebal.
   - Ikon kaca pembesar minimalis `14.dp` berwarna `#64748B`.
   - Placeholder teks `Cari ep..` berwarna `#64748B` (`12.sp`) dengan kursor aksen Crimson Red / Orange.
   - Reaktif seketika: saat pengguna mengetik angka episode (misal `1150` atau `12`), daftar episode langsung memfilter item atau melompat (*auto-scroll*) tepat ke nomor episode tersebut.

### B. Adaptive Range Selector untuk Anime Berepisode Panjang
1. **Aturan Kondisional:**
   - **Anime Panjang (> 50 episode):** Tampilkan baris tab rentang tipis dan bersih (`1 - 100`, `101 - 200`, dst. atau `Terbaru`) tepat di bawah header. Menggunakan chunking 100 episode per tab sehingga navigasi ribuan episode tetap super ringan, instan, dan bebas lag.
   - **Anime Pendek (<= 50 episode):** Sembunyikan baris rentang sepenuhnya agar antarmuka tetap ringkas dan tidak memakan ruang layar secara berlebihan.
2. **Gaya Visual Tab Rentang:**
   - Kapsul tipis tinggi `26.dp`, `RoundedCornerShape(6.dp)`.
   - Tab aktif berlatar `#242836` dengan teks SF SemiBold putih (`11.sp`).
   - Tab non-aktif berlatar transparan/`#12151C` dengan teks `#64748B` (`11.sp`).

### C. Desain Item Episode Geser Horizontal (LazyRow Anti-Slop)
1. **Dimensi & Spacing:**
   - Menggunakan `LazyRow` dengan `Arrangement.spacedBy(8.dp)` dan `PaddingValues(horizontal = 16.dp)`.
   - Dimensi tombol episode: tinggi `44.dp` (memenuhi standar tap target aksesibilitas R-03), lebar minimum `52.dp` (menyesuaikan lebar digit nomor), dengan sudut membulat `RoundedCornerShape(10.dp)`.
2. **State Visual:**
   - **Active State (Episode yang Sedang Diputar):**
     - Latar berwarna aksen **Oranye/Crimson Yuranime (`#FF5722` / `#E11D48`)**.
     - Teks nomor episode **SF Bold Putih (`14.sp`)**.
     - Indikator visual ikon bermain minimalis (`Icons.Default.PlayArrow` `14.dp`).
   - **Inactive State (Episode Lainnya):**
     - Latar permukaan gelap bersih (`#161922`, tanpa border tebal).
     - Teks nomor episode **SF SemiBold** warna abu-abu terang (`#CBD5E1`, `14.sp`).
3. **Auto-Scroll ke Episode Aktif:**
   - Saat `StreamingScreen` pertama kali dimuat atau saat berganti episode, sistem menjalankan `LaunchedEffect(currentEpisodeId)` untuk otomatis menggeser (`animateScrollToItem`) `LazyRow` ke indeks episode aktif sehingga selalu berada di tengah area pandang pengguna.
4. **Interaksi Pemutaran:**
   - Saat pengguna mengklik salah satu tombol nomor episode, player langsung memuat stream episode yang dipilih tanpa reload layar penuh, memperbarui status episode saat ini, info anime, dan daftar komentar.

---

## 6. SPESIFIKASI FITUR 5: Verifikasi Build, Release & GitHub Actions

1. Kode ter-compile bersih tanpa error melalui Gradle.
2. Commit dan push dilakukan dari direktori `/storage/emulated/0/termux/Workspace_uiApp/` ke repositori `dinzid04/DinzStream-App`.
3. Pantau workflow GitHub Actions hingga berstatus **Success (Hijau)** dan rilis `.apk` terbaru siap diunduh.
