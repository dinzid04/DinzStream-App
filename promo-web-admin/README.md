# Yuranime Promo Web Admin Console

Website Admin Panel untuk mengatur sistem **Popup Notifikasi Promosi Dinamis (`PromoPopupDialog`)** aplikasi mobile **Yuranime (`com.yuranime.dinz`)** secara real-time melalui **Firebase Firestore (`app_config/promo_popup`)**.

Didesain dengan standar **Apple Dark Glassmorphism / Linear Style (Anti-Slop)**:
- Background Obsidian Dark `#090A0F`, Card Glass `#12141C`, Hairline border `1px solid rgba(255, 255, 255, 0.08)`.
- Menggunakan tipografi SF Pro Display / Inter berkelas, tanpa template gradien warna-warni murah khas AI.
- Layout 2 Kolom Responsif: Form Kontrol Konfigurasi di sisi kiri dan Live Interactive Smartphone Mockup di sisi kanan.

---

## 🚀 Fitur Admin Panel

1. **iOS-Style Toggle Switch:** Aktifkan atau nonaktifkan popup promosi di semua HP pengguna secara real-time.
2. **Poster Image URL:** Masukkan link langsung poster promosi dengan live thumbnail verification dan tombol preset contoh.
3. **Target Redirect URL (Opsional):** Atur link tujuan jika poster diklik (atau kosongkan jika poster hanya bersifat gambar informatif tanpa link).
4. **Interval Muncul (Jam):** Pilihan cepat `8 Jam [Default]`, `1 Jam`, `12 Jam`, `24 Jam`, `Sekali Sebulan`, atau input angka kustom.
5. **Batas Maksimal Muncul per User:**
   - `0` = Tanpa batas (terus berulang setiap interval waktu jam).
   - `1` = Hanya muncul 1x seumur hidup promo.
   - `3`, `5`, dst. = Muncul maksimal sekian kali.
6. **Aksi Penyimpanan Real-Time:**
   - **Simpan & Terapkan ke Aplikasi:** Menyimpan pengaturan saat ini ke dokumen Firestore `app_config/promo_popup`.
   - **Push sebagai Promo Baru (Reset Cooldown):** Membuat `promoId` baru berbasis timestamp sehingga seluruh pengguna langsung mendapatkan popup saat membuka aplikasi kembali tanpa harus menunggu 8 jam.
7. **Interactive Smartphone Mockup:** Menampilkan preview 1:1 persis bagaimana poster promosi, tombol close bulat `✕` di kanan atas luar gambar, dan latar redup (78% black) tampil di aplikasi Yuranime.

---

## 💻 Cara Menjalankan Secara Lokal (Local Ready)

### Opsi 1: Menggunakan Node / Vite (Termux / PC)
```bash
cd /storage/emulated/0/termux/Workspace_uiApp/promo-web-admin/
npm install
npm run dev
```
Buka browser di `http://localhost:3000`.

### Opsi 2: Menggunakan Python HTTP Server (Tanpa npm install)
```bash
cd /storage/emulated/0/termux/Workspace_uiApp/promo-web-admin/
python3 -m http.server 3000
```
Buka browser di `http://localhost:3000`.

### Opsi 3: Buka Langsung File di Browser
Cukup buka file `index.html` langsung di browser Chrome / Firefox / Safari Anda. Aplikasi menggunakan Firebase Modular SDK via CDN ESM sehingga langsung berfungsi tanpa build step!

---

## ☁️ Cara Deploy ke Vercel (1-Click Vercel Ready)

Project ini sudah dilengkapi konfigurasi resmi `vercel.json`:

### Opsi 1: Deploy via Vercel CLI
```bash
cd /storage/emulated/0/termux/Workspace_uiApp/promo-web-admin/
npx vercel
```
Ikuti instruksi di layar dan web admin panel Anda akan langsung live dalam beberapa detik!

### Opsi 2: Deploy via Vercel Web Dashboard (GitHub Import)
1. Buka [https://vercel.com/new](https://vercel.com/new).
2. Hubungkan repositori GitHub `dinzid04/DinzStream-App`.
3. Pada bagian **Root Directory**, pilih folder `promo-web-admin`.
4. Klik **Deploy**!

---

## 🔒 Konfigurasi Firebase Firestore
- **Project ID:** `dinz-ai-gemini-copy`
- **Database ID:** `ai-studio-eff2e4c3-4396-44bb-b0c9-0936b4c631a4` (atau `(default)`)
- **Document Path:** `app_config/promo_popup`
- **Fields:**
  - `isActive: Boolean`
  - `promoId: String`
  - `imageUrl: String`
  - `targetUrl: String`
  - `intervalHours: Float`
  - `maxImpressions: Int`
  - `updatedAt: Long`
