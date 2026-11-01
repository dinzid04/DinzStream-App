# Spesifikasi Desain & Panduan Eksekusi Yuranime (Single Source of Truth)

Dokumen ini adalah acuan spesifikasi desain dan teknis aplikasi **Yuranime** di `/storage/emulated/0/termux/Workspace_uiApp/` berdasarkan inspeksi menyeluruh pada folder referensi `/storage/emulated/0/termux/design/reff/` dan kepatuhan penuh terhadap aturan `.agents/skills/anti-slop/SKILL.md`.

---

## 1. Identitas Brand, Tipografi & Parameter Desain (Anti-Slop Direction)

- **Design Read:** Platform streaming anime mobile native Android untuk penonton Indonesia bergaya dark sinematik dengan visual contrast tinggi, tanpa elemen template murahan (anti-slop). Dial parameter: `ENERGY 2 / RHYTHM 2 / MOTION 2`.
- **Palet Warna Inti (R-01, R-29):**
  - Background Utama: `#0B0B0B` (Layar Utama, Home, Splash & Video Player)
  - Card Surface: `#12151C` / `#161B26` (Midnight Slate Surface)
  - Aksen Brand: Crimson Red `#E11D48` / `#EF4444` / `#E23441` (Tombol Aksi Utama, Badge, Aksen Navigasi)
  - Teks Utama: `#FFFFFF` (Putih Bersih)
  - Teks Sekunder: `#CBD5E1` / `#94A3B8` (Abu-abu terang untuk sinopsis & metadata)
  - Tombol Google: Background `#FFFFFF` dengan teks `#111827` dan logo Google 4-warna asli.
- **Tipografi Resmi (R-06):**
  - Brand Wordmark & Judul Slider Home: **`Yuranime-logo-font.ttf`** (**Calistoga-Regular**) yang menyatu dengan identitas brand Yuranime.
  - UI Umum, Navigasi, Metadata & Sinopsis: **SF Pro Display** (`Regular`, `Medium`, `SemiBold`, `Bold`).

---

## 2. SPESIFIKASI TAHAP 1: Package Name `com.yuranime.dinz`
- `namespace` dan `applicationId` pada `app/build.gradle.kts` adalah **`com.yuranime.dinz`**.
- Seluruh file Kotlin, konfigurasi AndroidManifest, dan aturan ProGuard berada di dalam package `com.yuranime.dinz` tanpa menyisakan dependensi lama `com.dinz.animein`.
- Versi aplikasi: `versionCode = 6`, `versionName = "1.9.0"`.

---

## 3. SPESIFIKASI TAHAP 2: Hero Slider Home & Motion Graphics Logo Header
*Mengacu pada tangkapan layar `/storage/emulated/0/termux/design/reff/Proyek Baru 157 [BD62D8D].png` & logo vector di `/storage/emulated/0/termux/design/reff/`*

### A. Animasi Motion Graphics Berkala pada Logo Header (Pojok Kiri Atas)
1. **Pemisahan Layer:**
   - Layer 1: **Ikon Serigala/Rubah + Busur Merah** (`ic_splash_wolf_white`, `ic_splash_red_arc_left`, `ic_splash_red_arc_right`, `ic_splash_play_triangle`).
   - Layer 2: **Teks `Yuranime`** menggunakan `Yuranime-logo-font.ttf` (`Calistoga-Regular`).
2. **Siklus Animasi Teratur & Tenang via `LaunchedEffect`:**
   - **Siklus Awal:** Teks `Yuranime` muncul keluar dari balik ikon rubah dengan gerakan *slide* ke kanan (`translationX` + `alpha` + `clipToBounds`) langsung disusul sapuan kilau cahaya (*linear gradient shimmer sweep*) yang melintas di permukaan teks. Setelah itu masuk ke **jeda tenang selama ~4–5 detik**.
   - **Siklus Berkala Berikutnya (Animasi Ikon disusul Teks):**
     1. Teks `Yuranime` ditarik/disembunyikan masuk kembali ke balik ikon rubah (`alpha -> 0f`, `translationX -> 0f`).
     2. Jalankan animasi *motion graphics* pada **Ikon Rubah & Busur Merah**: dua busur merah berputar menyapu lingkaran (sweep rotation 360°) dan siluet rubah melakukan animasi *scale pulse* yang halus (1.0f -> 1.08f -> 1.0f).
     3. Begitu animasi ikon selesai, teks `Yuranime` **ngeslide keluar ke arah kanan** dari balik ikon rubah, diikuti sapuan **efek cahaya mengkilap (*glossy shimmer*)**, lalu kembali ke **jeda tenang selama ~4–5 detik**.

### B. Desain Baru Hero Slider
1. **Background Cover:**
   - Menampilkan gambar cover anime secara lebar dengan *overlay* gelap tingkat **sedang** (tidak hitam pekat di tengah, memakai vignette sedang dan gradasi halus ke `#0B0B0B` di batas bawah agar gambar cover tetap hidup).
2. **Poster di Pojok Kiri Bawah:**
   - Kartu poster vertikal rasio `2:3` (`aspectRatio(2f/3f)`), sudut membulat `10.dp`, lebar `105.dp`–`115.dp`, ditempatkan di pojok kiri bawah.
