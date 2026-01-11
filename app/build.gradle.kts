plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.termux"
    compileSdk = 34

    defaultConfig {
        // CRITICAL: MUST be com.termux for binary RUNPATH compatibility
        // Termux binaries have hardcoded RUNPATH /data/data/com.termux/files/usr/lib
        // App appears as "MobileCLI Games" to users but uses same package
        // This means MobileCLI and MobileCLI Games CANNOT be installed simultaneously
        // (installing Games replaces MobileCLI, but they share the same bootstrap/binaries)
        applicationId = "com.termux"
        minSdk = 24
        // IMPORTANT: targetSdk must be <= 28 to allow executing binaries
        // from app data directory. This is how Termux works.
        // Android 10+ (API 29+) blocks exec() from app data with SELinux.
        targetSdk = 28
        versionCode = 100
        versionName = "2.0.0-lab"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../mobilecli-release.keystore")
            storePassword = "mobilecli2026"
            keyAlias = "mobilecli"
            keyPassword = "mobilecli2026"
        }
    }

    // Two build flavors: user (clean UX) and dev (full terminal visibility)
    flavorDimensions += "mode"
    productFlavors {
        create("user") {
            dimension = "mode"
            // User build: clean UX, dev mode OFF by default
            buildConfigField("boolean", "DEV_MODE_DEFAULT", "false")
        }
        create("dev") {
            dimension = "mode"
            // Dev build: full visibility, dev mode ON by default
            buildConfigField("boolean", "DEV_MODE_DEFAULT", "true")
            // Developer edition - all features unlocked
            buildConfigField("boolean", "DEVELOPER_EDITION", "true")
            buildConfigField("boolean", "SHOW_ALL_COMMANDS", "true")
            buildConfigField("boolean", "DISABLE_IP_PROTECTION", "true")
            // Add "-dev" suffix to version name for clarity
            versionNameSuffix = "-dev"
        }
        create("lab") {
            dimension = "mode"
            // Lab build: experimental sandbox for inventions
            buildConfigField("boolean", "DEV_MODE_DEFAULT", "true")
            buildConfigField("boolean", "DEVELOPER_EDITION", "true")
            buildConfigField("boolean", "SHOW_ALL_COMMANDS", "true")
            buildConfigField("boolean", "DISABLE_IP_PROTECTION", "true")
            // Lab-specific features
            buildConfigField("boolean", "LAB_EDITION", "true")
            buildConfigField("boolean", "MULTI_AGENT_ENABLED", "true")
            buildConfigField("boolean", "EXPERIMENTAL_FEATURES", "true")
            versionNameSuffix = "-lab"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // CRITICAL: We MUST use targetSdk 28 for Termux binaries to execute
        // Android 10+ blocks exec() from app data directories
        // This is a technical requirement, not a choice
        disable += "ExpiredTargetSdkVersion"
        abortOnError = false
    }
}

dependencies {
    // Termux terminal libraries (Apache 2.0 licensed)
    implementation("com.github.termux.termux-app:terminal-view:v0.118.0")
    implementation("com.github.termux.termux-app:terminal-emulator:v0.118.0")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.slidingpanelayout:slidingpanelayout:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // JSON parsing for game scenes
    implementation("com.google.code.gson:gson:2.10.1")

    // RecyclerView for hierarchy panel
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
