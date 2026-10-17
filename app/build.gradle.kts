import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Personal signing key, kept outside the repo (docs/06 § Release). Without it, release builds are
// unsigned and debug builds use the default debug key.
val signingProperties: Properties? = listOf(
    rootProject.file("keystore.properties"),
    File(System.getProperty("user.home"), ".android/edgevol/release.properties"),
).firstOrNull { it.isFile }?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }

android {
    namespace = "app.edgevol"
    // Compose BOM 2026.09.00 (ui 1.12.1) requires compileSdk 37; targetSdk stays 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "app.edgevol"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (signingProperties != null) {
            create("personal") {
                storeFile = file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Same key for both, so a debug build can replace the release build (and back) without an
        // uninstall, which would switch the accessibility service off.
        val personal = signingConfigs.findByName("personal")
        debug {
            if (personal != null) signingConfig = personal
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (personal != null) signingConfig = personal
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric reads the merged resources and manifest.
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)

    // ui-test-manifest adds the empty test activity createComposeRule() launches (debug builds only).
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
}
