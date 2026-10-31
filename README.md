# AnimeIn - Native Android (Jetpack Compose)

Aplikasi streaming anime dan video pendek Cuplix native Android yang dibangun menggunakan **Kotlin**, **Jetpack Compose**, **Material 3**, serta mematuhi aturan kebersihan desain dari `.agents/skills/anti-slop/SKILL.md` dan [`DESIGN.md`](./DESIGN.md).

---

## Fitur & Arsitektur Layar

### 1. Halaman Home
* **Top Promo Banner:** Banner horizontal `16:9` / `21:9` dengan sudut membulat modern (`rounded-xl`).
* **Section Lanjut Nonton:** Riwayat tontonan dengan tombol `>` dan progress bar pemutaran.
* **Section Cuplix:** Avatar baris lingkaran dengan border ring tipis oranye `#FF5722` untuk cuplikan video pendek.
* **Section Episode Baru:** Kartu vertikal `2:3` dengan teks nomor episode oranye, judul putih tebal, metrik views (merah), dan favorites (kuning emas).
* **Section Sedang Hangat:** Banner horizontal `16:9` dengan thumbnail vertikal di kiri, teks genre oranye, judul putih, serta metrik views & favorites.
* **Section Judul Baru:** Katalog rilisan judul baru vertikal `2:3`.
* **Section Jadwal Hari Ini:** Kartu anime dengan badge jam tayang ungu `#A855F7`.
* **Section Jat For Yu:** Rekomendasi anime kurasi dalam format kartu landscape `16:9`.
* **Section Paling Dinanti:** Rilisan mendatang dengan badge tanggal rilis biru `#3B82F6`.
* **Section Paling Populer:** Anime terpopuler dengan metrik views & favorites.
* **Section Baca Manre:** Katalog komik / manhwa dengan statistik pembaca merah.
* **Bottom Navigation Bar:** 5 Tab presisi (`Home`, `Jadwal`, `Cari`, `Chat`, `Profil`) dengan touch target minimal 48dp.

### 2. Halaman Detail Anime (Anime Detail Screen)
* **Hero Cover Header:** Cover landscape di atas dengan gradasi hitam halus, tombol Back, pill status `ONGOING`, pill tipe `SERIES`, tombol bintang favorit, tombol share, serta tombol Play episode terbaru.
* **Scrollable TabBar (8 Tab):**
  1. `INFO`: Poster 2:3, judul utama, judul alternatif, sinopsis lengkap, tipe, tanggal rilis, studio, badge kategori genre, dan FAB edit.
  2. `EPISODE`: Grid 3 kolom dengan pita merah `NEW` dan placeholder logo serigala AnimeIn (`ic_wolf_logo`).
  3. `SEASON`: Daftar vertikal kartu 16:9 untuk setiap season, OVA, atau kolaborasi terkait.
  4. `DISCUSSION`: Diskusi komunitas pengguna dengan timestamp dan avatar.
  5. `CUPLIX`: Grid 3 kolom video cuplikan pendek khusus anime tersebut dengan indikator jumlah suka (`♡`).
  6. `POSTER`: Galeri poster vertikal kiriman pengguna dengan poin `P` dan upvote `▲`.
  7. `COVER`: Galeri cover landscape kiriman komunitas pengguna dengan info kontributor.
  8. `CONTRIBUTOR`: Papan peringkat kontributor dengan total poin, penayangan, dan suka.
