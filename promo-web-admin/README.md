# Yuranime Admin Console (Promo Popup & Kode Redeem Manager)

Website Admin Panel untuk mengatur sistem **Popup Notifikasi Promosi Dinamis (`PromoPopupDialog`)** dan **Manajemen Kode Redeem Hadiah (`redeem_codes`)** aplikasi mobile **Yuranime (`com.yuranime.dinz`)** secara real-time melalui **Firebase Firestore (`dinz-62119`)**.

🌐 **Live Vercel Production URL:** [https://yuranime-promo-admin.vercel.app](https://yuranime-promo-admin.vercel.app)

Didesain dengan standar **Apple Dark Glassmorphism / Linear Style (Anti-Slop)**:
- Background Obsidian Dark `#090A0F`, Card Glass `#12141C`, Hairline border `1px solid rgba(255, 255, 255, 0.08)`.
- Menggunakan tipografi SF Pro Display / Inter berkelas, tanpa template gradien warna-warni murah khas AI.
- Dua Tab Navigasi Utama:
  1. 📢 **Popup Promosi:** Form kontrol interval, batasan tayang, live smartphone mockup skala 1:1.
  2. 🎁 **Kelola Kode Redeem:** Form pembuat kode hadiah kustom, preset kosmetik (Border & Profile Effect), pesan sukses, dan live explorer database `redeem_codes`.

---

## 🚀 Fitur Tab 1: Popup Promosi (`app_config/promo_popup`)

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

## 🎁 Fitur Tab 2: Kelola Kode Redeem (`redeem_codes`)

1. **Form Buat / Edit Kode Redeem:**
   - **Kode Redeem (Auto Uppercase):** Input kode unik (contoh: `VIP2026`, `DINZMYTHIC`) atau tombol `⚡ Buat Kode Acak` (`YURA-XXXX`).
   - **Hadiah EXP & Poin:** Masukkan jumlah EXP untuk naikin level dan Poin (Primo) untuk belanja di Shop.
   - **Hadiah Custom Title:** Berikan gelar eksklusif langsung ter-unlock di akun pengguna (contoh: `MYTHIC SOVEREIGN`, `SEPUH YURANIME`).
   - **Hadiah Avatar Decoration / Border:** Pilihan cepat ID border (contoh: `border_sovereign_gold`, `border_silver_wings`, `border_venom_logo`, `border_mythic_emperor`, `border_bronze_crest`).
   - **Hadiah Profile Effect:** Pilihan cepat ID effect animasi (contoh: `effect_venom_symbiote`, `effect_mythic_aurora`, `effect_magic_hearts_gold`, `effect_cosmic_twilight`).
   - **Pesan Notifikasi:** Kustomisasi pesan pop-up saat user berhasil mengklaim kode di aplikasi.
   - **Status Aktif/Nonaktif:** Matikan sementara kode tanpa menghapusnya dari database.
2. **Template Paket Cepat (1-Klik):**
   - 🎉 Weekend Festival (+5.000 EXP, +2.500 Poin)
   - 👑 VIP Special Pass (+10.000 EXP, +5.000 Poin, VIP Title & Gold Border)
   - 🕷️ Venom Symbiote (+12.000 EXP, +6.000 Poin, Venom Border & Effect)
   - ✨ Mythic Sovereign (+25.000 EXP, +15.000 Poin, Title & Aurora Effect)
3. **Live Database Explorer:**
   - Menampilkan seluruh kode di koleksi Firestore `redeem_codes` secara real-time via `onSnapshot` & REST fallback.
   - Kolom pencarian instan dan filter chip (Semua / Aktif / Nonaktif).
   - Tombol 1-klik salin kode ke clipboard.
   - Tombol 1-klik toggle Aktif / Nonaktif langsung ke Firestore.
   - Tombol Edit (otomatis mengisi form dan scroll ke atas).
   - Tombol Hapus dokumen dari Firestore dengan konfirmasi aman.
   - **⚡ Seed 6 Kode Resmi:** Tombol untuk langsung mendaftarkan 6 kode bawaan resmi Yuranime (`YURANIMEVIP`, `DINZMYTHIC`, `SEPUH2026`, `SOVEREIGN99`, `ANIMEKING`, `VENOMSYMBIOTE`) ke database Firestore jika database masih kosong!

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

## 🔒 Konfigurasi Firebase Firestore
- **Project ID:** `dinz-62119`
- **Database ID:** `(default)`
- **Collections:**
  - `app_config/promo_popup`: Konfigurasi poster promosi.
  - `redeem_codes/{code}`: Koleksi kode redeem dengan field:
    - `active: Boolean`
    - `exp: Long / Integer`
    - `points: Long / Integer`
    - `title: String?`
    - `borderId: String?`
    - `effectId: String?`
    - `message: String`
    - `updatedAt: Long`
