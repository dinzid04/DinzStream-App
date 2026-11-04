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
- **R-02 (Copywriting):** Tidak ada karakter em dash (`—`) pada teks UI. Menggunakan tanda titik dua (`:`), koma (`,`), atau kurung `()`.
- **R-03 & R-39 (Mobile Interaction & Touch Targets):** Semua tombol dan item interaktif memiliki tinggi tap target minimum 44dp dan respons visual klik instan.
- **R-17 & R-36 (Real Data):** Semua data XP, level, poin, dan profil berasal langsung dari Firestore `users/{uid}`, bukan angka dummy yang dibuat-buat.
- **R-24 & R-26 (Interactive Completeness):** Setiap menu di profil memiliki layar tujuan yang aktif dan berfungsi nyata.
- **R-27 (UI States):** Tersedia *loading shimmer/spinner*, *empty state*, dan *error recovery* pada semua pembacaan data Firebase.
