# Spesifikasi Desain & Rencana Eksekusi Yuranime v1.6.0

Dokumen ini mendokumentasikan spesifikasi teknis dan rencana implementasi perbaikan menyeluruh pada **Video Player**, **Halaman Search (Cari)**, **Paling Populer di Home**, pembangunan **Halaman View Cuplix**, serta penerapan **Global SVG Placeholder & Rebranding ke Yuranime** di `/storage/emulated/0/termux/Workspace_uiApp/` berdasarkan gambar referensi terbaru di `reff_ui/` dan standar Anti-AI-Slop.

---

## 1. Paling Populer di Home (Leaderboard Ranking Cards)
*Referensi: `reff_ui/IMG-20261031-WA0125.jpg`*

Mengganti `LazyRow` horizontal lama menjadi **Daftar Peringkat Vertikal (Leaderboard Stack)** yang elegan dan berbobot:
1. **Struktur Kartu:**
   - Bentuk kartu horizontal dengan tinggi tetap `88.dp`–`92.dp`, sudut membulat `14.dp`, margin horizontal `16.dp`.
   - **Background Ilustrasi:** Menampilkan `image_cover` anime di sisi kanan kartu (`ContentScale.Crop`).
   - **Gradient Masking Halus:** Lapisan gradasi horizontal dari warna kartu gelap solid (`#121622`) di sisi kiri (lebar ~50%), memudar mulus ke transparan (`Color.Transparent`) di sisi kanan, sehingga ilustrasi menyatu alami tanpa garis batas potong yang kasar.
2. **Peringkat Top 3 (Gold / Silver / Bronze Accent):**
   - **Rank 1:** Angka `1` besar bergaya miring tebal (*bold italic* `32.sp`), warna krem emas (`#FBBF24`), border kartu beraksen emas bercahaya (`1.5.dp`, `#F59E0B66`).
   - **Rank 2:** Angka `2` besar warna perak/krem (`#E2E8F0`), border kartu perak halus (`1.dp`, `#94A3B840`).
   - **Rank 3:** Angka `3` besar warna amber/perunggu (`#F59E0B`), border kartu amber halus (`1.dp`, `#D9770640`).
   - **Rank 4 s/d 10:** Angka rank timbul gelap (*muted embossed* `30.sp`, warna `#334155`), background kartu gelap `#131824` dengan border minimalis `#1E2536`.
3. **Tipografi & Metadata:**
   - **Judul Anime:** Huruf kapital penuh (*all-caps*), SF Bold ukuran `15.sp`, warna putih bersih (`#FFFFFF`), maksimal 1 baris dengan `TextOverflow.Ellipsis`.
   - **Rating Bintang & Favorites:** Baris di bawah judul berisi 5 bintang emas (`★★★★★`, `#F59E0B`, ukuran `13.sp`) diikuti jumlah favorit pengguna (diambil dari field `favorites` API, e.g. `39102`, SF SemiBold `13.sp`, warna `#FCD34D`).
4. **Interaksi:** Mengetuk kartu membuka langsung `AnimeDetailScreen(anime.id)`.

---

## 2. Halaman View Cuplix (Vertical Swipeable Feed & Comments)
*Referensi: `reff_ui/Screenshot_20261031_211731_ANIMEIN.jpg` & `Screenshot_20261031_211753_ANIMEIN.jpg`*

Membangun layar baru `CuplixViewScreen.kt` bergaya TikTok/Reels untuk memutar cuplikan video pendek anime:
1. **Vertical Pager Video Playback:**
   - Menggunakan `VerticalPager` (atau `LazyColumn` dengan snapping per halaman) yang dapat di-swipe ke atas/bawah.
   - Setiap item cuplikan memuat video dari endpoint stream anime (`/3/2/episode/streamnew/{id_episode}`) dan otomatis melompat (*seek*) ke `time_start` (ms), memutar hingga `time_end` (ms), lalu looping secara otomatis dan mulus.
2. **Overlay Aksi Kanan (Action Sidebar):**
   - **Tombol Like:** Ikon hati (Heart) putih + teks jumlah suka (`count_likes` e.g. `440`). Klik melakukan toggle like animasi.
   - **Tombol Komentar:** Ikon balon obrolan + teks jumlah komentar (`count_comments` e.g. `8`). Klik membuka Bottom Sheet komentar.
   - **Poster Anime Thumbnail:** Kartu rounded kecil (`width = 44.dp`, `height = 62.dp`) di pojok kanan bawah yang menampilkan poster resmi anime. Mengetuknya membuka `AnimeDetailScreen(id_movie)`.
