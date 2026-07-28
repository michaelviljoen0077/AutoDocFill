# AutoDocFill

Android native app for automated PDF form filling with offline-first, privacy-focused design.

## Features

- **Profile Vault**: Securely store personal information locally with SQLCipher encryption
- **PDF Processing**: Upload and process fillable AcroForm PDFs and scanned documents
- **OCR Support**: Extract text from scanned PDFs using ML Kit (on-device)
- **Autofill Engine**: Intelligent field mapping with confidence scoring
- **Validation**: Built-in validation for email, phone, dates, and ID formats
- **Signature Capture**: Draw signatures on touch screen
- **Audit Logging**: Comprehensive append-only audit trail
- **Biometric Auth**: Fingerprint/face authentication for sensitive operations

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room + SQLCipher (encrypted)
- **PDF**: iText 7 Community
- **OCR**: ML Kit Text Recognition (on-device)
- **Security**: Android Keystore, EncryptedSharedPreferences, BiometricPrompt

## Privacy & Security

- ✅ All data stored locally on device
- ✅ SQLCipher database encryption at rest
- ✅ No external API calls or data transmission
- ✅ Biometric authentication support
- ✅ Sensitive fields require manual approval

## Build Instructions

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 26+ (target SDK 34)

### Build Steps

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator (min API 26)

```bash
./gradlew assembleDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

## Project Structure

```
app/
├── data/
│   ├── local/          # Room database, DAOs, converters
│   └── model/          # Data entities
├── domain/
│   ├── pdf/            # PDF processing, OCR, autofill
│   ├── repository/     # Data repositories
│   ├── security/       # Biometric auth
│   └── signature/      # Signature management
├── presentation/
│   ├── document/       # Document screens & ViewModels
│   ├── profile/        # Profile screens & ViewModels
│   ├── signature/      # Signature canvas
│   ├── navigation/     # Navigation setup
│   └── theme/          # Material Design theme
└── di/                 # Hilt modules
```

## MVP Roadmap

### Phase 1: Foundation (Weeks 1-2) ✅
- [x] Project setup with dependencies
- [x] Room database with SQLCipher encryption
- [x] Profile Vault data models and UI
- [x] PDF upload and storage

### Phase 2: Autofill (Weeks 3-4) ✅
- [x] PDF field detection (AcroForm)
- [x] Autofill engine with validation
- [x] Field mapping with confidence scores
- [x] Review/edit UI

### Phase 3: Signature & Export (Weeks 5-6) ✅
- [x] Signature capture canvas
- [x] Signature placement on PDF
- [x] PDF export functionality
- [x] Document management UI

### Phase 4: OCR & Polish (Weeks 7-8) ✅
- [x] ML Kit OCR integration
- [x] Audit logging system
- [x] Biometric authentication
- [x] Navigation and theming

## Future Enhancements

- [ ] Scanned PDF signature detection with computer vision
- [ ] Cloud backup (optional, user-controlled)
- [ ] Multiple language support
- [ ] Document templates library
- [ ] Batch processing
- [ ] Advanced field detection with ML
- [ ] Wear OS companion app

## License

MIT License - See LICENSE file for details

## Contributing

Contributions welcome! Please open an issue first to discuss proposed changes.

## Support

For issues or questions, please open a GitHub issue.
