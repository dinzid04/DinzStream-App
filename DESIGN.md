# Spesifikasi Desain & Panduan Pembaruan Yuranime (Single Source of Truth)

Dokumen ini adalah acuan spesifikasi desain dan arsitektur teknis aplikasi **Yuranime (`com.yuranime.dinz`)** di `/storage/emulated/0/termux/Workspace_uiApp/` berdasarkan gambar referensi resmi `/storage/emulated/0/termux/design/reff/4d7ec2d15d1d3b7dd4b9ab92096e7692.jpg`, konfigurasi Firebase dari proyek `nimedinz` (`dinz-ai-gemini-copy`), dan kepatuhan penuh terhadap aturan `.agents/skills/anti-slop/SKILL.md` serta `/antislop-ui`.

---

## 1. Identitas Brand, Tipografi & Parameter Desain (Anti-Slop Direction)

- **Design Read:** Platform streaming anime mobile native Android untuk penonton Indonesia bergaya *dark cinematic anime* berkelas, dengan sentuhan *cosmic & gold luxury* pada Papan Peringkat, serta *clean iOS Dark Glassmorphism* pada halaman Profile, Edit Profil, Points & Rewards, dan Unduh. Menghilangkan elemen template murah, kotak kaku tebal, dan teks placeholder tanpa arah.
- **Dial Parameter Anti-Slop:** `ENERGY 2 / RHYTHM 2 / MOTION 2`.
- **Palet Warna Resmi (R-01, R-29):**
  - Background Utama: `#0B0B0B` (Home, Splash, Video Player) & `#0E0F12` (Profile & Sub-screens)
  - Background Kosmik Leaderboard: `#0A071B` / `#0D0A22` dengan aksen gradasi nebula `#19113B`
  - Kartu Papan Peringkat: `#141026` / `#18132F` dengan garis tepi halus `#2D2258` dan ornamen sudut emas `#EAB308` / `#FACC15`
  - Podium Emas (TOP 1): Gradasi `#FFF1B8` -> `#F59E0B` -> `#D97706` (Wings emas berkilau)
  - Podium Perak (TOP 2): Gradasi `#EEF2FF` -> `#818CF8` -> `#6366F1` (Wings perak kebiruan)
  - Podium Perunggu (TOP 3): Gradasi `#FFE4E6` -> `#FB7185` -> `#E11D48` (Wings coral-rose)
  - Kontainer Kartu Menu: `#161B26` / `#1A1C20` dengan border `0.5.dp` `#26FFFFFF`
  - Aksen Utama: Crimson Red `#E11D48` / `#EF4444`, Yuranime Orange `#FF5722` / `#F97316`, Golden Star `#FBBF24`
  - Teks Utama: `#FFFFFF` (Putih Bersih)
  - Teks Sekunder: `#CBD5E1` / `#94A3B8` / `#A5B4FC`
- **Tipografi Resmi (R-06):**
  - Brand Wordmark: **`Yuranime-logo-font.ttf`** (**Calistoga-Regular**)
  - UI Umum, Navigasi, Menu, Skor & Papan Peringkat: **SF Pro Display** (`Regular`, `Medium`, `SemiBold`, `Bold`)

---

## 2. SPESIFIKASI FITUR 1: Real-Time Google Login & Firebase Database Architecture

Menggantikan sistem login placeholder/dummy dengan **Real-Time Firebase Authentication + Cloud Firestore** yang terintegrasi langsung dengan database `nimedinz`:

### A. Konfigurasi Proyek Firebase (`dinz-ai-gemini-copy`)
1. **Identitas & Kunci Proyek:**
   - **`project_id`**: `dinz-ai-gemini-copy`
   - **`project_number`**: `400464992596`
   - **`api_key`**: `AIzaSyAPZsUFl1RoV3bfMWWOsJ0M9yt_YPqrRSg`
   - **`storage_bucket`**: `dinz-ai-gemini-copy.firebasestorage.app`
   - **`firestore_database_id`**: `ai-studio-eff2e4c3-4396-44bb-b0c9-0936b4c631a4` (atau `(default)`)
   - **`application_id` Android**: `1:400464992596:android:68fde66c88d351699610c1`
   - **`package_name`**: `com.yuranime.dinz`
2. **File Konfigurasi `app/google-services.json`:**
   - Menyertakan file `google-services.json` yang memuat konfigurasi lengkap project `dinz-ai-gemini-copy` dengan package name `com.yuranime.dinz`.
   - Menginisialisasikan `FirebaseApp` secara aman melalui `FirebaseOptions.Builder` sebagai inisialisasi eksplisit agar tidak terjadi crash saat start pada build release atau debug.

### B. Alur Real-Time Google Sign-In & Fallback Web OAuth
1. **Dual Flow Login Google:**
   - **Flow Utama:** Menggunakan Google Credential Manager / `GoogleSignInClient` dengan Web Client ID untuk mendapatkan ID Token, lalu login via `FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`.
   - **Flow Fallback (100% Reliable di Semua HP):** Menggunakan `FirebaseAuth.getInstance().startActivityForSignInWithProvider(activity, OAuthProvider.newBuilder("google.com").build())` via Chrome Custom Tabs yang menjamin login Google sukses tanpa ketergantungan pencocokan SHA-1 fingerprint keystore lokal.
2. **Sinkronisasi Profil Dokumen Firestore (`users/{uid}`):**
   - Saat user pertama kali login atau membuka aplikasi:
     - Dokumen `users/{uid}` disinkronkan secara *real-time* via snapshot listener.
     - Struktur field:
       - `uid`: String ID unik dari Google
       - `name`: String nama asli dari akun Google (dapat diedit)
       - `username`: String tag pengguna (contoh `@gading_04`)
       - `email`: String email pengguna
       - `avatar`: String URL foto profil (default foto Google)
       - `banner`: String URL gambar cover (default banner anime berkualitas tinggi)
       - `bio`: String bio profil
       - `level`: Int level saat ini (dimulai dari `1`)
       - `exp`: Long akumulasi XP pengguna (default `0`)
       - `points`: Long koin/poin reward pengguna (default `100`)
       - `lastCheckInDate`: Long timestamp atau string tanggal check-in harian terakhir
       - `levelInfo`: Object `{ level: 1, exp: 0, unlockedBadges: [] }` (kompatibel penuh dengan `nimedinz`)
3. **Akumulasi XP & Poin Real-Time:**
   - Menonton episode: `+20 XP` & `+10 Points`
   - Memberi komentar/reaksi: `+10 XP` & `+5 Points`
   - Daily Check-In: `+50 XP` & `+10..50 Points`

---

## 3. SPESIFIKASI FITUR 2: Perbaikan Halaman Profile (Anti-Slop & Anti-Gap)

1. **HAPUS Baris Tombol Media Sosial:**
   - **Hapus sepenuhnya** baris scrollable tombol `WhatsApp`, `Telegram`, `Instagram`, `TikTok`, dan `Discord` dari `ProfileScreen`!
2. **Header Kompak & Terpadu:**
   - **Cover Banner:** Tampil di bagian atas dengan rasio proporsional, bergradasi halus menghitam ke warna background `#0E0F12`.
   - **Avatar Bulat:** Diameter `76.dp` memuat foto asli dari Google/Firebase, dilengkapi border oranye tipis (`#FF5722`) dan badge kamera biru (`#3B82F6`) untuk edit foto.
   - **Nama & Level Info:** Tepat di sebelah kanan avatar, menampilkan **Nama Pengguna** (SF Bold Putih `20.sp`), ikon edit pensil, badge status **`Lv. [Level]`**, serta bar progres XP tipis (`4.dp`) dengan indikator angka XP saat ini menuju level berikutnya.
3. **Daftar Menu Rapat & Bersih (`Arrangement.spacedBy(8.dp)`):**
   - Langsung menempel tepat di bawah header profil tanpa ruang kosong besar (`Spacer(12.dp)`):
     - **`Edit Profil`** (*"Ganti Nama, Foto atau Cover"*) -> Navigasi ke Layar Edit Profil
     - **`Unduh`** (*"Kelola konten yang diunduh"*) -> Navigasi ke Layar Unduh
     - **`Papan Peringkat`** (*"Lihat peringkat XP bulanan"* — badge `TOP 100`) -> Navigasi ke Layar Papan Peringkat
     - **`Points & Rewards`** (*"Check-in harian & tukar poin"* — badge `★ [Poin Real-time]`) -> Navigasi ke Layar Rewards
     - **`Info`** (*"Simak Cerita Kami"*) -> Modal info aplikasi & lisensi
     - **`Keluar Akun`** -> Logout bersih memanggil `FirebaseAuth.getInstance().signOut()` dan reset state auth ke layar login.

