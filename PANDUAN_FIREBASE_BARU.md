# 📘 Panduan Lengkap Pembuatan & Konfigurasi Firebase Baru (Android App + Web Admin)

Dokumen ini berisi panduan langkah-demi-langkah dari nol untuk membuat project **Google Firebase baru** yang terintegrasi penuh dengan:
1. **Aplikasi Android Yuranime (`com.yuranime.dinz`)** — untuk Login Akun Google, Sinkronisasi Profil/Leaderboard, dan Notifikasi Popup Promosi.
2. **Website Admin Panel (`promo-web-admin`)** — untuk mengontrol popup promosi secara real-time dari Vercel/browser.

---

## 🔍 Perbedaan Mendasar: Firebase Web vs Firebase Android App

| Fitur | Firebase Versi Web | Firebase Versi Android App |
| :--- | :--- | :--- |
| **Identitas Proyek** | Hanya butuh script JS (`apiKey`, `projectId`) | Membutuhkan **Package Name** (`com.yuranime.dinz`) & **Sertifikat SHA-1** |
| **File Konfigurasi** | Ditaruh langsung di kode HTML/JS | Berupa file resmi **`google-services.json`** di folder `app/` |
| **Login Google** | Menggunakan Popup Web Browser | Terintegrasi langsung dengan akun Google di HP Android via Google Play Services |
| **Keamanan** | Berdasarkan Domain Terdaftar (Authorized Domains) | Berdasarkan fingerprint tanda tangan keystore Android (SHA-1) |

> [!IMPORTANT]
> **1 Project Firebase untuk 2 Platform:**  
> Anda **TIDAK PERLU** membuat 2 project Firebase terpisah. Cukup **1 Project Firebase**, lalu di dalamnya kita daftarkan **1 Aplikasi Android** DAN **1 Aplikasi Web**. Keduanya akan berbagi database Cloud Firestore yang sama secara real-time!

---

## 🚀 LANGKAH 1: Buat Project Firebase Baru di Console