3. **Overlay Info Kiri Bawah (Creator & Anime Metadata):**
   - **Avatar Pengguna:** Avatar lingkaran kecil (`36.dp`) dengan foto/pokemon avatar pengguna.
   - **Nama Pengguna:** Teks warna oranye kemerahan (`#FF5722`), SF SemiBold `14.sp`.
   - **Deskripsi / Caption:** Teks deskripsi pendek (e.g. `☕ 🗿`).
   - **Pill Anime (Oranye):** Kotak pill oranye (`#FF5722`) berisi `[Judul Anime] >` yang dapat diklik untuk membuka Detail Anime.
   - **Pill Episode (Transparan Gelap):** Kotak pill semi-transparan `#00000080` berisi `Episode [Nomor] >` yang dapat diklik untuk membuka Streaming Screen pada episode tersebut.
4. **Header Bar Atas:**
   - Tombol kembali (`<-`) di kiri atas dan tombol menu tiga titik (`⋮`) di kanan atas, di atas gradasi hitam halus.
5. **Cuplix Comment Bottom Sheet:**
   - Menampilkan modal bottom sheet saat tombol komentar ditekan.
   - **Input Bar Atas:** Kolom input rounded `#1A202C` bertuliskan placeholder "Tulis komentar.." dan tombol kirim panah oranye (`#FF5722`).
   - **Daftar Komentar:** Mengambil data komentar asli dari `/data/fyp/comment/list?id_fyp={id}&page=1`.
   - Menampilkan avatar karakter pokemon (`https://assets.animein.net/images/pokemon/{pokemon}.png`), username, timestamp relatif (e.g. `5 months ago`), dan isi komentar pengguna.

---

## 3. Video Player & Fullscreen Polish
*Referensi: `reff_ui/Screenshot_20261031_174938_AnimeIn.jpg`*

1. **Kontrol Player Fullscreen:**
   - **Tengah:** Tombol Mundur 10 detik (`<<`), Bulatan Play/Pause bergaris putih tipis dengan ikon bersih di tengah, Tombol Maju 10 detik (`>>`).
   - **Bar Bawah:**
     - Waktu saat ini di kiri (e.g. `04:50`, SF Medium `13.sp`).
     - Seekbar durasi warna putih bersih (`#FFFFFF`) tebal dengan buffer line semi-transparan.
     - Total durasi di kanan (e.g. `23:39`).
     - Tombol toggle fullscreen di sudut kanan bawah.
   - **Bar Atas:** Tombol Back (`<-`) di kiri atas dan Ikon Gerigi Settings (`⚙`) di kanan atas.
2. **Floating Popup Menu (Speed, Resolution, Auto Next, Lock):**
   - Memastikan floating menu gaya PiuPiu di pojok kanan atas player tetap aktif dan responsif di mode fullscreen maupun portrait tanpa merusak layout player.
   - Penggantian server/resolusi video mempertahankan posisi playback terakhir tanpa ter-reset ke awal.

---

## 4. Halaman Search (Cari) Polish
*Referensi: `reff_ui/Screenshot_20261031_211627_AnimeIn.jpg`*

1. **Verifikasi Visual:**
   - Mempertahankan kartu `KATEGORI` full banner dengan label kategori dan teks 22sp.
   - Mempertahankan kartu `TAHUN` dengan ilustrasi kiri dan tahun kanan.
   - Mempertahankan kartu `STUDIO` dan `TIPE` dengan font putih SF Bold dan tinggi 58dp.
   - Mempertahankan kartu `KARAKTER` dengan masking horizontal gradient fade dari `#18202C` ke transparan.
2. **Navigasi Search Sub-Pages:**
   - Memastikan klik pada `Cuplix` di Search membuka CuplixViewScreen.
   - Memastikan pagination dan filter genre/type bekerja presisi.

---

## 5. Global SVG Placeholder & Rebranding ke Yuranime

1. **Rebranding:**
   - Mengubah nama aplikasi di `res/values/strings.xml` menjadi **"Yuranime"**.
   - Memperbarui header logo di `HomeScreen` menggunakan monogram dan wordmark Yuranime baru.
2. **Ikon Aplikasi Resmi (Adaptive Icon):**
   - Memasang vector drawable ikon resmi Yuranime (Kitsune Play) yang telah divalidasi ke `res/drawable/ic_launcher_foreground.xml` dan latar `res/drawable/ic_launcher_background.xml`.
3. **Global SVG Placeholder System (Coil):**
   - Membuat vector drawable `placeholder_yuranime.xml` (siluet geometris Kitsune Play Yuranime pada latar slate gelap `#121622`).
   - Mengintegrasikannya sebagai `placeholder()` dan `error()` default pada seluruh komponen kartu anime (`AnimeVerticalCard`, `AnimeWideCard`, `ContinueWatchingCard`, `TopPromoBanner`, `StreamingScreen`, `AnimeDetailScreen`, `SearchScreen`).
   - Menghilangkan kotak abu-abu polos dan broken link saat gambar sedang dimuat atau mengalami kendala jaringan.
