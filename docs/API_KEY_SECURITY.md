# API Key Security

API key di google-services.json normal untuk Android app (harus di-commit),
tapi WAJIB dibatasi agar tidak disalahgunakan.

## Langkah:

1. Buka https://console.cloud.google.com/apis/credentials
2. Pilih project yad-video-editor
3. Klik API key: AIzaSyBpEKuyMVvgep1C71MQXKq_sI4rH_kkOdo
4. Application restrictions -> Android apps
5. Add item:
   - Package name: com.yad.videoeditor
   - SHA-1: BD:37:C5:AC:97:E8:8F:0F:1E:0D:85:29:AF:AD:CD:AB:CB:14:80:33
6. Add item lagi:
   - Package name: com.yad.videoeditor.admin
   - SHA-1: BD:37:C5:AC:97:E8:8F:0F:1E:0D:85:29:AF:AD:CD:AB:CB:14:80:33
7. API restrictions -> Restrict key
8. Centang Firebase + Firestore + FCM
9. Save

Setelah ini, API key hanya bisa dipakai dari APK dengan signature cocok.