---

## 4. SPESIFIKASI FITUR 3: Halaman Khusus Papan Peringkat (100% Mengacu Gambar Referensi)

Halaman `LeaderboardScreen` dibuka saat menu "Papan Peringkat" diklik, mengambil data peringkat XP seluruh pengguna dari koleksi `users` di Firebase secara *real-time*.

### A. Area Podium Atas (`TOP 1`, `TOP 2`, `TOP 3`)
- Mengacu persis pada komposisi gambar `/storage/emulated/0/termux/design/reff/4d7ec2d15d1d3b7dd4b9ab92096e7692.jpg`.
- Latar belakang kosmik malam pekat bertabur bintang lembut (`#0A071B` / `#0D0A22`) dengan pendar cahaya halus di belakang podium.
1. **Posisi Tengah-Atas (`TOP 1` - Juara 1):**
   - Avatar bulat paling besar (`88.dp`) dibingkai dengan **ornamen sayap & lingkaran emas berkilau**, serta badge pelat logam **`TOP 1`** di bawah avatar.
   - Di bawah badge terdapat panji/perisai perisai emas berujung lancip dengan gradasi warna **Emas-Oranye Cerah** (`#FFF1B8` -> `#F59E0B` -> `#D97706`).
   - Di dalam perisai tercantum: **Nama Pengguna** (SF Bold) dan **Total Skor XP** (SF Bold tebal) di bawahnya, diakhiri dengan aksen belah ketupat emas berkilau di ujung lancip bawah.
2. **Posisi Kiri (`TOP 2` - Juara 2):**
   - Posisinya sedikit lebih rendah dari Top 1.
   - Avatar bulat (`72.dp`) dibingkai dengan **ornamen sayap perak / biru-ungu**, badge pelat logam **`TOP 2`**.
   - Panji perisai gradasi **Biru-Ungu / Perak** (`#EEF2FF` -> `#818CF8` -> `#6366F1`) memuat Nama Pengguna dan Total Skor XP.
3. **Posisi Kanan (`TOP 3` - Juara 3):**
   - Posisi sejajar dengan Top 2 di sisi kanan.
   - Avatar bulat (`72.dp`) dibingkai dengan **ornamen sayap perunggu / coral-pink**, badge pelat logam **`TOP 3`**.
   - Panji perisai gradasi **Coral-Pink / Rose** (`#FFE4E6` -> `#FB7185` -> `#E11D48`) memuat Nama Pengguna dan Total Skor XP.

### B. Baris Header Tabel
- Di bawah area podium terdapat bar horizontal transparan gelap dengan 3 kolom penanda:
  - **Kiri:** `Peringkat` (SF Medium `#938CB7` `13.sp`)
  - **Tengah:** `Pengguna` (SF Medium `#938CB7` `13.sp`)
  - **Kanan:** `Total XP` diikuti ikon belah ketupat kecil **`?`** (yang ketika diklik memunculkan dialog panduan cara mendapatkan XP).

### C. Daftar Peringkat `4` sampai `100` (Kartu Ornamen Kosmik Gelap)
- Menggunakan `LazyColumn` vertikal yang halus:
- Setiap baris berupa kartu horizontal berwarna ungu-gelap kosmik (`#141026` / `#18132F`) dengan **aksen garis sudut emas tipis** di keempat pojoknya (`#EAB308` / `#FACC15`) dan ornamen mahkota sayap mini di bagian tengah garis atas.
- **Sisi Kiri:** Bentuk **Belah Ketupat Putih Bersih (`◇`, rotasi 45°)** dengan angka peringkat tebal (`4`, `5`, `6`, dst.) di tengahnya.
- **Sebelah Kanan Belah Ketupat:** **Avatar Bulat** pengguna (`46.dp`) dengan border halus.
- **Tengah:** **Nama Pengguna** (font **SF Bold** putih `15.sp`).
- **Kanan:** **Jumlah Skor XP** (font **SF Bold** warna emas terang `#FDE68A` `16.sp`).
- **Sticky Bottom Bar (Peringkat Pengguna):** Di bagian paling bawah layar terdapat bar melayang yang menampilkan posisi peringkat, avatar, nama, dan XP milik pengguna yang sedang login saat ini.

---

## 5. SPESIFIKASI FITUR 4: Rombak Halaman Edit Profil, Points & Rewards, dan Unduh

### A. Halaman `Edit Profil` (Real-Time Cloud Sync)
- **Preview Cover Banner & Avatar:**
  - Menampilkan preview banner cover di atas dan avatar profil bulat di kiri bawah.
  - Masing-masing dilengkapi tombol kamera bersih untuk memilih foto dari galeri HP via `ActivityResultContracts.PickVisualMedia` atau memilih dari koleksi avatar/cover anime siap pakai.
- **Formulir Bersih:**
  - Input teks bersih untuk **Nama Tampilan (Display Name)** dan **Bio Singkat**.
  - Tombol **`Simpan Perubahan`** bergaya iOS berlatar Yuranime Orange (`#FF5722`), memanggil `UserProfileChangeRequest` pada `FirebaseAuth` dan melakukan `updateDoc` pada dokumen Firestore `users/{uid}` secara *real-time*.

### B. Halaman `Points & Rewards` (Daily Check-In & Tukar Hadiah)
- **Header Saldo Poin:**
  - Card bertema gelap dengan ikon bintang emas berkilau (`#FBBF24`) dan angka poin *real-time* dari Firebase.
- **Kalender Check-In 7 Hari:**
  - 7 kapsul hari bersih (`Hari 1` s/d `Hari 7`) dengan nominal reward (`+10`, `+15`, `+20`, `+25`, `+30`, `+40`, `+50`).
  - Tombol aksi **`Klaim Check-In Hari Ini`** yang otomatis non-aktif jika sudah diklaim pada hari yang sama (tercatat di Firebase).
- **Misi Harian & Penukaran:**
  - Daftar misi harian bersih tanpa border kaku (*Tonton 1 Episode = +20 Poin*, *Beri Reaksi Komentar = +10 Poin*).
  - Daftar item penukaran (*Badge Komentar Spesial*, *Bingkai Avatar Emas*, *Title Level Otaku Sepuh*) yang bisa ditukarkan dengan poin.

### C. Halaman `Unduh` (Manajemen Konten Offline)
- **Indikator Penyimpanan:**
  - Bar tipis minimalis di bagian atas yang menampilkan penggunaan memori penyimpanan perangkat.
- **Tab Filter:**
  - Tab bersih: `Selesai` dan `Sedang Mengunduh`.
- **Daftar Episode:**
  - Thumbnail 16:9 di sisi kiri, judul anime, nomor episode, dan ukuran file di tengah, tombol putar langsung dan hapus di sisi kanan.
  - *Empty State:* Menampilkan visual bersih dengan vektor `ic_yuranime_monochrome` dan teks proporsional yang mengarahkan penonton untuk mengunduh episode anime favorit.

---

## 7. SPESIFIKASI FITUR 5: Implementasi Presisi `Ulang Tahun Hari Ini` di Home & Halaman Detail Karakter (100% Sesuai Gambar Referensi)

Berdasarkan analisis visual mendalam terhadap gambar referensi `/storage/emulated/0/termux/design/reff/file_00000000d52c8211a7e0a165e64eec65.png`:

### A. Section `Ulang Tahun Hari Ini` di Halaman Home (Sisi Kiri Referensi)
1. **Header Section:**
   - **Kiri:** Teks **`Ulang Tahun Hari Ini`** (**SF Pro Display Bold**, warna Putih `#FFFFFF`, ukuran `18.sp`) berdampingan dengan penanda titik bullet dan tanggal hari ini ` • 4 Nov` (atau format dinamis, **SF Pro Medium**, warna abu-abu `#94A3B8`, ukuran `14.sp`).
   - **Kanan:** Tombol bulat gelap (`size = 32.dp`, warna latar `#1A1D26`, shape `CircleShape`) memuat ikon panah chevron kanan (`>`) berwarna `#94A3B8`.
