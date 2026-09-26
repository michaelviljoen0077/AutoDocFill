# AutoDocFill

Android app that fills in PDF forms from a locally stored profile. Offline-first and privacy-focused: your profile and documents stay on the device.

## Features

- **Profile vault**: store personal, contact, address, employment and ID details (plus custom fields) in an encrypted database. Supports multiple profiles; the *active* one is used for autofill.
- **PDF import**: pick any PDF through the system file picker; it is copied into app-private storage.
- **Field detection**: reads AcroForm fields (text, checkbox, radio, dropdown, signature) from fillable PDFs. Scanned PDFs are run through on-device OCR (ML Kit) to find labels like `Name: ____`.
- **Autofill**: maps field names to profile values with a confidence score, validates emails, phone numbers, dates, postal codes and ID numbers, and never overwrites values you edited by hand.
- **Manual edit**: review, search and correct every field value.
- **Export & sign**: write the values into a copy of the PDF, optionally stamp a hand-drawn signature (placed on the form's signature field if it has one), then save or share it via the Android share sheet.
- **History**: an audit log of uploads, autofills, edits, exports and signatures.

### Known limitations

- Scanned (non-fillable) PDFs can be analysed, but values can't be written back into them yet, so they can't be exported.
- `BiometricAuthManager` exists but is not wired into any screen yet.

## Privacy & security

- Profiles and documents are stored only on the device; the app never uploads them. OCR runs on-device.
- The Room database is encrypted with SQLCipher. The passphrase is generated with `SecureRandom` and kept in `EncryptedSharedPreferences` (Android Keystore).
- Cloud backup and device-to-device transfer are disabled so personal data can't leave the device through backups.
- Sensitive values (ID/passport/licence/tax numbers, date of birth) are flagged for review and are not copied into the audit log.

## Tech stack

Kotlin · Jetpack Compose + Material 3 · MVVM · Hilt · Room + SQLCipher · iText 7 · ML Kit Text Recognition · Coroutines/Flow

## Building

Requirements: JDK 17 and the Android SDK (compileSdk 34, minSdk 26). Android Studio Hedgehog or newer works out of the box.

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run unit tests
./gradlew assembleRelease      # release build (needs a signing config)
```

CI (`.github/workflows/android.yml`) builds the debug APK and runs the unit tests on every push and pull request.

Room schemas are exported to `app/schemas/` on build; commit them so future database migrations can be written and tested. The database currently uses `fallbackToDestructiveMigration()`, which wipes data on a schema change — replace it with real migrations before shipping.

## Project structure

```
app/src/main/java/com/autodocfill/app/
├── data/
│   ├── local/          # Room database, DAOs, type converters
│   └── model/          # Entities: Profile, Document, FieldMapping, AuditLog
├── di/                 # Hilt modules (encrypted database)
├── domain/
│   ├── pdf/            # Field detection, matching, validation, autofill, OCR, PDF writing
│   ├── repository/     # Repositories over the DAOs
│   ├── security/       # Biometric auth
│   └── signature/      # Signature storage and placement on PDFs
└── presentation/
    ├── document/       # Document list, manual edit
    ├── history/        # Audit log
    ├── navigation/     # Bottom navigation
    ├── profile/        # Profile list and editor
    ├── signature/      # Signature drawing canvas
    └── theme/          # Material 3 theme
```

## License

MIT — see [LICENSE](LICENSE).
