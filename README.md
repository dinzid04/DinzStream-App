# AnimeIn — Native Android (Jetpack Compose)

Aplikasi streaming anime dan video pendek Cuplix native Android yang dibangun menggunakan **Kotlin**, **Jetpack Compose**, **Material 3**, serta mematuhi aturan kebersihan desain dari `.agents/skills/anti-slop/SKILL.md` dan [`DESIGN.md`](./DESIGN.md).

---

## 🎨 Fitur & Struktur Halaman Home
1. **Top Promo Banner:** Banner horizontal `16:9` / `21:9` dengan sudut membulat modern (`rounded-xl`).
2. **Section Lanjut Nonton:** Riwayat tontonan dengan tombol `>` dan progress bar merah.
3. **Section Cuplix:** Avatar baris lingkaran dengan border ring tipis oranye `#FF5722` untuk cuplikan video pendek.
4. **Section Episode Baru:** Kartu vertikal `2:3` dengan teks nomor episode oranye, judul putih tebal, metrik views (merah), dan favorites (kuning emas).
5. **Section Sedang Hangat:** Banner horizontal `16:9` dengan thumbnail vertikal di kiri, teks genre oranye, judul putih, serta metrik views & favorites.
6. **Section Judul Baru:** Katalog rilisan judul baru vertikal `2:3`.
7. **Section Jadwal Hari Ini:** Kartu anime dengan badge jam tayang ungu `#A855F7`.
8. **Section Jat For Yu:** Rekomendasi anime kurasi dalam format kartu landscape `16:9`.
9. **Section Paling Dinanti:** Rilisan mendatang dengan badge tanggal rilis biru `#3B82F6`.
10. **Section Paling Populer:** Anime terpopuler dengan metrik jutaan views.
11. **Section Baca Manre:** Katalog komik / manhwa dengan statistik pembaca merah.
12. **Bottom Navigation Bar:** 5 Tab presisi (`Home`, `Jadwal`, `Cari`, `Chat`, `Profil`) dengan touch target >= 48dp.

---

## 🚀 Cara Push ke GitHub & Otomatisasi Build APK

Repositori lokal sudah siap dan ter-commit di folder:
`/storage/emulated/0/termux/Workspace_uiApp`

Untuk mengunggah ke GitHub dan memicu pembuatan APK otomatis di GitHub Releases:

1. Buat repositori baru di akun GitHub Anda (misal: `animein-android`).
2. Jalankan perintah berikut di terminal:
   ```bash
   cd /storage/emulated/0/termux/Workspace_uiApp
   git remote add origin https://github.com/<USERNAME>/<REPO_NAME>.git
   git push -u origin main
   ```
3. Buka tab **Actions** di repositori GitHub Anda.
4. Workflow **Build Android APK & Release** akan berjalan otomatis mengompilasi APK dan mempublikasikannya ke tab **Releases**!