2. **Kartu Karakter Ulang Tahun (`LazyRow`):**
   - **Dimensi & Bentuk:** Kartu vertikal ramping dengan rasio poster 2:3 (`width = 115.dp`, `aspectRatio(2f/3f)`, `RoundedCornerShape(12.dp)`). **Tanpa border kaku atau stroke garis**.
   - **Gambar:** Foto karakter mengisi penuh seluruh kartu (`Modifier.fillMaxSize()`, `ContentScale.Crop`).
   - **Gradient & Teks:** Diberikan overlay gradasi vertikal halus di bagian bawah di dalam kartu (`Brush.verticalGradient` transparan ke `#E6000000` / `#F0000000` mulai dari 50% tinggi kartu).
   - **Overlay Teks Pojok Kiri Bawah (Di Dalam Kartu):**
     - **Baris 1:** Tanggal ulang tahun berwarna **Oranye Yuranime (`#FF5722`)**, font **SF SemiBold** `11.sp` (contoh: `04 Nov`, `05 Nov`).
     - **Baris 2:** Nama Karakter berwarna **Putih Bersih (`#FFFFFF`)**, font **SF Bold** `13.sp`, 1 baris dengan `TextOverflow.Ellipsis` (contoh: `Kyouka Izumi`, `Hibiya Amamiya`, `Mizusu Togashi`, `Subaru Oki...`).

### B. Halaman Detail Karakter (`BirthdayDetailScreen` / `CharacterDetailScreen` - Sisi Kanan Referensi)
1. **Header Hero & Backdrop:**
   - Tombol kembali bulat gelap (`Size = 40.dp`, icon `ArrowBack`, padding dari status bar) di pojok kiri atas.
   - Backdrop gambar karakter melebar di bagian atas layar dengan gradasi vertikal gelap lembut yang menyatu transparan ke warna latar murni `#0B0B0B`.
2. **Baris Profil Utama (Row Sejajar):**
   - **Kiri:** Foto karakter vertikal potret (`width = 135.dp`, `aspectRatio(2f/3f)`, `RoundedCornerShape(12.dp)`). **Tanpa border**.
   - **Kanan (Kolom Rata Kiri, Vertikal Sejajar Poster):**
     - Label abu-abu (`#94A3B8`, `12.sp`) `Nama Karakter` -> di bawahnya teks nama **SF Bold** putih `22.sp` (`Kyouka Izumi`).
     - Label abu-abu (`#94A3B8`, `12.sp`) `Nama Jepang` -> di bawahnya teks kanji **SF Bold** putih `16.sp` (`泉 鏡花`).
     - Label abu-abu (`#94A3B8`, `12.sp`) `Alias` -> di bawahnya teks alias **SF Bold** putih `15.sp` (`Little Princes`).
3. **Bagian `Deskripsi`:**
   - Label abu-abu (`#94A3B8`, `13.sp`, **SF Medium**) bertuliskan `Deskripsi`.
   - Di bawahnya paragraf teks sinopsis/biografi (warna `#E2E8F0`, `14.sp`, line height proporsional `20.sp`) **tanpa kotak border pembungkus**.
   - Di bawah teks terdapat tombol aksi teks **`Selengkapnya...`** berwarna **Oranye (`#FF5722`, 13.sp, SF SemiBold)** dengan ikon chevron bawah (`⌄`) untuk ekspansi/kolaps teks lengkap.
4. **Bagian `Biodata` (Grid 2 Kolom Bersih Tanpa Kotak Border):**
   - Judul section: **`Biodata`** (**SF Bold**, putih, `17.sp`).
   - Tersusun dalam 2 kolom bersih berdampingan (`Row` dengan 2 `Column(weight = 1f)`):
     - **Kolom Kiri:**
       - Label `Tanggal Lahir` (`#94A3B8`, `12.sp`) -> Nilai **`04 November`** (Putih **SF Bold** `15.sp`)
       - Spacer `12.dp`
       - Label `Tinggi Badan` (`#94A3B8`, `12.sp`) -> Nilai **`148 cm`** (Putih **SF Bold** `15.sp`)
       - Spacer `12.dp`
       - Label `Golongan Darah` (`#94A3B8`, `12.sp`) -> Nilai **`B`** (Putih **SF Bold** `15.sp`)
     - **Kolom Kanan:**
       - Label `Jenis Kelamin` (`#94A3B8`, `12.sp`) -> Nilai **`Perempuan`** (Putih **SF Bold** `15.sp`)
       - Spacer `12.dp`
       - Label `Berat Badan` (`#94A3B8`, `12.sp`) -> Nilai **`40 kg`** (Putih **SF Bold** `15.sp`)
5. **Bagian `Pengisi Suara (Seiyuu)`:**
   - Judul section: **`Pengisi Suara (Seiyuu)`** (**SF Bold**, putih, `17.sp`).
   - Baris profil seiyuu bersih (`Row`, alignment center, **tanpa kotak card border**):
     - **Kiri:** Foto bulat Seiyuu (`size = 54.dp`, `CircleShape`, `ContentScale.Crop`).
     - **Kanan:** Nama Seiyuu (**SF Bold**, putih, `16.sp`, contoh: `Sumire Morohoshi`), dan tepat di bawahnya label bahasa **`Bahasa: Jepang`** berwarna **Oranye (`#FF5722`, 13.sp, SF Medium)**.
6. **Bagian `Daftar Anime Terkait`:**
   - Judul section: **`Daftar Anime Terkait`** (**SF Bold**, putih, `17.sp`).
   - Daftar item anime vertikal bersih tanpa border box (`Arrangement.spacedBy(14.dp)`):
     - **Kiri:** Gambar thumbnail anime persegi membulat (`56.dp x 56.dp`, `RoundedCornerShape(10.dp)`, `ContentScale.Crop`).
     - **Tengah (Column weight = 1f):**
       - Judul Anime (**SF Bold**, putih, `15.sp`, 1 baris `TextOverflow.Ellipsis`).
       - Baris bawah: Teks peran oranye **`Karakter Pendukung`** / **`Karakter Utama`** (`#FF5722`, `13.sp`, **SF Medium**) + spasi + status rilis abu-abu **`Selesai Tayang`** (`#94A3B8`, `13.sp`).
     - **Kanan:** Ikon panah tipis chevron kanan (`>`, warna `#94A3B8`, `size = 18.dp`). Saat diklik, membuka detail anime `AnimeDetailScreen` via ID yang cocok.

---

## 8. SPESIFIKASI FITUR 6: 2 Mode Tampilan Hero Slider di Halaman Home

Aplikasi mendukung 2 mode tata letak Slider Home yang dapat dipilih pengguna:

### A. Mode 1 — Default Slider (Tampilan Bawaan dengan Poster di Kiri Bawah)
- **Status:** **DEFAULT** aktif saat aplikasi pertama kali diinstal.
- **Komposisi:**
  - Background banner cover anime landscape 16:9 di bagian atas.
  - Kartu poster vertikal potret di pojok kiri bawah (`width = 84.dp`, `aspectRatio = 2f/3f`).
  - Di sebelah kanan poster: Judul anime, sinopsis ringkas, tombol `TONTON SEKARANG`, dan pagination dots.

### B. Mode 2 — Clean Cover Slider (Tampilan Baru Sesuai Sisi Kiri Referensi)
- **Status:** Diaktifkan melalui halaman Pengaturan.
- **Komposisi:**
  - Header Atas: Logo animasi Yuranime di kiri, ikon pencarian (`🔍`) bersih di pojok kanan atas banner.
  - **Sembunyikan kartu poster kecil di kiri bawah!**
  - Gambar cover penuh dengan gradasi hitam halus di bagian bawah yang menyatu ke kanvas `#0B0B0B`.
  - Tata letak rata kiri di bagian bawah cover:
    1. **Judul Anime** (menggunakan font `Yuranime-logo-font.ttf` / all-caps bold putih, maks 2 baris).
    2. **Sinopsis Singkat** (maks 2 baris `TextOverflow.Ellipsis`, warna `#CBD5E1`, font SF Regular `13.sp`).
    3. Baris Aksi Bawah: Tombol kapsul merah-oranye **`▶ TONTON SEKARANG`** (`background = Color(0xFFFF2A55)` atau `#FF3366`, `RoundedCornerShape(24.dp)`, padding vertikal `10.dp`, horizontal `18.dp`) di sebelah kiri, dan pagination dots di sebelah kanan.

