plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

val releaseKeystorePath = providers.gradleProperty("ANDROID_RELEASE_KEYSTORE_PATH")
    .orElse(providers.environmentVariable("ANDROID_RELEASE_KEYSTORE_PATH")).orNull
val releaseStorePassword = providers.gradleProperty("ANDROID_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("ANDROID_RELEASE_STORE_PASSWORD")).orNull
val releaseKeyAlias = providers.gradleProperty("ANDROID_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("ANDROID_RELEASE_KEY_ALIAS")).orNull
val releaseKeyPassword = providers.gradleProperty("ANDROID_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("ANDROID_RELEASE_KEY_PASSWORD")).orNull
val releaseKeystoreFile = releaseKeystorePath?.let { rootProject.file(it) }
val releaseSigningConfigured = !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank() &&
    releaseKeystoreFile?.isFile == true

android {
    namespace = "com.traverse.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.traverse.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystoreFile != null) storeFile = releaseKeystoreFile
            storePassword = releaseStorePassword.orEmpty()
            keyAlias = releaseKeyAlias.orEmpty()
            keyPassword = releaseKeyPassword.orEmpty()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        check(releaseSigningConfigured) {
            "Release signing is required. Set ANDROID_RELEASE_KEYSTORE_PATH, " +
                "ANDROID_RELEASE_STORE_PASSWORD, ANDROID_RELEASE_KEY_ALIAS, and " +
                "ANDROID_RELEASE_KEY_PASSWORD."
        }
    }
}

tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease") {
        dependsOn(validateReleaseSigning)
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Security (for encrypted token storage)
    implementation(libs.androidx.security.crypto)

    // Custom Tabs — GitHub sign-in opens in the reader's own browser so an
    // existing GitHub session is reused, and the deep link can't be hijacked.
    implementation(libs.androidx.browser)

    // Image Loading
    implementation(libs.coil.compose)

    // Video/Media
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // QR Code
    implementation(libs.zxing.core)
    implementation(libs.mlkit.barcode.scanning)

    // Camera
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
