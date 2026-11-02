# Yuranime - Tempat Nonton Anime Subtitle Indonesia

![Yuranime Banner](https://raw.githubusercontent.com/dinzid04/DinzStream-App/main/app/src/main/res/drawable/poster_login_01.jpg)

> Nikmati pengalaman streaming anime terbaik dan terlengkap dengan kualitas tinggi, bebas hambatan, serta antarmuka modern yang nyaman bagi para penikmat anime Indonesia.

---

## Pembaruan Terbaru Versi 1.9.2

Aplikasi **Yuranime** kini hadir dengan pembaruan besar yang meningkatkan kenyamanan, estetika, dan stabilitas:

### 1. Daftar Episode Horizontal & Search Inline di Halaman Streaming
- **Header Ergonomis:** Teks judul EPISODE dengan jumlah total episode (`• N Ep`) dan input pencarian ringkas (`Cari ep..`).
- **Pencarian Cepat & Auto-Scroll:** Mengetik nomor episode langsung memfilter dan menggulir daftar ke nomor tersebut.
- **Adaptive Range Selector:** Pemilih rentang (`1 - 100`, `101 - 200`, dst.) otomatis muncul untuk anime panjang (> 50 episode) dan disembunyikan untuk anime pendek.
- **LazyRow Modern (Anti-Slop):** Tombol episode proporsional (tinggi 44dp, sudut membulat 10dp). Episode yang sedang ditonton disorot warna Crimson/Oranye dengan ikon playing dan digulir otomatis ke layar pandang.
- **Pergantian Episode Mulus:** Memuat stream dan memperbarui info episode seketika saat nomor episode ditekan.

---

### 2. Strict SemVer Update Checker (Bebas False-Positive)
- **Sinkronisasi Hulu ke Hilir:** Nilai `versionName` di Gradle disinkronkan dengan tag rilis GitHub Actions.
- **Perbandingan Numerik Ketat:** Membandingkan versi segmen demi segmen (`remote > local`). Popup update tidak akan muncul jika versi sama atau saat gagal memuat API.

---

### 3. In-App Update Notification Popup & Profile Screen
- **Header Landscape Anime:** Menampilkan banner anime dengan gradasi gelap menyatu ke latar utama.
- **Avatar & Badge:** Foto profil bulat ber-border oranye dengan lencana kamera biru untuk kustomisasi instan.
- **Akses Komunitas:** Deretan tombol kapsul media sosial resmi (**WhatsApp**, **Telegram**, **Instagram**, **TikTok**, dan **Discord**).
- **Menu Pilihan Lengkap:**
  - **Edit Profil:** Kustomisasi nama pengguna, foto avatar, dan cover banner.
  - **Unduh:** Kelola daftar anime yang tersimpan secara offline.
  - **Papan Peringkat:** Pantau ranking penonton bulanan (**TOP 100**).
  - **Points & Rewards:** Check-in harian dan kumpulkan koin reward (**★ 60**).
  - **Info Cerita Kami:** Informasi pengembang dan tombol cek pembaruan aplikasi.
  - **Keluar / Logout:** Keluar akun dengan aman.

---

## Tautan Resmi & Komunitas

- **Download APK Terbaru:** [Unduh di GitHub Releases](https://github.com/dinzid04/DinzStream-App/releases)
- **Repositori Proyek:** [GitHub dinzid04/DinzStream-App](https://github.com/dinzid04/DinzStream-App)
- **Website Resmi:** [Kunjungi Portal Yuranime](https://github.com/dinzid04/DinzStream-App)
