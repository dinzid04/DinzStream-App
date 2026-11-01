# Spesifikasi Desain & Panduan Eksekusi Yuranime (Single Source of Truth)

Dokumen ini adalah acuan spesifikasi desain dan teknis aplikasi **Yuranime** di `/storage/emulated/0/termux/Workspace_uiApp/` berdasarkan inspeksi aset terbaru di `/storage/emulated/0/termux/design/reff/` serta aturan kualitas dari `.agents/skills/anti-slop/SKILL.md`.

---

## Sistem Desain & Parameter Liveliness (Anti-Slop Direction)

- **Design Read:** Aplikasi streaming anime mobile native Android untuk penonton anime Indonesia, bergaya dark sinematik dengan hierarki permukaan kontras tinggi, dial `ENERGY 2 / RHYTHM 2 / MOTION 2`.
- **Palet Inti (R-01, R-29):**
  - Background Utama: `#0B0B0B` / `#000000` (Splash & Video Player)
  - Surface Kontainer Info Streaming: `#12151C` (Dark Slate Surface untuk memisahkan blok metadata utama dari background halaman)
  - Surface Placeholder Gambar: `#121622`
  - Aksen Brand & Interaksi: `#E23441` / `#D8353E` (Crimson Red pada logo Yuranime), `#FF5722` (Deep Orange untuk aksen navigasi/badge aktif), dan `#3B82F6` (Action Link Blue untuk aksi ekspansi sinopsis `Selengkapnya >`).
- **Tipografi (R-06):**
  - UI Umum: **SF Pro Display** (`Regular`, `Medium`, `SemiBold`, `Bold`) untuk keterbacaan metadata dan kepadatan informasi katalog.
  - Brand Wordmark & Splash Screen: **Calistoga Regular** (`Yuranime-logo-font.ttf` / `yuranime_logo_font.ttf`) khusus untuk teks identitas logo `Yuranime`.

---

## PRIORITAS 1: Pembaruan Halaman Streaming (Card Info Anime di Atas `COVER & POSTER`)
*Mengacu pada gambar referensi: `/storage/emulated/0/termux/design/reff/Proyek Baru 154 [AC47779].png`*

1. **Kontainer Info Anime (`#12151C` / Dark Surface Card):**
   - Kontainer kartu dengan warna latar `#12151C`, sudut membulat `12.dp`, serta padding proporsional (`horizontal = 14.dp, vertical = 14.dp`) yang ditempatkan tepat di bawah Video Player (`16:9`) dan tepat di atas bagian `COVER & POSTER`.
2. **Baris Atas (Poster di Kiri + Judul & Metadata di Kanan):**
   - **Sisi Kiri:** Gambar **Poster Anime** vertikal (`aspectRatio(2f / 3f)`, lebar `80.dp`, sudut membulat `8.dp`, dengan placeholder `ic_yuranime_monochrome`).
   - **Sisi Kanan Poster (tersusun vertikal rata atas):**
     - **Judul Anime:** Judul anime lengkap menggunakan font **SF Bold** warna putih (`#FFFFFF`), ukuran `19.sp`, `lineHeight = 24.sp`, maksimal 2 baris (`maxLines = 2`, `overflow = TextOverflow.Ellipsis`).
     - **Baris Episode, Views & Aired Start:** Tepat di bawah judul, ditampilkan dalam 1 baris (`Row` dengan `Alignment.CenterVertically`):
       - Teks `Episode [Nomor]` (warna abu-abu terang `#CBD5E1`, **SF Medium** `13.sp`).
       - Spasi + **Ikon Mata** (`14.dp`, warna `#CBD5E1`) + jumlah views singkat (contoh: `3.1K`, menggunakan formatter angka ringkas dari data asli API).
       - **Kondisional Aired Start:** Jika data `airedStart` / tanggal rilis tersedia dan tidak kosong atau bukan tanda strip (`"-"`), tampilkan pemisah ` • ` diikuti tanggal rilisnya (contoh: `24 Okt 2026`). Jika `airedStart` kosong, null, atau `"-"`, jangan tampilkan titik pemisah maupun tanggalnya.
3. **Baris Bawah (Sinopsis + Tombol `Selengkapnya >`):**
   - Tepat di bawah baris poster dan di dalam kontainer `#12151C` yang sama, tampilkan teks **Sinopsis Anime** berwarna `#94A3B8`, ukuran `13.sp`, `lineHeight = 19.sp`.
   - Secara default dibatasi 3 baris (`maxLines = 3`, `overflow = TextOverflow.Ellipsis`).
   - Tepat di bawah sinopsis, tampilkan tombol interaktif **`Selengkapnya >`** (dan **`Sembunyikan`** saat terbuka) berwarna biru `#3B82F6` (**SF SemiBold**, `13.sp`) dengan ikon chevron yang membuka atau menutup seluruh isi sinopsis secara mulus (`animateContentSize()`).
