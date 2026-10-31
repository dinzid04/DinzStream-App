# Spesifikasi Desain & Panduan Eksekusi Yuranime (Single Source of Truth)

Dokumen ini adalah satu-satunya acuan spesifikasi desain dan teknis aplikasi **Yuranime** di `/storage/emulated/0/termux/Workspace_uiApp/` yang disusun berdasarkan analisis 5 gambar referensi terbaru di `reff_ui/`, aset vektor di `design/logo/`, serta standar Anti-AI-Slop.

---

## 1. Window & Notch Fix: Full-Bleed Status Bar Transparan (Video Player)
*Mengacu pada: `reff_ui/Screenshot_20261031_174938_AnimeIn.jpg`*

1. **Konfigurasi Activity & Window (`MainActivity.kt` & Orientation Listener):**
   - Mengaktifkan `WindowCompat.setDecorFitsSystemWindows(window, false)` secara global.
   - Mengatur warna bilah sistem:
     - `window.statusBarColor = android.graphics.Color.TRANSPARENT`
     - `window.navigationBarColor = android.graphics.Color.TRANSPARENT`
   - Mengaktifkan rendering menembus area poni/kamera notch (*display cutout*):
     - `window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`
     - Pada Android R+ (API 30+), gunakan mode `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` bila tersedia.
2. **Mode Fullscreen / Landscape:**
   - Menyembunyikan seluruh system bars secara imersif menggunakan `WindowInsetsControllerCompat`:
     ```kotlin
     val controller = WindowInsetsControllerCompat(window, window.decorView)
     controller.hide(WindowInsetsCompat.Type.systemBars())
     controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
     ```
   - Mengembalikan system bars saat kembali ke mode Portrait.
3. **Overlay Tombol Atas Video Player (`<-` dan `⚙`):**
   - Menghilangkan bar hitam yang memotong ikon.
   - Menerapkan padding yang aman (`statusBarsPadding()` atau safe cutout insets) pada layer overlay baris atas kontrol video player sehingga tombol kembali (`<-`) di kiri atas dan gerigi pengaturan (`⚙`) di kanan atas berada utuh di dalam area video dan TIDAK PERNAH terpotong notch kamera atau status bar.

---

## 2. Halaman Search (`CARI`): Gambar Utuh, Filter Klik & Pull-to-Refresh
*Mengacu pada: `reff_ui/Screenshot_20261031_211627_AnimeIn.jpg`*

1. **Proporsi Kartu & Keterlihatan Kepala Karakter (`KATEGORI`, `TAHUN`, `KARAKTER`):**
   - Perbesar tinggi kartu (`height`) menjadi `112.dp`–`118.dp` agar ruang vertikal lebih leluasa.
   - Pada kartu `KATEGORI` dan `TAHUN`: gunakan `ContentScale.Crop` dengan `alignment = Alignment.TopCenter` atau `Alignment.TopEnd` agar bagian kepala/wajah karakter anime tampil utuh tanpa terpotong di tepi atas kartu.
   - Pada kartu `KARAKTER`: kartu berlatar `#18202C` dengan ilustrasi karakter di sisi kanan menggunakan `ContentScale.Crop` dan `alignment = Alignment.TopCenter`. Gradasi horizontal dari `#18202C` solid di kiri memudar mulus ke transparan di kanan.
2. **Interaktivitas & Filter Otomatis:**
   - Saat pengguna mengklik item pada:
     - `KATEGORI` (*Theme / Genre / Demographic*, misal: Action, Adventure, Comedy)
     - `TAHUN` (misal: 2026, 2025, 2024)
     - `STUDIO` (misal: MAPPA, Ufotable, Kyoto Animation)
     - `TIPE` (misal: MOVIE, ONA, OVA, LIVE ACTION)
     - `KARAKTER` (misal: Aika, Amane, C.C.)
   - Alihkan langsung ke mode tampilan hasil pencarian (`SearchResultsContent`) dan picu query filter API terkait (`genre_in`, `keyword`, `type`, dsb.) untuk menampilkan anime yang relevan secara dinamis.
3. **Pull-to-Refresh & Acak (Shuffle) Item:**
   - Mengimplementasikan `PullToRefreshBox` (atau `rememberPullToRefreshState`) pada tampilan awal `SearchScreen`.
   - Setiap kali halaman dibuka atau ditarik ke bawah (refresh), lakukan `shuffle()` terhadap daftar `genres`, `studios`, `years`, dan `characters` dari API, sehingga item yang tampil selalu segar dan bervariasi.

