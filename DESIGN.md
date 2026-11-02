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

## 6. Kepatuhan Standar Anti-Slop (Delivery Gate Checklist)

- **R-01 & R-29 (Color Craft):** Tidak ada gradasi default biru-ungu AI generik. Warna emas dan kosmik ungu dirancang khusus sesuai identitas referensi podium.
- **R-02 (Copywriting):** Tidak ada karakter em dash (`—`) pada teks UI. Menggunakan tanda titik dua (`:`), koma (`,`), atau kurung `()`.
- **R-03 & R-39 (Mobile Interaction & Touch Targets):** Semua tombol dan item interaktif memiliki tinggi tap target minimum 44dp dan respons visual klik instan.
- **R-17 & R-36 (Real Data):** Semua data XP, level, poin, dan profil berasal langsung dari Firestore `users/{uid}`, bukan angka dummy yang dibuat-buat.
- **R-24 & R-26 (Interactive Completeness):** Setiap menu di profil memiliki layar tujuan yang aktif dan berfungsi nyata.
- **R-27 (UI States):** Tersedia *loading shimmer/spinner*, *empty state*, dan *error recovery* pada semua pembacaan data Firebase.