---

## 9. SPESIFIKASI FITUR 7: Halaman Pengaturan (`SettingsScreen`) & Penyimpanan Preferensi

1. **Akses Pengaturan:**
   - Menu **`Pengaturan`** di `ProfileScreen` dengan ikon gerigi (`Icons.Default.Settings`) dan sub-teks deskriptif *"Atur tampilan slider & preferensi aplikasi"*.
2. **Penyimpanan State:**
   - Disimpan secara lokal dan persisten menggunakan `SharedPreferences` (atau `AppPreferences` singleton) dengan key `KEY_HOME_SLIDER_MODE`:
     - Value `0` / `"DEFAULT"`: Mode 1 (Slider dengan Poster).
     - Value `1` / `"CLEAN_COVER"`: Mode 2 (Slider Tanpa Poster / Clean Cover).
3. **Komponen Pengaturan:**
   - Pilihan Tampilan Slider: Radio item atau Switch toggle yang elegan bergaya iOS dark mode.
   - Pilihan Pemutaran: Toggle Auto Next Episode (default On).
   - Pemeliharaan: Tombol "Bersihkan Cache Gambar & Data Sementara" dengan konfirmasi instan.
   - Info Versi Aplikasi: Teks versi SemVer dan tombol "Periksa Pembaruan".

---

## 10. Kepatuhan Standar Anti-Slop (Delivery Gate Checklist)

- **R-01 & R-29 (Color Craft):** Tidak ada gradasi default biru-ungu AI generik. Warna emas dan kosmik ungu dirancang khusus sesuai identitas referensi podium.
- **R-02 (Copywriting):** Tidak ada karakter em dash pada teks UI. Menggunakan tanda titik dua (`:`), koma (`,`), atau kurung `()`.
- **R-03 & R-39 (Mobile Interaction & Touch Targets):** Semua tombol dan item interaktif memiliki tinggi tap target minimum 44dp dan respons visual klik instan.
- **R-17 & R-36 (Real Data):** Semua data XP, level, poin, profil, dan komik berasal langsung dari API real-time, bukan angka dummy yang dibuat-buat.
- **R-24 & R-26 (Interactive Completeness):** Setiap menu dan kartu memiliki aksi nyata, termasuk navigasi chapter komik, detail komik, dan onboarding.
- **R-27 (UI States):** Tersedia *loading shimmer/spinner*, *empty state*, dan *error recovery* pada semua pembacaan data.

---

## 11. SPESIFIKASI FITUR 8: Integrasi Scrape Komik ke Halaman Home (Menggantikan `Judul Baru`)

Berdasarkan data scraper komik real-time (`https://api.shngm.io/v1`) dan gambar referensi `/storage/emulated/0/termux/design/reff/Screenshot_20260731_160054_Firefox(1).jpg`:

### A. Penggantian Section di `HomeScreen`
1. **Hapus Section `Judul Baru`:**
   - Section `Judul Baru` (`uiState.newTitlesList`) di `HomeScreen` dihapus sepenuhnya.
   - Posisinya digantikan oleh section **Komik (Rilisan Terbaru)**.
2. **Header Section:**
   - Menggunakan komponen `SectionHeader` bawaan Yuranime:
     - Judul teks putih tebal **SF Bold** `16.sp`: `"Komik Terbaru"` atau `"Rilisan Komik Terbaru"`.
     - Kanan: Tombol bulat `30.dp` dengan background `ColorControlCircle` (`#1A1D26`) dan ikon panah chevron kanan (`>`) ber-touch target `48.dp`.
     - **TIDAK menambahkan garis vertikal merah `|` di sebelah kiri judul.**

### B. Desain Kartu Komik (Kompak, Rapat & Mobile-Friendly)
Mengacu pada gambar referensi:
1. **Tata Letak Kartu:**
   - Kartu permukaan gelap halus `#141418` dengan `RoundedCornerShape(12.dp)` dan padding dalam `8.dp` - `10.dp`.
   - Menggunakan `LazyRow` kartu horizontal atau grid 2-kolom horizontal pager yang halus tanpa ruang kosong menganga di tengah.
   - Ukuran kartu proporsional: lebar `290.dp` - `310.dp`, tinggi `128.dp` - `136.dp`.
2. **Sisi Kiri Kartu (Poster Komik):**
   - Gambar poster vertikal dengan `aspectRatio(2f/3f)`, lebar sekitar `80.dp`, sudut membulat `8.dp`, `ContentScale.Crop`, dengan fallback placeholder `R.drawable.placeholder_yuranime`.
3. **Sisi Kanan Atas Kartu (Judul & Bendera):**
   - **Judul Komik:** Teks **SF Bold** putih `14.sp` - `15.sp`, maksimal 2 baris dengan `TextOverflow.Ellipsis`.
   - **Ikon Bendera Negara:** Tepat di bawah judul (jarak `4.dp`), menampilkan bendera kecil (`width = 20.dp`, `height = 14.dp`, `RoundedCornerShape(3.dp)`):
     - **Manhua (China):** `https://flagpedia.net/data/flags/w1160/cn.webp`
     - **Manga (Jepang):** `https://flagpedia.net/data/flags/w1160/jp.webp`
     - **Manhwa (Korea):** `https://flagpedia.net/data/flags/w1160/kr.webp`
4. **Sisi Kanan Bawah Kartu (3 Chapter Terbaru - Rapat & Presisi):**
   - Tepat di bawah bendera dengan jarak rapat (`6.dp` - `8.dp`) sehingga **tidak ada ruang kosong berlebih**:
   - Menampilkan maksimal 3 baris chapter terbaru secara vertikal (`Arrangement.spacedBy(3.dp)`):
     - **Kiri:** Titik bulat kecil oranye-merah (`#FF5722`, diameter `5.dp`) diikuti teks **`CH. [Nomor]`** (font **SF Bold** putih `12.sp`).
     - **Kanan:** Waktu rilis chapter (contoh: `Just now`, `4 minutes ago`, `1 hour ago`, font SF Medium `11.sp`, warna abu-abu `#64748B`).
5. **Aksi & Navigasi Komik:**
   - Klik kartu komik: Membuka layar Detail Komik (`ComicDetailScreen`) yang menampilkan sinopsis, genre, cover besar, status, dan daftar lengkap semua chapter.
   - Klik baris chapter: Membuka layar Baca Komik (`ComicReaderScreen`) yang menampilkan gambar halaman per chapter secara vertikal (webtoon style) yang mulus.
   - Klik tombol chevron `>` pada header: Membuka layar Daftar Semua Komik Terbaru (`ComicViewAllScreen`).

---

## 12. SPESIFIKASI FITUR 9: Petunjuk Pemula Interaktif (First-Time User Onboarding / Spotlight Coachmark)

Berdasarkan referensi visual `/storage/emulated/0/termux/design/reff/Screenshot_20261107_150254_Chrome.jpg`:

### A. Mekanisme & Penyimpanan State
1. Fitur hanya muncul bagi pengguna yang baru pertama kali membuka layar terkait.
2. Status kelulusan disimpan secara persisten di `SharedPreferences` / `AppPreferences`:
   - `KEY_ONBOARDING_HOME_COMPLETED`
   - `KEY_ONBOARDING_DETAIL_COMPLETED`
   - `KEY_ONBOARDING_SEARCH_COMPLETED`
   - `KEY_ONBOARDING_STREAMING_COMPLETED`
3. Pengguna dapat menekan tombol `Skip` kapan saja untuk langsung mengakhiri dan menyimpan status selesai.

### B. Visual Spotlight & Coachmark Tooltip Card
1. **Overlay & Cutout:**
   - Seluruh layar dilapisi overlay gelap semi-transparan (`Color(0xCC000000)`).
   - Area komponen target mendapatkan cutout / sorotan transparan berbingkai membulat dengan garis border merah-oranye menyala (`Color(0xFFEF4444)` / `Color(0xFFFF5722)` tebal `2.dp` plus outer glow tipis).
