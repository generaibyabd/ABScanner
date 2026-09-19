# DECISIONS.md — ABScanner Architecture & Design Records

## Milestone & Architecture Plan
1. **Module & Package Architecture**:
   - Single application module `:app` with clean package separation mirroring `:codec` logic:
     - `com.abdeveloper.abscanner.codec`: pure Kotlin parsers, validators, formatters, symbologies, check-digits, and URL risk analyzer.
     - `com.abdeveloper.abscanner.data`: Room database (entities, DAO, repository), DataStore preferences.
     - `com.abdeveloper.abscanner.camera`: CameraX analysis, ML Kit bundled scanning + ZXing fallback, CoordinateTransform mapping, Torch, Zoom, Auto-zoom.
     - `com.abdeveloper.abscanner.generator`: ZXing QR/barcode generator, custom BitMatrix styler (module shapes: square, rounded, dots; colors; logo overlay; ECC level L/M/Q/H), decode verification.
     - `com.abdeveloper.abscanner.ui`: Jetpack Compose UI, Material 3 theme (#3785D2 brand accent), NavigationRail (>=600dp) / NavigationBar (<600dp), 3 tabs: Scan, Create, Settings.
     - `com.abdeveloper.abscanner.tile`: Quick Settings `TileService` to quickly launch scanner.
   - Application ID & Namespace: `com.abdeveloper.abscanner`.

2. **Decisions & Deviations**:
   - **DI Strategy**: Uses robust Constructor Injection with Android `ViewModelProvider.Factory` and an `AppContainer` singleton rather than heavy external DI annotation processors, eliminating classpath friction and ensuring fast compilation.
   - **Offline-First Rule**: ML Kit bundled model (`com.google.mlkit:barcode-scanning:17.3.0`) + ZXing Core (`3.5.3`) operates completely without network dependencies.
   - **Permissions**: Only `CAMERA` runtime permission requested strictly on the Scan screen with explanatory rationale. No location, storage, or external tracking permissions.
   - **Photo Import**: Uses Modern zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) falling back gracefully to `GetContent`.
   - **Localization**: Supports 10 languages (English, Chinese, Hindi, Spanish, Arabic with RTL support, French, Bengali, Portuguese, Russian, Indonesian) with instant in-app locale switching via `AppCompatDelegate.setApplicationLocales`.
   - **Security**: URL risk analyzer checks IP address hosts, non-https schemes, homograph/suspicious chars, URL shortener domains, and phishing TLDs. Sensitive fields (Wi-Fi password, OTP) masked by default with reveal toggle.
