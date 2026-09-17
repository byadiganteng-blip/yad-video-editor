# 👑 Setup Admin

## Cara Set Admin User

Admin **TIDAK** dibuat otomatis. Harus di-set manual via Firestore:

1. Login sebagai admin di aplikasi (email + password)
2. Copy UID dari Firebase Console -> Authentication -> Users
3. Buka Firestore -> Collection `admins` -> Add document
4. Document ID: `<UID>` (paste UID tadi)
5. Field: `email` = `ynuraini686@gmail.com`
6. Field: `isAdmin` = `true`
7. Save

## Password Admin

Password disimpan sebagai **SHA-256 hash** di `SecureConfig.kt`.

Default password: `YADIGANTENG`

Untuk ganti password:

```bash
echo -n "PASSWORD_BARU" | sha256sum
```

Lalu replace `ADMIN_PASS_HASH` di `SecureConfig.kt`.

## Keamanan

- Jangan commit password ke repo
- Ganti password default sebelum release
- Batasi akses Firestore `admins` collection
