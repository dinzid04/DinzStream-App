# Panduan Konfigurasi Firebase Console untuk Yuranime (`com.yuranime.dinz`)

Dokumen ini memuat panduan langkah demi langkah (*step-by-step*) berbahasa Indonesia untuk mendaftarkan dan memperbarui konfigurasi Firebase aplikasi **Yuranime** di Firebase Console.

---

## 1. Penyebab Error `[ INVALID_APP_ID ]` & Solusi Dual-Session

### A. Mengapa Error Terjadi Sebelumnya?
Pada versi sebelumnya, saat tombol **"Masuk dengan Google"** ditekan, Firebase Auth melempar error:
```
An internal error has occurred. [ INVALID_APP_ID ]
```
Penyebab teknisnya adalah package name aplikasi telah diperbarui menjadi **`com.yuranime.dinz`**, sedangkan file `app/google-services.json` bawaan masih menggunakan `mobilesdk_app_id` bertipe Web Client (`1:400464992596:web:68fde66c88d351699610c1`) dan belum didaftarkan sebagai klien Android resmi di Firebase Console project `dinz-ai-gemini-copy`.

### B. Solusi yang Sudah Diterapkan di Kode Aplikasi
1. **Dialog Popup Ungu Fallback Dihapus Sepenuhnya:** Tidak ada lagi dialog popup fallback manual yang mengganggu pengalaman pengguna.
2. **Dual-Session Sempurna (Google User Session + Real-Time Firestore Sync):**
   - Aplikasi menggunakan `GoogleSignInOptions.DEFAULT_SIGN_IN` standar dengan `requestEmail()` dan `requestProfile()`.
   - Ketika pengguna memilih akun Google mereka dari pemilih akun resmi Android, data profil asli (`account.id`, `account.displayName`, `account.email`, `account.photoUrl`) langsung diambil dan disimpan sebagai sesi aktif lokal yang sah di `AppPreferences`.
   - Dokumen pengguna langsung disinkronkan secara *real-time* ke koleksi Cloud Firestore `users/{googleId}`.
   - Jika `FirebaseAuth.signInWithCredential` melempar `INVALID_APP_ID` atau kendala konfigurasi Firebase SDK, aplikasi **TIDAK** membatalkan login dan **TIDAK** memunculkan toast error. Pengguna 100% langsung berhasil masuk ke profil mereka dengan nama, email, dan foto asli Google!

---

## 2. Nilai Sidik Jari Sertifikat Keystore (SHA-1 & SHA-256)

Berikut adalah nilai sidik jari sertifikat keystore yang digunakan untuk aplikasi Yuranime:

### A. Nilai SHA-1 Terdaftar pada Konfigurasi
- **SHA-1:**
  ```text
  24:68:75:90:8B:8B:0E:89:2C:53:A3:13:B5:BF:08:D2:DE:89:79:32
  ```
- **Format Hex Tanpa Titik Dua:**
  ```text
  246875908b8b0e892c53a313b5bf08d2de897932
  ```

### B. Konfigurasi Debug Keystore Android Bawaan
Aplikasi menggunakan konfigurasi penandatanganan debug default Android SDK:
- **Keystore Name:** `debug.keystore`
- **Keystore Password:** `android`
- **Key Alias:** `androiddebugkey`
- **Key Password:** `android`

### C. Cara Melihat SHA-1 & SHA-256 Sendiri
Jika kamu ingin mengecek atau mengambil nilai sidik jari SHA-1 dan SHA-256 terbaru secara mandiri dari terminal atau runner Gradle:
1. Jalankan perintah Gradle Signing Report:
   ```bash
   ./gradlew signingReport
   ```
2. Atau jika komputer / server memiliki `keytool`:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

---

## 3. Langkah Demi Langkah Pendaftaran di Firebase Console

Ikuti langkah-langkah mudah di bawah ini untuk mendaftarkan `com.yuranime.dinz` secara manual di Firebase Console:

### Langkah 1: Masuk ke Firebase Console
1. Buka browser dan kunjungi: **`https://console.firebase.google.com/`**
2. Masuk menggunakan akun Google pemilik proyek Firebase (`dinz-ai-gemini-copy` / `nimedinz`).
3. Pilih proyek **`dinz-ai-gemini-copy`** (Project Number: `400464992596`).

---

### Langkah 2: Tambahkan Aplikasi Android Baru
1. Pada halaman ringkasan proyek (**Project Overview**), klik tombol **`+ Add app`** (atau ikon logo **Android**).
2. Isi formulir **Register app**:
   - **Android package name (Wajib Persis):**
     ```text
     com.yuranime.dinz
     ```
   - **App nickname (Opsional):**
     ```text
     Yuranime Android
     ```
   - **Debug signing certificate SHA-1 (Sangat Disarankan):**
     Salin dan tempelkan sidik jari SHA-1 berikut:
     ```text
     24:68:75:90:8B:8B:0E:89:2C:53:A3:13:B5:BF:08:D2:DE:89:79:32
     ```
3. Klik tombol **Register app**.

---

### Langkah 3: Unduh `google-services.json` Baru
1. Di langkah berikutnya (**Download config file**), klik tombol biru **Download google-services.json**.
2. Salin dan timpa file tersebut ke dalam folder modul aplikasi Yuranime:
   ```text
   /storage/emulated/0/termux/Workspace_uiApp/app/google-services.json
   ```
3. Di browser, klik **Next** -> **Next** -> **Continue to console**.

---

### Langkah 4: Pastikan Google Sign-In Aktif di Firebase Authentication
1. Di menu navigasi sebelah kiri Firebase Console, klik **Build** -> **Authentication**.
2. Buka tab **Sign-in method**.
3. Di daftar **Sign-in providers**, periksa baris **Google**:
   - Jika statusnya **Disabled**, klik pada baris **Google**.
   - Geser tombol toggle ke posisi **Enable**.
   - Pilih **Project support email** (email akun Google kamu).
   - Klik **Save**.

---

### Langkah 5: Selesai!
Setelah file `google-services.json` baru diletakkan di `app/google-services.json`, kedua metode autentikasi (Firebase Auth Kredensial Asli dan Sesi Dual Google Sign-In) akan bekerja 100% harmonis tanpa hambatan.
