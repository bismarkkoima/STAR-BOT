import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load secrets from local.properties (gitignored) instead of hardcoding them.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.personalai.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.personalai.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "CLAUDE_API_KEY", "\"${localProps.getProperty("CLAUDE_API_KEY", "")}\"")
        buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"${localProps.getProperty("PICOVOICE_ACCESS_KEY", "")}\"")
        // Offline-first: true (default) = never touch the network, use only the on-device model.
        buildConfigField("boolean", "OFFLINE_ONLY", localProps.getProperty("OFFLINE_ONLY", "true"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // NOTE: for a personal-use app, put your API key in local.properties
    // (gitignored) and read it via BuildConfig, rather than hardcoding it.
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Bundle config/tools.json and prompts/system_prompt.md directly from the
    // repo root as assets, so there's one source of truth instead of copies
    // living both in /config and /app/src/main/assets.
    sourceSets {
        getByName("main") {
            assets.srcDirs("../../config", "../../prompts")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // HTTP client for calling the Claude API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // On-device LLM (Gemma) via MediaPipe LLM Inference API — the offline brain.
    // If Gradle can't resolve this version, bump to the latest tasks-genai on Maven Central.
    implementation("com.google.mediapipe:tasks-genai:0.10.24")

    // Wake-word detection ("Hey Assistant") - fully offline, free for personal use.
    // Sign up at console.picovoice.ai for a free AccessKey (see docs/02_voice_wake_word.md).
    implementation("ai.picovoice:porcupine-android:3.0.2")
}
