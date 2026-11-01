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

## 2. SPESIFIKASI FITUR 1: In-App Update Notification Popup (iOS Blurred Style + Full GitHub README & Image Renderer)

Mengacu pada struktur referensi kartu `/storage/emulated/0/termux/design/reff/6a2395898afdd4a30a15119b92cca46c.jpg` dipadukan dengan gaya visual iOS Dark Glassmorphism.

### A. Deteksi Versi Semver Otomatis (`UpdateChecker`)
1. Mengambil data rilis terbaru dari GitHub API repositori `dinzid04/DinzStream-App`:
   - Endpoint: `https://api.github.com/repos/dinzid04/DinzStream-App/releases/latest`
   - Ambil `tag_name`, `name`, `body`, dan URL file `.apk` pada `assets`.
2. Bandingkan versi yang terpasang (`BuildConfig.VERSION_NAME`, misal `1.9.0`) dengan versi di GitHub (`tag_name`, misal `v1.9.0-build-17` atau `v1.9.1` atau `v2.0.0`):
   - Parsing semver (Major.Minor.Patch serta build counter).
   - Jika versi GitHub lebih tinggi atau terdapat rilis baru, otomatis munculkan **Update Notification Dialog**.

### B. Desain Popup Bergaya iOS Frosted Glassmorphism
1. **Dialog Overlay & Card:**
   - Card container berbentuk membulat elegan (`RoundedCornerShape(22.dp)`).
   - Latar belakang dark frosted glass (`#CC1C1C1E` / `#D9242426`) dengan border putih tipis `0.5.dp` (`#26FFFFFF`).
   - Lebar dialog proporsional (maksimal `360.dp` - `380.dp`) di tengah layar dengan background scrim semi-transparan buram.
2. **Struktur Konten:**
   - **Header Atas:** Ikon ilustrasi roket / update badge modern, teks judul **"Pembaruan Tersedia"** atau **"Update Notice"** (SF Bold Putih `18.sp`), teks versi rilis baru (pill oranye/biru), serta tombol silang (close) di pojok kanan atas.
   - **Area Konten Tengah (Scrollable):** Dapat di-scroll vertikal halus (`rememberScrollState`), menampilkan isi catatan rilis atau `README.md` repositori yang di-render secara rapi.
   - **Pembatas:** Garis horizontal tipis `0.5.dp` (`#26FFFFFF`) di atas tombol aksi.
   - **Tombol Aksi Bawah:** Tombol penuh bergaya iOS dengan teks **"Update Sekarang"** (SF Bold Putih), berlatar Crimson Red / iOS Blue (`#0A84FF`), yang membuka browser atau intent unduh file `.apk` terbaru secara langsung.

### C. Custom Markdown & README Renderer (Wajib Render Gambar, Simbol & Link Biru)
1. **Render Gambar Otomatis:**
   - Sintaks Markdown `![alt](url)` atau `<img src="url">` diproses dan di-render menjadi komponen `AsyncImage` Coil asli:
   - Sudut gambar membulat `10.dp`, scale fit/crop rapi, dengan fallback placeholder `R.drawable.ic_yuranime_monochrome`.
2. **Konversi Simbol Markdown Menjadi Visual Rapi:**
   - `##` / `###` (Heading): Teks judul tebal (**SF Bold Putih** `15.sp` - `17.sp`) dengan garis aksen tipis di bawahnya.
   - `*` atau `-` (Bullet list): Baris item rapi dengan titik bulat kecil (`CircleShape` aksen merah/biru) di sebelah kiri dan teks rapi di kanan.
   - `>` (Blockquote): Kotak kutipan dengan padding dalam, latar gelap semi-transparan, dan garis vertikal tebal di sisi kiri (aksen oranye `#F97316` atau biru `#0A84FF`) tanpa menampilkan karakter `>`.
   - `**teks**`: Dirender tebal (**SF Bold Putih**).
   - `---` / `***`: Dirender sebagai `HorizontalDivider(thickness = 0.5.dp, color = Color(0x26FFFFFF))`.
3. **Teks URL Berwarna Biru & Bisa Diklik:**
   - Semua format URL (`https://...` atau `[teks](url)`) dirender dengan warna Biru iOS (`#0A84FF`), bergaris bawah halus, dan dapat diklik untuk membuka tautan via `Intent(Intent.ACTION_VIEW, Uri.parse(url))`.

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

## 5. SPESIFIKASI FITUR 4: Verifikasi Build, Release & GitHub Actions

1. Kode ter-compile bersih tanpa error melalui Gradle.
2. Commit dan push dilakukan dari direktori `/storage/emulated/0/termux/Workspace_uiApp/` ke repositori `dinzid04/DinzStream-App`.
3. Pantau workflow GitHub Actions hingga berstatus **Success (Hijau)** dan rilis `.apk` terbaru siap diunduh.
