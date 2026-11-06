plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.charan.fintrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.charan.fintrack"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "4.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Retained because the repository still contains the unused Room account data layer.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
}