---

## 3. Halaman View Cuplix & Komentar Cuplix (100% Persis Referensi)
*Mengacu pada: `reff_ui/Screenshot_20261031_211731_ANIMEIN.jpg` & `Screenshot_20261031_211753_ANIMEIN.jpg`*

1. **Perilaku Video Player Cuplix:**
   - Autoplay seketika saat dibuka dalam latar hitam penuh.
   - **DILARANG menampilkan seekbar, angka durasi/waktu, atau kontrol kecepatan.**
   - Mengetuk area video langsung menghentikan sementara (**Pause**) dan menampilkan bulatan ikon play/pause bersih di tengah; mengetuk lagi langsung melanjutkan (**Play**).
2. **Header Atas & Menu Titik Tiga (`⋮`):**
   - Pojok kiri atas: Tombol panah kembali (`<-`).
   - Pojok kanan atas: Tombol titik tiga (`⋮`). Saat diklik, menampilkan pop-up resolusi Cuplix (misal: 360p, 480p, 720p) dengan sinkronisasi posisi pemutaran.
3. **Tata Letak Kiri Bawah (Creator & Metadata Anime):**
   1. Foto profil lingkaran (avatar pokemon sprite) pengunggah berbingkai oranye.
   2. Username pengunggah warna oranye (`#FF5722`, e.g. `Yuuichii`).
   3. Teks caption di bawah username (warna putih).
   4. **Overlay Judul Anime:** Kapsul/pill warna oranye (`#FF5722` / `#E65100`) dengan teks `[Judul Anime]  >` yang lebarnya menyesuaikan panjang teks (`Modifier.wrapContentWidth()`). Mengetuknya membuka `AnimeDetailScreen(idMovie)`.
   5. **Overlay Episode:** Tepat di bawah kapsul judul, kapsul/pill warna abu-abu gelap (`#26282C`) berisi teks `Episode [Nomor]  >` dengan `wrapContentWidth()` (sehingga panjang kapsul episode terlihat lebih pendek daripada kapsul judul di atasnya, persis referensi).
4. **Tata Letak Kanan Bawah (Action Sidebar):**
   1. Ikon Hati outline putih (`♡` / merah bila disukai) dengan angka like di bawahnya (`440`).
   2. Ikon Komentar outline putih dengan angka komentar di bawahnya (`8`).
   3. Poster anime kecil persegi panjang vertikal rounded di paling kanan bawah (sejajar kapsul judul/episode).
5. **Panel Komentar Cuplix:**
   - Mengetuk ikon komentar membuka bottom sheet di paruh bawah layar sementara video tetap terlihat di atas.
   - Input rounded `Tulis komentar..` dengan panah kirim oranye (`>`), diikuti daftar komentar asli dari API (`/data/fyp/comment/list`).

---

## 4. Section `Paling Populer` di Home (Top 10 Ranking Cards)
*Mengacu pada: `reff_ui/IMG-20261031-WA0125.jpg`*

1. **Struktur Kartu:**
   - 10 urutan teratas (`1` sampai `10`) tersusun vertikal ke bawah di Home Screen.
   - Tinggi kartu sekitar `84.dp`–`88.dp`, sudut membulat `14.dp`, warna latar abu-abu gelap pekat `#141414`.
2. **Peringkat (Rank 1–10):**
   - **Peringkat 1, 2, 3:**
     - Garis aksen lengkung berwarna emas (`#D4AF37` / `#F5B041`) di tepi kiri kartu.
     - Angka `1`, `2`, `3` sangat besar (`36.sp`), miring (*Italic*), **SF Bold**, warna emas.
   - **Peringkat 4 sampai 10:**
     - Tanpa garis emas di tepi kiri.
     - Angka `4`–`10` besar (`30.sp`), miring (*Italic*), **SF Bold**, warna abu-abu gelap transparan (`#2C2C2C`).
3. **Judul & Bintang:**
   - Di sebelah kanan angka peringkat: Judul anime huruf kapital putih tebal (`UPPERCASE`, **SF Bold**, 1 baris dengan `ellipsis`).
   - Tepat di bawahnya: 5 ikon bintang emas (`★★★★★`) diikuti angka jumlah favorit berwarna emas (`favorites`, e.g. `39102`).
