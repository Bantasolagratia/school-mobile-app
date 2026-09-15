# Sekolah Mobile App (Android & iOS)

Aplikasi mobile resmi portal sekolah berbasis **Kotlin Multiplatform & Compose Multiplatform (Material 3)**. Aplikasi ini dirancang untuk berjalan pada platform **Android** dan **iOS** dengan tampilan responsif yang adaptif untuk **Smartphone** maupun **Tablet / iPad**.

Aplikasi terintegrasi dengan ekosistem backend:
- **Auth Service (GoTrue port 8000)**: Autentikasi JWT & sesi login siswa.
- **Backend API (Spring Boot port 8080)**: Data profil siswa (`/auth-flow/profile`) dan Direktori Guru (`/management/guru`).

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa**: Kotlin `2.2.10`
- **UI Toolkit**: Compose Multiplatform `1.7.3` & Jetpack Compose Material 3
- **Runtime**: Java 17+ (kompatibel Java 25 & Gradle 9.5.0)
- **Target Android**: `compileSdk = 35`, `minSdk = 24`, `targetSdk = 35`
- **Target iOS**: Framework multiplatform `SharedApp` (iOS X64, Arm64, Simulator Arm64) + SwiftUI runner di `iosApp`
- **Networking**: Ktor Client `2.3.12` (OkHttp engine untuk Android, Darwin engine untuk iOS) & Kotlinx Serialization `1.7.3`
- **Arsitektur**: MVVM (Model-View-ViewModel) dengan StateFlow & Coroutines

### Struktur Modul Proyek
```text
├── app/          # Modul Aplikasi Android (Runner utama di Android Studio)
├── shared/       # Modul Kotlin Multiplatform (Logic, Repositories, Ktor Client, dan UI Compose)
│   ├── commonMain/   # UI Screens (Adaptive Phone/Tab), ViewModels, Models, Repositories
│   ├── androidMain/  # Implementasi spesifik Android (SharedPreferences, Context)
│   ├── iosMain/      # Implementasi spesifik iOS (NSUserDefaults, MainViewController)
│   └── commonTest/   # Unit Test serialisasi dan model
├── iosApp/       # Proyek Xcode & SwiftUI Runner untuk iOS (iPhone & iPad)
└── gradle/       # Gradle Version Catalog (libs.versions.toml) & Wrapper (Gradle 9.5.0)
```

---

## 📱 Fitur & Alur Bisnis

1. **Autentikasi Siswa (GoTrue port 8000)**:
   - Endpoint: `POST /token?grant_type=password`
   - Manajemen sesi aman dan auto-login tersimpan di penyimpanan lokal persisten (`SharedPreferences` di Android, `NSUserDefaults` di iOS).
2. **Profil Siswa & Validasi Peran (Spring Boot port 8080)**:
   - Endpoint: `GET /auth-flow/profile`
   - Mendeteksi peran `ROLE_MURID` / `isStudent`, menampilkan Nama Siswa, Nomor Induk Siswa (NIS), dan Rombel / Kelas (`Kelas 10-C`).
3. **Direktori Guru / Modul Guru**:
   - Endpoint: `GET /management/guru`
   - Pencarian real-time berdasarkan Nama, NIP, atau Mata Pelajaran / Jabatan.
   - Avatar inisial otomatis.
   - Modal detail kontak pengajar dengan tombol salin nomor Telepon dan WhatsApp langsung ke papan klip (*clipboard*).
4. **Desain Adaptif (Scalable Phone & Tablet)**:
   - **Smartphone**: Menggunakan *Bottom Navigation Bar* (`Beranda`, `Guru`, `Profil`).
   - **Tablet / iPad**: Menggunakan *Navigation Rail* vertikal di sisi kiri serta tata letak multi-kolom (*2-column grid*).
5. **Konfigurasi Host Server Dinamis**:
   - Dialog pengaturan host server langsung diakses dari layar login.
   - Default: `10.0.2.2` untuk Android Emulator, `localhost` untuk iOS Simulator, atau IP LAN Wi-Fi (misal: `192.168.18.94`) untuk testing di HP fisik.

---

## 🧪 Akun Uji Coba Pengembang

Tersedia akun murid terverifikasi pada backend lokal:
- **Email**: `wrenley@murid.sekolah.com`
- **Kata Sandi**: `Password123!`
- **Nama Siswa**: Wrenley Roth
- **Kelas**: Kelas 10-C (NIS: 202610012)

---

## 🚀 Cara Menjalankan

### Android
Buka direktori ini di **Android Studio**. Konfigurasi `:app` sudah terdaftar sebagai target utama. Cukup pilih emulator atau perangkat Android dan klik tombol **Run**.

Atau via command line:
```bash
./gradlew :app:assembleDebug
```

### iOS
Buka folder `iosApp` pada **Xcode**:
```bash
open iosApp/iosApp.xcodeproj
```
Pilih simulator iPhone atau iPad, lalu jalankan (**Cmd + R**).

