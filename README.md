# Traverse Android

**Traverse** is a companion Android app for the [LeetFeedback](https://leet-feedback.vercel.app/) Chrome
extension that helps competitive programmers track their progress across
online judges and get AI-powered insights into their problem solving.

Whether you're grinding LeetCode, TUF, GFG, or any similar platform, Traverse
lets you collect solve data, monitor streaks, and even use an optional Gemini
backend for intelligent feedback.

> 🔗 You’ll want the Chrome extension installed first:
> https://chromewebstore.google.com/detail/traverse/nnapafjmoelkehjedfgjchoeelgbiama
> 
> 📖 Full usage documentation is available at:
> https://leet-feedback.vercel.app/guide

---

## Getting Started

1. **Install the LeetFeedback Chrome extension** and sign up with a username and password.  The extension watches your activity on
   supported coding sites and sends it to your personal account.
2. **Install the Traverse Android app** from [Github](https://github.com/lqSky7/traverse-android/releases), then log in using the *same username* you registered in the
   extension.
3. Start solving problems on LeetCode, TUF, GFG, etc.  The extension will
   automatically sync your attempts with Traverse; we never ask for or store your site credentials.
4. (Optional) Configure a Gemini API key in the extension to unlock
   AI-based analysis and hints.  The key is stored locally in the extension
   and is not collected by our servers.

## Highlights

* **Secure and private.** All authentication is handled via anonymous
  tokens—you retain full control over your data.
* **Insightful stats.** View your solve history, streaks, rankings, and more
  directly in the app.
* **Cat-powered profile images.** First login fetches a cute random cat image!
* **Offline cache.** Data persists locally so you can browse even without a
  network connection.
* **Open source.** Feel free to dig through the code and contribute.

---

For more information, visit the LeetFeedback documentation or explore the
source code. Happy coding! 🚀


---

## CI/CD Setup

This repository includes a GitHub Actions workflow that automatically builds and releases APKs when you push tags.

### Setting up GitHub Secrets

To enable automatic APK signing and releases, you need to configure the following secrets in your GitHub repository:

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Add the following secrets:

#### Required Secrets:

- **`SIGNING_KEY`**: Base64-encoded keystore file
  ```bash
  # Generate keystore (if you don't have one)
  keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
  
  # Convert to base64
  base64 my-release-key.jks | tr -d '\n' > keystore.txt
  # Copy the content of keystore.txt to SIGNING_KEY secret
  ```

- **`ALIAS`**: Your keystore alias (e.g., `my-key-alias`)

- **`KEY_STORE_PASSWORD`**: Password for the keystore

- **`KEY_PASSWORD`**: Password for the key alias

### Creating a Release

To trigger a build and create a release:

```bash
# Tag your commit
git tag -a v1.5.0 -m "Release version 1.5.0"

# Push the tag
git push origin v1.5.0
```

The workflow will:
1. Build the release APK
2. Sign it with your keystore
3. Create a GitHub release with the APK attached
4. Generate release notes automatically

### Manual Build

You can also download unsigned APKs from any push to master/main:
1. Go to **Actions** tab in your repository
2. Click on the latest workflow run
3. Download the `traverse-android-apk` artifact

---

## New Features in v1.5

### 🔐 Password Reset
- Complete password recovery flow with email verification
- Multi-step UI with animated gradients
- Accessible from login screen

### 📱 QR Code System
- Generate QR codes for your profile
- Scan QR codes to add friends instantly
- Share QR codes via any app
- Camera permission handling

### 📊 ML Analytics Dashboard
- Comprehensive revision analytics
- Stability distribution visualization
- Accuracy trends over time
- Projected workload forecasting
- Retention heatmap for problem tracking
- Performance metrics and streaks

### 🤝 Friend Streaks (Backend Ready)
- Track shared streaks with friends
- Send and accept streak requests
- Maintain streaks by solving together

### ❄️ Freeze Shop
- Purchase streak freezes with XP
- Automatic freeze usage on missed days
- Gift freezes to friends

---

## Development

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 26+

### Building Locally

```bash
# Clone the repository
git clone https://github.com/lqSky7/traverse-android.git
cd traverse-android

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Architecture
- **MVVM** pattern with Jetpack Compose
- **Retrofit** for networking
- **Kotlin Coroutines** for async operations
- **StateFlow** for reactive state management
- **Material 3** design system
- **CameraX** for QR scanning
- **ML Kit** for barcode detection

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes with signed commits (`git commit -s -S -m "Add amazing feature"`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is open source. See the LICENSE file for details.