2. **Coachmark Card:**
   - Latar belakang abu-abu gelap `#16181D`, sudut membulat `16.dp`, border tipis `#2A2E37` (`1.dp`), padding dalam `16.dp`.
   - **Judul Panduan:** Font **SF Bold** putih `17.sp`.
   - **Deskripsi Penjelasan:** Font **SF Medium/Regular** abu-abu terang `#CBD5E1` `13.sp`, line height `18.sp`.
   - **Indikator Progres (Dots):**
     - Langkah aktif: Kapsul memanjang berwarna merah-oranye (`width = 20.dp`, `height = 6.dp`, `#EF4444`).
     - Langkah belum aktif: Titik bulat abu-abu (`size = 6.dp`, `#4B5563`).
   - **Baris Tombol:**
     - Kiri: Tombol teks **`Skip`** (`#94A3B8`, `14.sp`, SF Medium).
     - Kanan:
       - Tombol kapsul gelap **`Previous`** (jika bukan langkah pertama, background `#22252D`, border tipis `#374151`, teks putih `13.sp`).
       - Tombol kapsul merah-oranye **`Next`** / **`Selesai`** (background `#EF4444`, outline tipis oranye, teks putih SF Bold `13.sp`).

### C. Cakupan Layar Onboarding
1. **Layar Beranda (`HomeScreen`):**
   - Langkah 1: *Hero Slider & Tonton Sekarang* (fitur tonton langsung anime terpopuler).
   - Langkah 2: *Cuplix (Video Pendek Anime)* (cuplikan video pendek anime viral).
   - Langkah 3: *Section Komik Terbaru* (baca komik manga, manhwa, dan manhua terupdate).
   - Langkah 4: *Menu Navigasi Bawah* (akses cepat ke Home, Jadwal, Cari, Chat, dan Profile).
2. **Layar Detail Anime (`AnimeDetailScreen`):**
   - Langkah 1: *Tab Informasi & Episode* (ringkasan sinopsis dan daftar episode tayang).
   - Langkah 2: *Tombol Putar & Tambah Favorit* (mulai streaming instan).
   - Langkah 3: *Tab Musim, Cuplix & Poster* (eksplorasi konten terkait).
3. **Layar Pencarian (`SearchScreen`):**
   - Langkah 1: *Kolom Pencarian* (cari judul anime favorit).
   - Langkah 2: *Filter Kategori, Tahun & Studio* (temukan genre dan tema pilihan).
   - Langkah 3: *Sortir & Toggle Tampilan Grid/List* (atur kenyamanan visual pencarian).
4. **Layar Pemutar Video (`StreamingScreen`):**
   - Langkah 1: *Pengaturan Player* (pilih resolusi, kecepatan putar, dan auto-next).
   - Langkah 2: *Daftar Episode* (pindah episode tanpa keluar layar).
   - Langkah 3: *Kolom Komentar* (berdiskusi dengan sesama pecinta anime).

---

## 13. SPESIFIKASI FITUR 10: Perbaikan Total Error Login Google `[ INVALID_APP_ID ]` & Pembuatan `FIREBASE_SETUP.md`

Berdasarkan screenshot bug `/storage/emulated/0/termux/design/reff/Screenshot_20261108_075236_Yuranime.jpg`:

### A. Analisis Masalah
- Error `An internal error has occurred. [ INVALID_APP_ID ]` muncul saat `FirebaseAuth.getInstance().signInWithCredential` dipanggil karena `mobilesdk_app_id` di `google-services.json` bertipe Web (`1:400464992596:web:...`) dan package name `com.yuranime.dinz` belum didaftarkan sebagai Android Client di Firebase Console `dinz-ai-gemini-copy`.
- Fallback lama malah memunculkan popup dialog ungu `"Masuk Akun Yuranime"` yang merusak pengalaman pengguna.

### B. Solusi Arsitektur
1. **Hapus Dialog Popup Fallback:**
   - Dialog formulir login email/sandi manual ungu dihapus total dari alur masuk Google. Pengguna hanya berinteraksi dengan dialog pemilih akun Google resmi Android.
2. **Google Sign-In Standar Bebas Kegagalan ID Token:**
   - Gunakan `GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestProfile().build()`.
3. **Dual-Session Sempurna (Google User Session + Firestore Sync):**
   - Ketika pengguna memilih akun dari Google Sign-In, ambil objek `GoogleSignInAccount`:
     - `googleId`: `account.id ?: account.email.hashCode().toString()`
     - `name`: `account.displayName ?: "Pengguna Google"`
     - `email`: `account.email`
     - `avatar`: `account.photoUrl?.toString()`
   - Simpan data sesi login aktif ini secara lokal di `SharedPreferences` / `AppPreferences`.
   - Lakukan sinkronisasi dokumen profil ke Firestore pada koleksi `users/{googleId}` secara *real-time*.
   - Jika `FirebaseAuth.signInWithCredential` berhasil, gunakan sesi FirebaseUser. Jika melempar `INVALID_APP_ID` atau `FirebaseException`, jangan gagalkan login dan jangan tampilkan toast error: gunakan sesi aktif GoogleSignInAccount tersebut!
   - Pengguna 100% langsung masuk ke halaman profil dengan nama, email, dan foto profil asli mereka tanpa kendala.

### C. Dokumen Panduan Manusia `FIREBASE_SETUP.md`
- Menyediakan file petunjuk praktis di root project `/storage/emulated/0/termux/Workspace_uiApp/FIREBASE_SETUP.md` berbahasa Indonesia lengkap dengan:
  - Cara menambahkan Android App di Firebase Console untuk `com.yuranime.dinz`.
  - Nilai SHA-1 dan SHA-256 keystore.
  - Cara mengaktifkan Google Sign-In di Firebase Authentication.
  - Cara mengunduh dan menimpa file `google-services.json`.

---

## 14. SPESIFIKASI FITUR 11: Perbaikan Gambar Karakter Terpotong di Halaman Search (`CARI`)

Berdasarkan referensi visual `/storage/emulated/0/termux/design/reff/Screenshot_20261108_075327_Yuranime.jpg` dan aset asli banner AnimeIn berukuran 600x200 (rasio 3:1):

### A. Analisis Masalah
- Banner kategori dan karakter memiliki ilustrasi wajah karakter di sisi **kanan**.
- Banner tahun memiliki ilustrasi karakter di sisi **kiri**.
- Implementasi sebelumnya menggunakan `height(114.dp)` dengan `ContentScale.Crop` dan `alignment = Alignment.TopCenter`. Pada layar smartphone (lebar kartu ~328dp-358dp), rasio wadah lebih tinggi dari 3:1 sehingga bagian sisi kiri dan kanan banner terpotong parah (wajah karakter di sisi kanan pada Kategori/Karakter dan di sisi kiri pada Tahun hilang terpotong).

### B. Solusi Tampilan Presisi
1. **Proporsi Wadah Kartu:**
   - Sesuaikan tinggi kartu menjadi `92.dp` (atau rasio aspek `aspectRatio(3.2f / 1f)`) yang proporsional dengan banner 600x200.
2. **Alignment & ContentScale:**
   - Untuk **`KATEGORI`** (`WhiteCategoryCard`) dan **`KARAKTER`** (`DarkCharacterCard`):
     - Gunakan `contentScale = ContentScale.Crop` dengan `alignment = Alignment.CenterEnd` (atau `ContentScale.FillWidth`). Hal ini menjamin seluruh wajah dan ilustrasi karakter di sisi kanan terlihat utuh, tajam, dan tidak terpotong!
   - Untuk **`TAHUN`** (`WhiteYearCard`):
     - Gunakan `contentScale = ContentScale.Crop` dengan `alignment = Alignment.CenterStart`. Hal ini menjamin seluruh wajah dan ilustrasi karakter di sisi kiri terlihat utuh dan tidak terpotong!
3. **Tipografi & Kontras:**
   - Teks grup label (`Genre`, `Theme`, `Demographic`) abu-abu `#6B7280` `12.sp` SF Medium.
   - Teks judul kategori warna gelap `#1E293B` `20.sp` SF Bold (pada Kategori) dan putih bersih pada Karakter.
   - Posisi teks tetap rapi di sisi berlawanan dari karakter ilustrasi sehingga terbaca sempurna tanpa menutupi wajah karakter.

---

## 15. SPESIFIKASI FITUR 12: Redesign Floating Bottom Navigation Bar "Liquid Glass" (Adaptasi quickLiquid)