3. **Kolom Informasi di Sebelah Kanan Poster:**
   - Disusun dalam `Row` dengan `Alignment.Bottom`.
   - **Pembatas Ketinggian:** Kolom teks kanan dibatasi agar **TIDAK PERNAH melewati batas atas gambar Poster** (`Modifier.heightIn(max = posterHeight)`).
   - **Judul Anime:** Menggunakan font **`Yuranime-logo-font.ttf`** (**Calistoga-Regular**) warna putih bersih. Jika judul pendek (seperti `ONE PIECE`), tampil 1 baris. Jika panjang, otomatis menyesuaikan ukuran (`20.sp`–`26.sp`, `maxLines = 2`) dengan bagian atas teks tetap berada di bawah/sejajar dengan batas atas poster.
   - **Sinopsis Singkat:** Di bawah judul, sinopsis ringkas (`maxLines = 2` atau `3`, `overflow = TextOverflow.Ellipsis`, font SF Regular/Medium `12.sp`–`13.sp`, warna `#CBD5E1`).
   - **Baris Bawah (Tombol CTA + Indikator Slide):**
     - Sisi Kiri: Tombol kapsul merah menyala (`#E11D48` / `#EF4444`) berisi ikon play putih di dalam lingkaran dan teks **`TONTON SEKARANG`** (SF Bold putih). Klik tombol langsung menuju **Halaman Detail Anime** (`onAnimeClick(anime.id)`).
     - Sisi Kanan: Indikator paginasi titik slide sejajar dengan tombol (titik aktif berupa kapsul putih memanjang, titik lainnya bulat abu-abu).

---

## 4. SPESIFIKASI TAHAP 3: Halaman Login Saat Klik `PROFILE` di Bottom Navbar
*Mengacu pada video referensi `/storage/emulated/0/termux/design/reff/VID-20261101-WA0035.mp4` dan aset poster di `/storage/emulated/0/termux/design/reff/poster/`*

1. **Background Kolase Poster Anime Bergerak Naik-Turun:**
   - Menggunakan 12 poster offline dari `/storage/emulated/0/termux/design/reff/poster/` (`anime_01.jpg` s/d `anime_12.jpg`) yang disimpan di drawable / assets.
   - 3 kolom *grid* poster dengan sudut kemiringan diagonal halus (`graphicsLayer { rotationZ = -12f; scaleX = 1.25f; scaleY = 1.25f }`).
   - Pergerakan *infinite auto-scroll* perlahan: kolom kiri & kanan bergerak ke atas, kolom tengah bergerak ke bawah persis seperti video referensi.
   - Lapisan *gradient overlay* gelap di atasnya (semi-transparan di atas dan tengah, lalu pekat `#0B0B0B` di bawah).
2. **Logo Utama di Tengah-Atas:**
   - Menampilkan logo lengkap **`yuranime-full-logo.svg`** / lockup emblem + Calistoga wordmark di posisi tengah-atas.
3. **Komponen Tombol & Informasi di Bagian Bawah:**
   - **Tombol Google:** Tombol kapsul penuh (`RoundedCornerShape(50)`, background **Putih `#FFFFFF`**, tinggi `52.dp`), berisi ikon Google vector 4-warna di kiri dan teks **`Masuk dengan Google`** (`#111827`, SF SemiBold/Bold) di tengah.
   - **Tombol `Login Tanpa Akun`:** Tombol teks **`Login Tanpa Akun`** (menggantikan teks "Login Tester" di referensi, warna abu-abu terang `#9CA3AF`, SF Medium `14.sp`), memungkinkan pengguna masuk sebagai tamu.
   - **Teks Persetujuan Layanan:** Teks terpusat menggunakan `buildAnnotatedString`:
     `Dengan melanjutkan, kamu menyetujui Ketentuan Layanan dan Kebijakan Privasi` (di mana **Ketentuan Layanan** dan **Kebijakan Privasi** bergaris bawah `TextDecoration.Underline` dan berwarna putih terang).
   - **Teks Versi Aplikasi:** Teks **`Versi 1.3.2`** (atau dari `BuildConfig.VERSION_NAME`) di paling bawah dengan warna abu-abu redup.

---

## 5. SPESIFIKASI TAHAP 4: Perbaikan Randomisasi `Cuplix` di Home, Jadwal, dan Search
1. **Independensi Antar Halaman:**
   - Daftar Cuplix di **Home Screen**, **Jadwal Screen**, dan **Search Screen** tidak boleh sama persis. Masing-masing memuat batch atau variasi acak yang berbeda.
2. **Selalu Berganti Saat Refresh & Saat Buka Ulang Aplikasi:**
   - Tidak ada hard-cache urutan Cuplix statis.
   - Setiap kali *pull-to-refresh* atau saat aplikasi dibuka kembali, feed Cuplix diambil dengan pengacakan dinamis (`.shuffled()`) yang menggabungkan variasi video terbaru dan populer.

---

## 6. SPESIFIKASI TAHAP 5: Verifikasi Build, Release & CI/CD
- Repositori bersih tanpa file video besar (video tetap diabaikan oleh `.gitignore`).
- Commit dan push ke remote `dinzid04/DinzStream-App` di GitHub.
- GitHub Actions workflow build release menghasilkan `Yuranime-v1.9.0.apk` yang terverifikasi sukses.
