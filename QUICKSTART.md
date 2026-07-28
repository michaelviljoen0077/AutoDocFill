# Quick Start Guide - AutoDocFill

## Prerequisites
- Windows PC with Android Studio installed
- JDK 17 or higher
- Android SDK (API 26+)
- Android device or emulator

## Setup Steps

### 1. Open in Android Studio
```
File → Open → Navigate to C:\Users\USER-PC\AutoDocFill
```

### 2. Sync Gradle
- Android Studio will automatically prompt to sync
- Click "Sync Now" in the notification bar
- Wait for dependencies to download (~5-10 minutes first time)

### 3. Configure Emulator (if not using physical device)
```
Tools → Device Manager → Create Device
- Select: Pixel 6 or similar
- System Image: API 33 or 34 (download if needed)
- Click Finish
```

### 4. Build the App
```
Build → Make Project (Ctrl+F9)
```

### 5. Run the App
```
Run → Run 'app' (Shift+F10)
```

## First Launch

1. **Grant Permissions**: App will request storage and camera permissions
2. **Create Profile**: 
   - Tap Profile tab
   - Tap + button
   - Fill in your details
   - Tap Save
3. **Upload PDF**:
   - Tap Documents tab
   - Tap + button
   - Select a fillable PDF form
   - Wait for processing (2-30 seconds)
4. **Autofill**:
   - Tap "Autofill" button on processed document
   - Review filled fields
   - Edit any incorrect values
5. **Export**:
   - Tap "Export" button
   - PDF saved to internal storage

## Test PDFs

You can test with:
- Job application forms (PDF)
- Government forms (IRS, DMV)
- Lease agreements
- Insurance claim forms

Search online for "fillable PDF form sample" to find test documents.

## Troubleshooting

### Gradle Sync Issues
```
File → Invalidate Caches → Invalidate and Restart
```

### Missing Dependencies
```
Tools → SDK Manager → SDK Tools
- Ensure Android SDK Build-Tools installed
- Ensure Android SDK Platform-Tools installed
```

### App Crashes on Launch
- Check logcat for errors
- Ensure minimum API 26 (Android 8.0)
- Grant all required permissions

### PDF Not Processing
- Ensure PDF is fillable (AcroForm)
- Check file permissions
- Try smaller PDF first (<5MB)

## Build Release APK

```
Build → Generate Signed Bundle / APK
- Select APK
- Create new keystore (for first time)
- Enter keystore details
- Select "release" build variant
- Finish
```

APK will be in: `app/release/app-release.apk`

## Development Tips

### Enable Debug Logging
Add to `AutoDocFillApplication.kt`:
```kotlin
override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    }
}
```

### Database Inspection
```
View → Tool Windows → App Inspection
Select "Database Inspector"
```

### Compose Preview
- Open any Screen.kt file
- Look for @Preview annotations
- Click "Split" or "Design" view

## Next Steps

1. **Test thoroughly** with various PDF types
2. **Add error handling** for edge cases
3. **Implement unit tests** for core logic
4. **Optimize performance** for large PDFs
5. **Add analytics** (optional, privacy-preserving)
6. **Submit to Play Store** when ready

## Support

- Check logs: `View → Tool Windows → Logcat`
- Review code: Navigate through package structure
- Test features: Use emulator or physical device

## Success Criteria

✅ App builds without errors
✅ App launches on device/emulator  
✅ Profile can be created and saved
✅ PDF can be uploaded
✅ Fields are detected
✅ Autofill completes
✅ PDF can be exported

If all above work, your app is ready for testing! 🎉
