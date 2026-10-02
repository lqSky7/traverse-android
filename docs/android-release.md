# Android release signing

Android release builds require a persistent signing keystore. CI will not create a temporary key or fall back to the debug key.

For GitHub Actions, add these repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded keystore file
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

For GitLab CI, add the same values as protected CI/CD variables. The Android release pipelines run on `v*` tags after unit tests and lint pass.

Locally, set `ANDROID_RELEASE_KEYSTORE_PATH` and the three password/alias variables in the shell environment before running `./gradlew assembleRelease`.

## Current key

Generated 2026-10-02. PKCS12, RSA 2048, 10,000-day validity, alias `traverse`.

Certificate fingerprint (SHA-256):

```
DB:76:6F:E8:10:F4:76:0B:17:54:36:67:F3:7D:90:65:E9:7B:26:5D:72:56:88:49:84:4D:8D:B2:BE:E5:75:FD
```

The fingerprint is public — it is embedded in every signed APK — so it belongs in the repo as a way to
verify that a given build came from this key. The keystore itself and its passwords are **not** in the
repo and must never be. `*.jks` and `*.keystore` are already in `.gitignore`; keep the working copy
outside the repository anyway, so there is no path by which it can be committed.

## On signature continuity

Every release up to and including `v1.5-build.52` was signed with a throwaway key, so no installed
build has ever been upgradeable over the previous one — a signature mismatch was already the normal
case, and the earlier releases have since been removed.

**From this key onward that changes.** Android refuses to install an APK whose signature differs from
the installed one, so replacing this keystore later means existing users must uninstall first and lose
their local data. Back the keystore and its passwords up somewhere durable and offline; losing them is
equivalent to breaking every future upgrade.

## Rotating the key

1. Back up the current keystore and passwords first — they are the only way to sign an upgrade.
2. Generate a replacement (any JDK's `keytool`):

   ```
   keytool -genkeypair -keystore traverse-release.keystore -alias traverse \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

3. Update all four secrets in **both** GitHub and GitLab, since the two pipelines read the same names.
4. Update the fingerprint above.
5. Accept that the next release cannot be installed over the current one.

The native apps use `https://traverses.tech/api/` as their API base URL, matching the website's same-origin `/api` route.