Mengacu pada repositori `amarnath3003/quickLiquid` dan rekaman referensi `/storage/emulated/0/termux/design/reff/VID-20261108-WA0016.mp4`:

### A. Tata Letak & Perilaku Navigasi (Floating Pill + Active Label Only)
1. **Wadah Mengambang (Floating Bar):**
   - Mengambang di atas konten dengan jarak `padding(horizontal = 20.dp, bottom = 20.dp)` dan `navigationBarsPadding()`.
   - Bentuk kapsul penuh membulat (`RoundedCornerShape(50)` / `CircleShape`), tinggi `64.dp`.
2. **Item Navigasi (Active = Icon + Judul, Inactive = Icon Saja):**
   - **Tab Aktif (Active Tab):**
     - Dibungkus kapsul lensa Liquid Glass yang menonjol di dalam bar navigasi.
     - Menampilkan baris horizontal (`Row`): Ikon Filled (Solid) di kiri + Teks Judul Halaman di kanan (`Home`, `Jadwal`, `Cari`, `Chat`, `Profile`) dengan font **SF Bold** `13.sp` putih `#FFFFFF`.
     - Animasi dinamis: Memanjang dan menyusut menggunakan `AnimatedVisibility` serta `animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))` untuk efek *spring physics / squash and stretch*.
   - **Tab Tidak Aktif (Inactive Tabs):**
     - **Hanya menampilkan Ikon Outline saja** tanpa teks judul sama sekali.
     - Warna ikon abu-abu kebiruan `#8E95A5`, ukuran `22.dp`, dengan area sentuh minimum `48.dp` memenuhi aturan aksesibilitas R-03.

### B. Implementasi Optik "Liquid Glass"
1. **Translucent Dark Base & Refractive Bezel:**
   - Permukaan dasar kaca cair gelap transparan `Color(0xD90E1116)`.
   - Rim highlight fresnel specular di bibir atas-kiri dengan gradasi `Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x14FFFFFF)))`.
   - Efek pembiasan tepi lensa (*refractive edge*) di mana bagian tengah kapsul bening dan tepi membiaskan konten.
   - Pada Android 13+ (API 33+) memanfaatkan runtime shader/render effect, serta fallback multi-layer canvas specular highlight yang efisien untuk versi Android sebelumnya.

---

## 16. SPESIFIKASI FITUR 13: Perbaikan Total Sistem Koordinat Spotlight Onboarding (Dynamic Measurement)

Berdasarkan analisis screenshot bug `/storage/emulated/0/termux/design/reff/Screenshot_20261108_112037_Yuranime.jpg` hingga `112107`:

### A. Larangan Hardcoded Offset & Sistem Pengukuran Real-Time
1. **Dilarang keras memakai offset tebakan / persentase layar statis!**
2. **State Map & Modifier Target:**
   - Dibuat state map `val targetBounds = remember { mutableStateMapOf<String, Rect>() }`.
   - Dibuat modifier extension `Modifier.onboardingTarget(id: String, targetBounds: MutableMap<String, Rect>)` yang mengukur `coordinates.boundsInRoot()` secara presisi begitu komponen ter-attach ke layout tree.
3. **Pemasangan Target di Setiap Layar:**
   - **HomeScreen:**
     - `"home_slider"` pada Hero Slider.
     - `"home_cuplix"` pada section Cuplix.
     - `"home_komik"` pada section Komik Terbaru.
     - `"home_navbar"` pada Liquid Glass Bottom Navbar.
   - **AnimeDetailScreen:**
     - `"detail_play_fav"` pada baris tombol Play dan Favorite.
     - `"detail_tabs"` pada deretan Tab Navigation (INFO, EPISODE, SEASON, CUPLIX, POSTER, COVER).
   - **StreamingScreen:**
     - `"stream_player_settings"` pada Video Player / Ikon Gerigi kontrol.
     - `"stream_episodes"` pada Pemilih Episode.
     - `"stream_comments"` pada area Komentar.
   - **SearchScreen:**
     - `"search_input"` pada kolom input pencarian judul.
     - `"search_categories"` pada kontainer filter Kategori, Tahun, dan Studio.

### B. Auto-Scroll LazyList Sebelum Sorotan Ditampilkan
1. Untuk target yang posisinya berada di bawah viewport (seperti `"home_komik"` di HomeScreen), aplikasi memanggil `lazyListState.animateScrollToItem(targetIndex)` terlebih dahulu.
2. Menunggu animasi scroll selesai (`delay(150)`), kemudian membaca nilai `targetBounds[id]` terbaru sehingga kotak sorotan merah membungkus 100% tepat pada elemen fisiknya.

### C. Posisi Tooltip Card Dinamis
1. Jika elemen target berada di paruh atas layar (`targetRect.center.y < screenHeight / 2`):
   - Tempatkan Tooltip Card tepat di bawah elemen target (`targetRect.bottom + 16.dp`).
2. Jika elemen target berada di paruh bawah layar:
   - Tempatkan Tooltip Card tepat di atas elemen target (`targetRect.top - cardHeight - 16.dp`).
   - Mencegah tooltip card menutupi elemen yang sedang disorot.

---

## 17. SPESIFIKASI FITUR 14: Tata Letak Komik Terbaru Grid 2 Kolom (Total 8 Poster, Tanpa Slider)

Berdasarkan referensi visual `/storage/emulated/0/termux/design/reff/Screenshot_20261108_112400_Yuranime.jpg`:

### A. Rombak Struktur dari Slider Menjadi Grid Vertikal
1. Hapus `LazyRow` / slider horizontal pada `Komik Terbaru`.
2. Ambil tepat 8 judul komik terbaru (`comics.take(8)`).
3. Susun menjadi 4 baris ke bawah menggunakan `Column(verticalArrangement = Arrangement.spacedBy(10.dp))`.
4. Setiap baris berisi 2 kartu komik berdampingan dengan `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp))` di mana setiap kartu menggunakan `Modifier.weight(1f)`.

### B. Desain Kartu Komik Padat & Tanpa Overlay Belakang
1. **Sisi Kiri (Poster Komik):**
   - Lebar poster tetap `62.dp`, rasio `aspectRatio(2f/3f)`, sudut `RoundedCornerShape(6.dp)`, `ContentScale.Crop`.
   - **DILARANG menambahkan overlay warna atau gradasi gelap di belakang atau di atas poster komik!** Poster harus tampil bersih dan murni.
2. **Sisi Kanan (`Modifier.weight(1f)`):**
   - **Judul Komik:** Font **SF Bold** putih `12.sp`, `maxLines = 2`, `overflow = TextOverflow.Ellipsis`, line height `14.sp`.
   - **Bendera Negara:** Berukuran `16.dp x 11.dp` tepat di bawah judul (China untuk Manhua, Jepang untuk Manga, Korea untuk Manhwa).
   - **3 Chapter Terbaru (Rapat Presisi):** Jarak `4.dp` di bawah bendera, 3 baris chapter:
     - Kiri: Titik bulat oranye kecil `#FF5722` + `CH. [Nomor]` (font **SF Bold** putih `10.sp`).
     - Kanan: Waktu rilis singkat (`9.sp` warna abu-abu `#64748B`, `maxLines = 1`).

---

## 18. SPESIFIKASI FITUR 15: Perbaikan Gambar Kategori & Tahun di SearchScreen (Anti-Zoom Glitch)

### A. Analisis Masalah
- Ketika layar tidak di-scroll, Coil ImageRequest yang menggunakan `crossfade(true)` dan kalkulasi dimensi dinamis mengalami penyesuaian ukuran ulang saat recomposition/layout settling, menyebabkan gambar seolah "nge-zoom" atau melompat secara tiba-tiba.

### B. Solusi Kunci Dimensi & Decode Size
1. Hapus semua modifier skala dinamis atau penyesuaian ukuran yang terikat state scroll.
2. Konfigurasi `ImageRequest` pada `AsyncImage` di `WhiteCategoryCard`, `WhiteYearCard`, dan `DarkCharacterCard`:
   - `.size(coil.size.Size.ORIGINAL)`
   - `.crossfade(false)`
3. Kunci tinggi wadah kartu secara statis: `Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(12.dp))`.
4. Gunakan `contentScale = ContentScale.FillHeight` (dengan `Alignment.CenterEnd` untuk Kategori/Karakter dan `Alignment.CenterStart` untuk Tahun).
5. Gambar tidak akan pernah meloncat atau nge-zoom saat diam maupun saat di-scroll.

