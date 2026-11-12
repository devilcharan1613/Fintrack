plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tracmio.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.aistudio.tracmio.tracmioxyz"
        minSdk = 23
        targetSdk = 35
        versionCode = 12
        versionName = "1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("debugConfig") {
            val keystoreFile = rootProject.file("debug.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }
    buildTypes {
        getByName("debug") {
            val keystoreFile = rootProject.file("debug.keystore")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("debugConfig")
            }
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "Tracmio-${variant.versionName}-${variant.buildType.name}.apk"
            }
    }
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.doLast {
        val debugDir = file("${layout.buildDirectory.get().asFile}/outputs/apk/debug")
        if (debugDir.exists()) {
            val apkFiles = debugDir.listFiles { _, name -> name.endsWith(".apk") }
            if (apkFiles != null && apkFiles.isNotEmpty()) {
                val primaryApk = apkFiles.first()
                val targetTracmio = File(debugDir, "Tracmio-1.2-debug.apk")
                val targetLegacy = File(debugDir, "Tracmio-1.00-debug.apk")
                val targetDefault = File(debugDir, "app-debug.apk")
                
                if (!targetTracmio.exists() || primaryApk != targetTracmio) {
                    primaryApk.copyTo(targetTracmio, overwrite = true)
                }
                if (!targetLegacy.exists()) {
                    targetTracmio.copyTo(targetLegacy, overwrite = true)
                }
                if (!targetDefault.exists()) {
                    targetTracmio.copyTo(targetDefault, overwrite = true)
                }
                
                // Also copy to root directory
                val rootTracmio = rootProject.file("Tracmio-1.2-debug.apk")
                val rootLegacy = rootProject.file("Tracmio-1.00-debug.apk")
                val rootDefault = rootProject.file("app-debug.apk")
                targetTracmio.copyTo(rootTracmio, overwrite = true)
                targetTracmio.copyTo(rootLegacy, overwrite = true)
                targetTracmio.copyTo(rootDefault, overwrite = true)
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.json:json:20231013")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
