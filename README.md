# Biliar Overlay (Android)

Overlay garis bidik transparan di atas aplikasi lain. Tombol ◎ memindai layar (sudut meja, bola putih, bola 8/9),
lalu garis bidik + pantulan digambar tepat di atas meja asli.

## Cara membangun
1. Pasang Android Studio (Koala atau lebih baru). File > Open > pilih folder ini, tunggu Gradle sync.
2. Sambungkan HP (USB debugging aktif), tekan Run. Atau Build > Build APK(s).
3. Buka aplikasi, tekan Mulai, izinkan "Tampil di atas aplikasi lain", lalu izinkan tangkap layar.
4. Buka game biliar, tekan ◎ (bisa digeser). Tahan lama untuk berhenti.

Minimal Android 8 (API 26). Logika deteksi dan fisika ada di app/src/main/assets/index.html.
