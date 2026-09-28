# Android release signing

Android release builds require a persistent signing keystore. CI will not create a temporary key or fall back to the debug key.

For GitHub Actions, add these repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded keystore file
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

For GitLab CI, add the same values as protected CI/CD variables. The Android release pipelines run on `v*` tags after unit tests and lint pass. Keep the same keystore for future releases so installed builds can be updated.

Locally, set `ANDROID_RELEASE_KEYSTORE_PATH` and the three password/alias variables in the shell environment before running `./gradlew assembleRelease`.

The native apps use `https://traverses.tech/api/` as their API base URL, matching the website's same-origin `/api` route.