---

## 19. SPESIFIKASI FITUR 16: Rombak Total Floating Bottom Navbar (100% Mengambang di Atas Konten & Clear Liquid Glass AGSL)

Berdasarkan evaluasi `Screenshot_20261108_135755_Yuranime.jpg` dan referensi video `VID-20261108-WA0016.mp4` (`amarnath3003/quickLiquid`):

### A. Lepaskan dari `Scaffold(bottomBar)` Berlatar Hitam
1. **DILARANG** meletakkan `BottomNavBar` di dalam slot `Scaffold(bottomBar = ...)` yang memotong area bawah konten (`innerPadding.calculateBottomPadding()`), karena menyebabkan blok hitam pekat di bawah kapsul navbar.
2. Pada `HomeScreen`, `JadwalScreen`, `SearchScreen`, dan `LoginProfileScreen`, gunakan `Box(modifier = Modifier.fillMaxSize())`:
   - Konten utama (`LazyColumn` / `Column`) menggunakan `Modifier.fillMaxSize()` dengan `contentPadding = PaddingValues(bottom = 110.dp)` agar daftar konten mengalir 100% sampai ke tepi paling bawah layar tepat di belakang kapsul kaca navbar.
   - `BottomNavBar` diletakkan langsung di dalam root `Box` dengan `Alignment.BottomCenter` dan `padding(start = 24.dp, end = 24.dp, bottom = 20.dp).navigationBarsPadding()`.

### B. Efek Optik "Clear Liquid Glass" (Tanpa Warna Merah/Coklat Keruh)
1. **Hapus** warna merah/coklat keruh dan border merah pada tombol aktif.
2. **Real-Time Backdrop Capture & AGSL Refraction (Android 13+):**
   - Rekam layer konten halaman ke dalam `GraphicsLayer` (`rememberGraphicsLayer()`).
   - Di dalam kapsul `BottomNavBar`, gambar ulang layer konten di bawahnya melalui `RuntimeShader` AGSL yang menerapkan fisika optik `quickLiquid`:
     - **Clear Center:** Bagian tengah kapsul bening transparan.
     - **Refractive Edge (Lensing):** Tepian melengkung kapsul membiaskan piksel konten yang lewat di bawahnya menggunakan rumus pembiasan Snell dan kurva busur cembung, dilengkapi *chromatic dispersion* tipis pada kanal R/G/B.
     - **Fresnel Specular Rim:** Pantulan cahaya kaca putih tipis (`1.dp`) yang tajam di bibir lengkungan atas dan bawah kapsul.
3. **Kapsul Tab Aktif:**
   - Lensa kaca putih bening (`Color(0x28FFFFFF)` hingga `Color(0x12FFFFFF)`) dengan border kaca putih tipis (`1.dp`, `Color(0x73FFFFFF)` -> `Color(0x1FFFFFFF)`).
   - Menampilkan **Ikon Putih Solid + Teks Judul Putih SF Bold** (`Home`, `Jadwal`, `Cari`, `Chat`, `Profile`) dengan animasi pegas `animateContentSize`.
   - Tab tidak aktif hanya menampilkan **Ikon Outline** tanpa teks.

---

## 20. SPESIFIKASI FITUR 17: Perbaikan `Komik Terbaru` di Home (Tanpa Kotak Overlay, 10 Item, Fix Gambar & 3 Chapter Sejajar)

Berdasarkan evaluasi `Screenshot_20261108_135755_Yuranime.jpg`:

### A. Hapus Kotak / Card Overlay di Belakang Item Komik
1. **HAPUS** background kotak abu-abu (`#141418`) dan border (`#222632`) pada setiap item komik.
2. Poster di kiri dan informasi di kanan tampil bersih langsung di atas latar utama halaman (`#0B0B0B`).

### B. 10 Komik Terbaru (5 Baris x 2 Kolom Grid Vertikal)
1. Ambil **10 komik terbaru (`comics.take(10)`)** dan susun dalam **5 baris ke bawah x 2 kolom berdampingan** (`Modifier.weight(1f)`).

### C. Perbaikan Gambar Poster Komik (100% Muncul)
1. Pada `ComicRepository`, gunakan fallback otomatis: jika `cover_portrait_url` kosong (`""`), ambil `cover_image_url` dari respons API.
2. Tambahkan header HTTP pada setiap request gambar komik:
   - `"User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"`
   - `"Referer": "https://app.shinigami.asia/"`

### D. Kesejajaran Presisi 3 Chapter Terbaru
1. Kunci tinggi teks Judul Komik menjadi tepat 2 baris (`minLines = 2, maxLines = 2`) diikuti ikon bendera negara (`cn.webp` / `jp.webp` / `kr.webp`) sehingga awal baris chapter di kolom kiri dan kanan sejajar 100% rata horizontal.
2. Hapus pembatas tinggi statis yang memotong chapter ke-3. Pastikan selalu tampil 3 baris chapter utuh (`• CH. [Nomor]` di kiri dan waktu rilis singkat di kanan) tanpa titik oranye kosong.

---

## 21. SPESIFIKASI FITUR 18: Perbaikan Kartu `KATEGORI` & `TAHUN` di SearchScreen Tanpa Pembatas Warna

Berdasarkan evaluasi `Screenshot_20261108_145552_Yuranime.jpg`:

### A. Penyatuan Warna Latar Penuh dari Kiri ke Kanan
1. Gambar banner `KATEGORI` dan `TAHUN` memiliki latar warna terang solid di belakang karakter.
2. Ambil sampel warna piksel tepi gambar secara otomatis saat bitmap dimuat (`allowHardware(false)`):
   - Pada `WhiteCategoryCard` (karakter di kanan), ambil warna piksel dari sisi kiri gambar (`x = 5% lebar`, `y = 50% tinggi`).
   - Pada `WhiteYearCard` (karakter di kiri), ambil warna piksel dari sisi kanan gambar (`x = 95% lebar`, `y = 50% tinggi`).
3. Terapkan warna tersebut sebagai `background` penuh kartu (`Modifier.fillMaxWidth().height(92.dp)`) disertai *smooth horizontal gradient blend* dari warna latar kartu menuju gambar karakter.
4. Tampilkan teks kategori (`Genre` / `Theme` `#64748B` dan judul besar **SF Bold** `#1E293B`) atau angka tahun di atas latar yang menyatu penuh tanpa garis pembatas warna di tengah kartu.

---

## 22. SPESIFIKASI FITUR 19: Halaman Detail Komik (`ComicDetailScreen`) Sesuai Referensi `Vortex`

Berdasarkan 3 gambar referensi `Screenshot_20261108_145758_ANIMEIN.jpg`, `145804`, dan `145812`:

### A. Hero Cover Atas & Informasi Utama
1. Gambar Cover/Poster komik berukuran besar di bagian atas layar (`height = 430.dp`, edge-to-edge tembus status bar) dengan gradasi gelap halus di bagian bawah.
2. Pojok kiri atas: tombol panah kembali `<-`. Pojok kanan atas: tombol ikon hati outline `♡` (favorit).
3. Pojok kiri bawah di atas gambar cover:
   - **Judul Komik** ukuran besar (`26.sp`, **SF Bold** putih bersih).
   - Di bawah judul: `Row` berisi ikon mata + jumlah views (dari data scrape `view_count`), pemisah tipis `|`, dan ikon hati + jumlah suka/bookmark (dari data scrape `bookmark_count`).
   - **Tanpa** badge `Choice Mode`.

### B. Bar Tab Melengkung dengan 2 Tab Saja (`INFO` & `CHAPTER`)
1. Di bawah area cover, tampilkan header melengkung (*curved arc header*) dengan busur halus.
2. **Hanya tampilkan 2 tab: `INFO` dan `CHAPTER`** (tanpa tab `CAST` dan tanpa `Choice Mode`).
3. Indikator garis bawah berwarna oranye (`#FF5722`) pada tab yang sedang aktif.