1. Buka browser di HP / PC Anda dan masuk ke **[Firebase Console](https://console.firebase.google.com/)** menggunakan akun Google aktif Anda.
2. Klik tombol **"Add project"** (atau **"Tambahkan project"**).
3. Masukkan nama project, misalnya: **`yuranime-app`** atau **`yuranime-official`**.
4. Klik **Continue**.
5. Pada pilihan *Google Analytics*:
   - Anda boleh mematikannya (*Disable*) agar proses pembuatan lebih instan, atau biarkan menyala (*Enable*) jika ingin statistik analitik.
6. Klik **"Create project"** dan tunggu beberapa detik hingga muncul keterangan *"Your new project is ready"*, lalu klik **Continue**.

---

## 📱 LANGKAH 2: Daftarkan Aplikasi Android (`com.yuranime.dinz`)

Setelah masuk ke dashboard project baru Anda:

1. Di halaman utama dashboard (bagian *"Get started by adding Firebase to your app"*), klik ikon **Android** (robot hijau).
2. Isi formulir pendaftaran aplikasi Android:
   - **Android package name (WAJIB TEPAT):**
     ```text
     com.yuranime.dinz
     ```
   - **App nickname (Opsional):**
     ```text
     Yuranime Android
     ```
   - **Debug signing certificate SHA-1 (SANGAT PENTING untuk Google Sign-In):**
     Salin dan tempelkan fingerprint SHA-1 keystore Android Yuranime berikut:
     ```text
     24:68:75:90:8B:8B:0E:89:2C:53:A3:13:B5:BF:08:D2:DE:89:79:32
     ```
3. Klik tombol biru **"Register app"**.
4. Pada langkah ke-2 (*Download config file*), klik tombol biru **"Download google-services.json"**.
5. Simpan file tersebut. File ini bernama **`google-services.json`**.
6. Klik **Next** ➔ **Next** ➔ **Continue to console**.

### 📂 Cara Menaruh File `google-services.json` Baru ke Project Termux:
Pindahkan file `google-services.json` yang baru saja Anda download ke direktori aplikasi:
```bash
cp /storage/emulated/0/Download/google-services.json /storage/emulated/0/termux/Workspace_uiApp/app/google-services.json
```
*(Pastikan file menimpa file `google-services.json` yang lama di folder `app/`)*.

---

## 🔐 LANGKAH 3: Aktifkan Fitur Login (Authentication)

Agar pengguna bisa login dengan akun Google dan guest session di aplikasi:

1. Pada menu navigasi sebelah kiri, buka **Build** ➔ **Authentication**.
2. Klik tombol **"Get started"**.
3. Pada tab **Sign-in method**, aktifkan 2 provider berikut:
   - **A. Provider Google:**
     1. Klik baris **Google**.
     2. Geser tombol toggle **Enable**.
     3. Pada kolom **Project support email**, pilih alamat email Google Anda.
     4. Klik **Save**.
   - **B. Provider Anonymous (Tamu):**
     1. Klik tombol **Add new provider**.
     2. Pilih **Anonymous**.
     3. Geser tombol toggle **Enable**.
     4. Klik **Save**. *(Provider ini penting agar web admin dan guest mode dapat menyinkronkan data tanpa kendala)*.

---

## 🗄️ LANGKAH 4: Buat Database Cloud Firestore

Database ini menyimpan profil pengguna, riwayat baca, leaderboard, dan notifikasi promo:

1. Pada menu sebelah kiri, buka **Build** ➔ **Firestore Database**.
2. Klik tombol **"Create database"**.
3. **Database ID:** Biarkan `(default)`.
4. **Location:** Pilih region terdekat dengan pengguna Indonesia, misalnya:
   - `asia-southeast2` (Jakarta) atau
   - `asia-southeast1` (Singapore).
5. Pada pilihan *Secure rules for Cloud Firestore*:
   - Pilih opsi **Start in test mode**.
6. Klik **Create** dan tunggu proses inisialisasi selesai.
7. Setelah database terbuka, masuk ke tab **Rules** di bagian atas.
8. Ganti seluruh isi aturan dengan kode aturan resmi berikut agar tidak terjadi error izin akses (*permission denied*):
   ```javascript
   rules_version = '2';

   service cloud.firestore {
     match /databases/{database}/documents {
       // Izinkan pembacaan & penulisan konfigurasi promo popup dan data pengguna
       match /{document=**} {
         allow read, write: if true;
       }
     }
   }
   ```
9. Klik tombol biru **Publish** di pojok kanan atas.

---

## 🔔 LANGKAH 5: Konfigurasi Sistem Notifikasi & Promo Popup

Sistem notifikasi di Yuranime memiliki 2 lapis:

### 1. In-App Promo Popup Dialog (Notifikasi Poster Layar Penuh Dinamis)
- Menggunakan dokumen Firestore: `app_config/promo_popup`.
- Anda tidak perlu membuat dokumen ini secara manual di Firebase Console!
- Begitu Anda mengisi gambar poster dan menekan **"Simpan & Terapkan"** dari Website Admin Panel, dokumen ini akan dibuat dan disinkronkan secara otomatis.

### 2. Push Notification Bar HP (Firebase Cloud Messaging / FCM)
- Buka menu **Build** ➔ **Messaging** (atau **Engage** ➔ **Messaging**).
- Jika baru pertama kali, klik **Create your first campaign** ➔ pilih **Firebase Notification messages**.
- Anda dapat mengirim notifikasi judul, teks, dan gambar langsung ke seluruh pengguna yang telah memasang aplikasi Yuranime di HP mereka!

---

## 🌐 LANGKAH 6: Hubungkan Web Admin Panel (`promo-web-admin`)

Agar Website Admin Vercel Anda dapat mengontrol promo di project Firebase baru ini:

1. Di Firebase Console, klik ikon gerigi ⚙️ **Project settings** (di pojok kiri atas samping menu Project Overview).
2. Scroll ke bagian paling bawah ke subjudul **"Your apps"**.
3. Klik tombol **"Add app"** lalu pilih ikon **Web** (`</>`).
4. Masukkan nama aplikasi web: `Yuranime Web Admin`.
5. *(Centang Firebase Hosting tidak diperlukan)* ➔ Klik **Register app**.
6. Anda akan melihat kode konfigurasi seperti ini:
   ```javascript
   const firebaseConfig = {
     apiKey: "AIzaSy...",
     authDomain: "yuranime-official.firebaseapp.com",
     projectId: "yuranime-official",
     storageBucket: "yuranime-official.firebasestorage.app",
     messagingSenderId: "...",
     appId: "1:...:web:..."
   };
   ```
7. Salin seluruh teks objek konfigurasi tersebut.

### 🔗 Cara Menerapkan ke Web Admin Vercel (Tanpa Perlu Coding):
1. Buka Web Admin Anda: **[https://yuranime-promo-admin.vercel.app](https://yuranime-promo-admin.vercel.app)**
2. Klik tombol **`⚙️ Akun / Database Firebase`** di pojok kanan atas.
3. Tempelkan (*paste*) kode `firebaseConfig` baru Anda ke dalam kotak textarea yang tersedia.
4. Klik tombol **"💾 Terapkan & Hubungkan Project Baru"**.
5. Web Admin akan memuat ulang dan langsung terhubung secara real-time ke project Firebase baru Anda!

---

## 📦 LANGKAH 7: Simpan & Build APK Baru di GitHub Actions

Setelah file `app/google-services.json` diganti dengan yang baru, commit dan push ke GitHub agar otomatis di-build menjadi `.apk` rilis:

```bash
cd /storage/emulated/0/termux/Workspace_uiApp

# 1. Cek status perubahan file
git status

# 2. Tambahkan google-services.json baru
git add app/google-services.json

# 3. Commit dan Push ke GitHub
git commit -m "feat(firebase): integrate new firebase project google-services.json"
git push origin main
```

GitHub Actions akan otomatis menjalankan proses kompilasi build APK terbaru dan merilis file `.apk` siap pasang di tab **Releases** repositori Anda.

---

## 📋 Checklist Ringkas Verifikasi Berhasil

- [ ] Project Firebase baru sudah terbuat di [console.firebase.google.com](https://console.firebase.google.com).
- [ ] Aplikasi Android terdaftar dengan nama paket `com.yuranime.dinz` & SHA-1 `24:68:75:90:8B:8B:0E:89:2C:53:A3:13:B5:BF:08:D2:DE:89:79:32`.
- [ ] File `google-services.json` baru sudah ditaruh di folder `app/`.
- [ ] Authentication Google & Anonymous sudah dalam status **Enabled**.
- [ ] Firestore Database sudah dibuat dan Rules sudah diset `allow read, write: if true;`.
- [ ] Web Admin di [yuranime-promo-admin.vercel.app](https://yuranime-promo-admin.vercel.app) sudah ditempel `firebaseConfig` baru dan berstatus **Online**.