4. **Bagian `COVER & POSTER` & `COMMENTS`:**
   - Mempertahankan bagian `COVER & POSTER` (tinggi sejajar dan selang-seling antara poster vertikal 2:3 dan cover landscape 16:9) serta daftar `COMMENTS` tepat di bawah kontainer info anime tersebut.

---

## PRIORITAS 2: Pembaruan Placeholder Gambar (`yuranime-monochrome.svg`) & App Icon (`yuranime-app-icon.svg`)
*Mengacu pada aset: `/storage/emulated/0/termux/design/reff/yuranime-monochrome.svg` & `/storage/emulated/0/termux/design/reff/yuranime-app-icon.svg`*

1. **SVG Placeholder Global (`yuranime-monochrome.svg`):**
   - Mengonversi `yuranime-monochrome.svg` menjadi VectorDrawable Android (`ic_yuranime_monochrome.xml`) serta memperbarui `placeholder_yuranime.xml` (rasio 2:3) dan `placeholder_yuranime_wide.xml` (rasio 16:9).
   - Mengganti seluruh placeholder lama di semua komponen `AsyncImage` pada aplikasi (Home, Detail, Streaming, Jadwal, Search, Cuplix) agar menggunakan siluet serigala/rubah `yuranime-monochrome.svg` yang tampil presisi di tengah (`Alignment.Center`) dengan ukuran proporsional saat gambar sedang dimuat setelah *skeleton shimmer* atau ketika gagal dimuat.
2. **Launcher & Adaptive App Icon (`yuranime-app-icon.svg`):**
   - Mengonversi `yuranime-app-icon.svg` menjadi `ic_launcher_background.xml` (warna merah khas `#E23441`), `ic_launcher_foreground.xml` (siluet serigala/rubah putih dengan aksen segitiga Play), serta memperbarui `mipmap-anydpi-v26/ic_launcher.xml` dan `mipmap-anydpi-v26/ic_launcher_round.xml` beserta raster fallback PNG `ic_launcher.png` dan `ic_launcher_round.png`.

---

## PRIORITAS 3: Motion Graphics Splash Screen (`yuranime-full-logo.svg` + Font Khusus)
*Mengacu pada aset: `/storage/emulated/0/termux/design/reff/yuranime-full-logo.svg`, `/storage/emulated/0/termux/design/reff/Yuranime-logo-font.ttf`, dan video referensi di `/storage/emulated/0/termux/design/reff/`*

1. **Ekstraksi Elemen Vector & Pemasangan Font:**
   - Menyalin file font logo `Yuranime-logo-font.ttf` (Calistoga Regular) ke `app/src/main/res/font/yuranime_logo_font.ttf` dan mendeklarasikan `YuranimeLogoFontFamily` di `Type.kt`.
   - Memisahkan komponen `yuranime-full-logo.svg` menjadi layer vector independen untuk dianimasikan pada 60fps:
     1. **Segitiga Play Putih** (inti di tengah).
     2. **Dua Busur Lingkaran Merah (Red Arcs)** (`#D8353E`) di sekeliling emblem.
     3. **Siluet Kepala Serigala/Rubah Putih** dengan ruang negatif segitiga Play di bagian dalam.
     4. **Teks `Yuranime`** di sebelah kanan emblem menggunakan `YuranimeLogoFontFamily`.
2. **Urutan Sekuens Animasi Motion Graphics (0ms - 2600ms):**
   - **Tahap A (0ms - 600ms | Kemunculan Inti):** Latar belakang hitam pekat (`#000000`). Elemen **Segitiga Play Putih** muncul di tengah layar dengan animasi *scale-up + fade-in* dan efek cincin impuls halus.
   - **Tahap B (600ms - 1400ms | Rotasi & Perakitan Logo):** Dua **Busur Lingkaran Merah** melakukan animasi *sweep / rotasi melingkar* mengelilingi pusat, sementara **Siluet Serigala/Rubah Putih** muncul menyatu (*scale & alpha reveal*) bersama transisi segitiga Play hingga membentuk ikon bulat serigala Yuranime yang utuh tepat di tengah layar.
   - **Tahap C (1400ms - 2200ms | Geser ke Kiri & Kemunculan Teks `Yuranime`):**
     - Emblem bulat serigala yang sudah utuh bergeser mulus dari tengah ke sisi kiri menggunakan `FastOutSlowInEasing`.
     - Bersamaan dengan pergeseran emblem ke kiri, teks **`Yuranime`** muncul secara mulus (*slide-in + staggered fade-in* dari kiri ke kanan) di sebelah kanan logo hingga membentuk komposisi `yuranime-full-logo.svg` yang utuh dan presisi di tengah layar.
   - **Tahap D (2200ms - 2600ms | Transisi Masuk ke Home):** Setelah komposisi logo penuh tampil sejenak, layar melakukan *crossfade + slight scale* menuju **Home Screen** yang datanya sudah di-*preload* di background sejak awal aplikasi dibuka.