### C. Tampilan Tab `INFO` (Menyesuaikan Data Scrape Komik)
1. **Bagian Atas (Poster Kiri + Info Kanan):**
   - Sisi Kiri: Poster Komik vertikal (`width = 125.dp`, `aspectRatio(2f/3f)`, `RoundedCornerShape(10.dp)`).
   - Sisi Kanan Poster (vertikal rata kiri):
     - Label abu-abu (`#94A3B8`, `12.sp`) `Title` -> teks putih **SF Bold** `17.sp` (judul komik).
     - Label abu-abu `Status` -> teks status berwarna **Oranye (`#FF5722`, SF Bold `16.sp`)** (`ongoing` / `completed`).
     - Label abu-abu `Type` -> tipe komik (`Manga` / `Manhwa` / `Manhua`) beserta bendera negara.
2. **Bagian Bawah Poster (Vertikal Rata Kiri Tanpa Kotak Border):**
   - Label abu-abu `Sinopsis` diikuti paragraf cerita komik lengkap (`#E2E8F0`, `14.sp`, line-height `21.sp`).
   - Label abu-abu `Kategori` -> daftar genre dipisahkan koma (putih **SF Bold** `15.sp`).
   - Label abu-abu `Aired Start` -> tanggal rilis (`created_at` / `release_year` dari scrape) atau `-`.
   - Label abu-abu `Aired End` -> tanggal tamat atau `-`.
   - Label abu-abu `Author` -> nama pengarang/artist dari `taxonomy` scrape (putih **SF Bold** `15.sp`).

### D. Tampilan Tab `CHAPTER` + Search Chapter Real-Time
1. **Kolom Search Chapter:** Di bagian atas daftar chapter (`Cari nomor atau judul chapter...`, latar `#14171F`, sudut `10.dp`, ikon kaca pembesar) yang memfilter daftar chapter secara real-time.
2. **Daftar Vertikal Chapter (Tanpa Gembok Kunci):**
   - Pemisah `HorizontalDivider` (`#1E222B`) antar item.
   - Sisi Kiri: Thumbnail persegi membulat (`54.dp x 54.dp`, `RoundedCornerShape(10.dp)`, `ContentScale.Crop` dengan fallback `yuranime-monochrome.svg`).
   - Bagian Tengah (3 baris vertikal rapat):
     1. Nomor chapter abu-abu (`#94A3B8`, SF SemiBold `12.sp`, contoh: `#01`, `#02`).
     2. Judul Chapter putih (**SF Bold** `15.sp`).
     3. Metadata bawah: Ikon kalender kecil + tanggal rilis (`24 Agu 26`) + ` • ` + ikon mata kecil + jumlah views (`584` dari `view_count` chapter) warna `#94A3B8` (`12.sp`).
   - Sisi Kanan: Ikon panah kanan `>` (langsung membuka halaman baca komik untuk chapter tersebut).

---

## 23. SPESIFIKASI FITUR 20: Kartu `Lanjut Nonton` (Watch History) Horizontal Cover di Home

1. Ubah tampilan `ContinueWatchingCard` di `HomeScreen` dari poster vertikal (`2:3`) menjadi **Cover Horizontal (`16:9`, lebar `185.dp`, sudut `8.dp`)**.
2. Simpan dan gunakan gambar cover horizontal (`getEffectiveCover()` / thumbnail episode) saat merekam riwayat tontonan di `StreamingScreen` dan `HomeViewModel`.

---

## 24. SPESIFIKASI FITUR 21: Isolasi Header Domain Gambar (Fix Gambar AnimeIn 100%) & On/Off Efek Liquid Glass Navbar (Auto Sesuai Versi Android)

### A. Isolasi Header Interceptor Berdasarkan Domain/Host pada Coil ImageLoader
1. **Penyebab Bug:**
   - CDN AnimeIn (`api.animein.net`, `xyz-api.animein.net`) dilindungi Cloudflare WAF. Jika request aset gambar mengirim `User-Agent: Mozilla/5.0...` (browser UA) atau header `x-proxy-secret` tanpa validasi JS challenge, Cloudflare menganggapnya sebagai bot dan memblokir dengan `HTTP 403 Forbidden` (`cf-mitigated: challenge`).
   - Selain itu, header `Referer` komik (`https://app.shinigami.asia/`) dan proxy lama `cf1.dinzid.my.id` yang tidak aktif menyebabkan CDN menolak request (`HTTP 403 / 530`).
2. **Solusi Interceptor Domain-Isolated:**
   - Di dalam `YuranimeApp.kt` (`newImageLoader()`):
     - Jika `host` berasal dari domain komik (`shngm`, `shinigami`, `komik`): kirim header `Referer: https://app.shinigami.asia/` dan `User-Agent` browser Chrome Android.
     - Jika `host` berasal dari domain AnimeIn atau domain lainnya: hapus header `Referer` (`removeHeader("Referer")`), hapus `x-proxy-secret`, dan gunakan User-Agent standar OkHttp (`okhttp/4.12.0`) sehingga Cloudflare WAF meloloskan request gambar secara native tanpa challenge (200 OK).
   - Inisialisasi Coil ImageLoader global (`Coil.setImageLoader(this)`) pada saat `Application.onCreate()`, serta gunakan cache disk baru `image_cache_v3` untuk membersihkan cache 403 sebelumnya.
3. **Normalisasi URL Gambar AnimeIn & Fallback Cerdas:**
   - Hapus pembungkus proxy mati `cf1.dinzid.my.id/?url=`.
   - Upgrade otomatis protokol `http://` ke `https://` untuk mencegah redirect 301.
   - Normalisasi URL relatif (`//`, `/`, atau tanpa protokol) ke `https://xyz-api.animein.net/` dan bersihkan duplikasi slash pada path.
   - Hindari konflik alias `@SerializedName` pada model Gson (seperti duplicate key `poster` pada `AnimeItem` yang dapat memicu `IllegalArgumentException`) dengan memisahkan deklarasi field secara bersih (`imagePoster`, `imageCover`, `poster`) dan mengandalkan fallback cerdas di level Kotlin (`getEffectivePoster()` dan `getEffectiveCover()` menggunakan `.takeIf { it.isNotBlank() }`).
   - Hasil: Seluruh gambar AnimeIn (Hero Slider, Cuplix, Episode Baru, Sedang Hangat, Jat For Yu, Paling Dinanti, Jadwal, Detail Anime) DAN Komik muncul 100% secara bersamaan tanpa error deserialization "Gagal Memuat Konten".

### B. Fitur On/Off Efek "Liquid Glass" pada Floating Bottom Navbar
1. **Default Berdasarkan Versi Android (`Build.VERSION.SDK_INT`):**
   - **Android 13 ke atas (API 33+ / TIRAMISU):** Default **ON (`true`)**.
   - **Android 12 ke bawah (API 32 ke bawah / <= S_V2):** Default **OFF (`false`)**.
   - Preferensi disimpan persisten di `AppPreferences` (`KEY_LIQUID_GLASS = "key_liquid_glass_navbar"`).
2. **Tampilan Saat ON vs OFF (Tata Letak Identik 100%):**
   - Bentuk kapsul mengambang (*floating pill*), ukuran, padding bawah (`navigationBarsPadding() + 20.dp`), dan animasi tab aktif/inaktif tetap sama persis.
   - **Saat ON (`isLiquidGlassEnabled == true`):**
     - Efek optik *Liquid Glass* penuh: AGSL `RuntimeShader` pembiasan *live backdrop* Snell refraction, *chromatic dispersion* tepi lensa (cyan/warm), dan *Fresnel specular rim* `1.dp`.
     - Kapsul tab aktif menggunakan lensa kaca putih bening dengan border gradasi kaca.
   - **Saat OFF (`isLiquidGlassEnabled == false`):**
     - Matikan komputasi shader/pembiasan dan rekam backdrop layar untuk menghemat daya GPU.
     - Kapsul luar menggunakan permukaan gelap semi-transparan bersih (`#D912141A`) dengan border tipis elegan (`#33FFFFFF`).
     - Kapsul tab aktif menggunakan permukaan semi-transparan ringan (`#26FFFFFF`) dengan border tipis (`#33FFFFFF`), tetap menampilkan Ikon Putih Solid + Teks Judul Putih SF Bold.
3. **Toggle Switch di `SettingsScreen`:**
   - Opsi switch *"Efek Liquid Glass Navbar"* di bagian `NAVIGASI & TAMPILAN` dengan deskripsi *"Aktifkan efek kaca cair pada navigasi bawah (Otomatis aktif di Android 13+)"* yang bereaksi real-time tanpa perlu restart aplikasi.