4. **Gambar Cover dengan Opacity Gradient:**
   - Di separuh kanan kartu: gambar cover anime dengan *horizontal gradient brush mask* (opasitas memudar dari 100% gelap `#141414` di bagian tengah ke transparan di sisi kanan).

---

## 5. Global Image Placeholder (`yuranime_outline.svg`) & Rebranding Aplikasi
*Aset: `design/logo/yuranime_outline.svg` & `design/logo/yuranime-icon-app.svg`*

1. **Global SVG Placeholder:**
   - Mengonversi `yuranime_outline.svg` menjadi vector drawable `ic_yuranime_outline.xml` di `res/drawable/`.
   - Menggunakan `ic_yuranime_outline` di tengah-tengah (`Alignment.Center`) dengan latar gelap `#121622` sebagai placeholder saat memuat dan fallback error pada seluruh pemanggilan `AsyncImage` di seluruh aplikasi.
2. **Rebranding:**
   - Nama aplikasi: **`Yuranime`** di `strings.xml` dan `AndroidManifest.xml`.
   - Ikon launcher: `yuranime-icon-app.svg` dikonversi ke `ic_launcher_foreground.xml` dan `ic_launcher_background.xml`.

---

## 6. Build & GitHub Release Workflow
- Kode wajib terkompilasi bersih tanpa peringatan fatal.
- Commit dan push ke remote `main`.
- Memantau GitHub Actions CI/CD hingga build sukses dan merilis file APK **Yuranime** terbaru.

---

## 7. Perbaikan Bug Kritis: Search Filter, Cuplix Like/Comment & Seamless Autoplay (v1.7.0)
1. **Search Filter Architecture (Reverse Engineered dari AnimeIn APK `classes7.dex`):**
   - Menggunakan endpoint filter dedikasi:
     - `/3/2/explore/movie_genre` dengan parameter `id_genre`, `genre_in`, `sort`, `page`
     - `/3/2/explore/movie_studio` dengan parameter `studio`, `sort`, `page`
     - `/3/2/explore/movie_type` dengan parameter `type`, `sort`, `page`
     - `/3/2/explore/movie_year` dengan parameter `year`, `season`, `sort`, `page`
     - `/3/2/explore/movie` dengan parameter `keyword`, `sort`, `page`
   - Indikator Active Filter Chip di bagian atas dengan tombol `[✕]` untuk membatalkan filter.
   - Sembunyikan 'Rekomendasi Populer' saat filter aktif sehingga daftar anime murni menampilkan hasil filter yang diklik.
   - Mendukung sorting (`views`, `alphabet`, `new`), toggle Grid/List, serta pagination infinite scroll.
2. **Cuplix Metric Keys & Comment Synchronization:**
   - Serialized keys asli terverifikasi:
     - `@SerializedName(value = "count_likes", alternate = ["likes", "like", "total_likes", "count_like"])`
     - `@SerializedName(value = "count_comments", alternate = ["comments", "comment", "total_comments", "count_comment"])`
     - `@SerializedName(value = "count_views", alternate = ["views", "view", "total_views", "count_view"])`
     - `@SerializedName(value = "is_like", alternate = ["is_liked", "liked"])`
   - Sinkronisasi realtime `commentsCountMap` dan `likesCountMap` di `CuplixViewModel` dengan data detail komentar yang dimuat di background agar tidak pernah menampilkan `0` jika terdapat komentar riil.
3. **Cuplix Seamless Autoplay & Zero-Black-Screen Crossfade:**
   - Autoplay instan saat page cuplix aktif (`playWhenReady = true; prepare(); play()`).
   - Mencegah kedipan layar hitam saat buffering/inisialisasi player dengan listener `Player.Listener.onRenderedFirstFrame()` yang mempertahankan poster thumbnail (`AsyncImage`) menutupi `PlayerView` hingga frame video pertama siap, lalu melakukan transisi fade-out halus (`tween(300)`).
4. **Interaktivitas Cuplix di Detail Anime (`AnimeDetailScreen`):**
   - Menghubungkan klik kartu Cuplix di tab Cuplix (`AnimeDetailScreen`) ke navigation controller `onCuplixClick(id)` di `MainActivity.kt` menuju `selectedCuplixId = cuplixId` sehingga pengguna dapat langsung memutar video cuplix tersebut.