---

## PRIORITAS 4: Perubahan UI Home Slider & Animasi Logo
*Mengacu pada tangkapan layar referensi: `/storage/emulated/0/termux/design/reff/Proyek Baru 157 [BD62D8D].png`*

1. **Header Bar & Animasi Logo Yuranime (`YuranimeAnimatedLogo`):**
   - Ditempatkan di bagian paling atas Home Screen (`statusBarsPadding()`) melayang anggun di atas backdrop slider.
   - Emblem bulat serigala dengan rotasi busur merah 360 derajat secara halus (`FastOutSlowInEasing`) saat pertama kali render, serta animasi mikro responsif (scale spring + rotasi 180 derajat) saat logo di-tap oleh pengguna.
   - Wordmark teks "Yuranime" menggunakan font resmi Calistoga Regular (`YuranimeLogoFontFamily`) warna putih solid.
2. **Hero Slider Sinematik (`YuranimeHeroSlider`):**
   - Latar belakang landscape cover beresolusi tinggi dengan gradien ganda (gelap di atas untuk keterbacaan logo/status bar, dan gelap pekat menyatu ke `#0B0B0B` di bawah).
   - Di sisi kiri bawah: Poster anime vertikal vertikal rasio 2:3 (`88.dp` x `132.dp`, rounded `8.dp`, border halus `#33FFFFFF`).
   - Di sisi kanan poster: Judul anime lengkap (**SF Bold**, uppercase/title case, `19.sp`, putih), sinopsis anime (`11.5.sp`, `#CBD5E1`, max 2 baris), dan tombol aksi CTA **"TONTON SEKARANG"** berwarna merah brand Yuranime (`#E23441`) lengkap dengan ikon Play putih.
   - Indikator paginasi pil aktif dan titik inaktif di sudut kanan bawah.

---

## PRIORITAS 5: Pembaruan Layar Login (Profile) Sinematik
*Mengacu pada aset video referensi `/storage/emulated/0/termux/design/reff/VID-20261101-WA0035.mp4` dan poster di `/storage/emulated/0/termux/design/reff/poster/`*

1. **Latar Belakang Poster Anime Marquee Miring (Tilted 3-Column Marquee):**
   - 3 kolom vertikal poster anime offline di drawable (`poster_login_01.jpg` s/d `poster_login_12.jpg`) dimiringkan -9 derajat dan bergerak vertikal mulus secara terus-menerus pada 60fps.
   - Lapisan vignette gradient gelap pekat di bagian atas dan bawah untuk memastikan kontras maksimal.
2. **Komposisi Identitas & Tombol Masuk:**
   - Bagian tengah: Logo bulat serigala Yuranime, teks "YURANIME" Calistoga, dan tagline "Tempat Nonton Anime Subtitle Indonesia".
   - Bagian bawah: Tombol putih bersih **"Masuk dengan Google"** dengan logo 4-warna Google resmi (`ic_google_logo.xml`), tombol **"Login Tester"**, disclaimer persetujuan layanan, dan teks versi app ("Versi 1.9.0").
3. **Tampilan Profil Akun Aktif & Avatar Bergerak (`AnimatedAvatarBorder`):**
   - Saat pengguna login, halaman profil menampilkan kartu akun dengan avatar border berputar 60fps native Compose, pemilih tema dekorasi avatar (Kitsune Flame, Neon Cyberpunk, Celestial Gold, Sakura Bloom, Void Abyss), riwayat tontonan lokal, dan opsi keluar akun.

---

## PRIORITAS 6: Perbaikan Randomisasi Cuplix
- Penggantian pengambilan Cuplix statis dengan fungsi `getRandomizedCuplixFeed` di `AnimeRepository.kt` yang menggabungkan feed populer dan terbaru serta melakukan pengacakan dinamis (shuffled).
- Menjamin klip yang diklik pengguna tetap berada di index 0 untuk pemutaran instan, sedangkan seluruh sisa feed di-randomize sehingga penonton selalu menemukan tayangan anime yang bervariasi setiap saat.
- Pengacakan juga aktif pada avatar bulat Cuplix di Home Screen dan Jadwal Screen.

---

## PRIORITAS 7: Migrasi Package Name Menjadi `com.yuranime.dinz`
- Seluruh namespace dan applicationId pada `app/build.gradle.kts` dimutakhirkan menjadi `com.yuranime.dinz`.
- Refactor struktur package direktori dari `com.dinz.animein` ke `com.yuranime.dinz` di seluruh 38 file Kotlin, AndroidManifest.xml, dan aturan ProGuard.
