plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val releaseStoreFilePath =
    providers.environmentVariable(
        "NEXPLAY_RELEASE_STORE_FILE",
    )

val releaseStorePassword =
    providers.environmentVariable(
        "NEXPLAY_RELEASE_STORE_PASSWORD",
    )

val releaseKeyAlias =
    providers.environmentVariable(
        "NEXPLAY_RELEASE_KEY_ALIAS",
    )

val releaseKeyPassword =
    providers.environmentVariable(
        "NEXPLAY_RELEASE_KEY_PASSWORD",
    )

val releaseSigningAvailable =
    releaseStoreFilePath.isPresent &&
        releaseStorePassword.isPresent &&
        releaseKeyAlias.isPresent &&
        releaseKeyPassword.isPresent

android {
    namespace = "com.nexplay.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nexplay.app"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile =
                    rootProject.file(
                        releaseStoreFilePath.get(),
                    )
                storePassword =
                    releaseStorePassword.get()
                keyAlias =
                    releaseKeyAlias.get()
                keyPassword =
                    releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningAvailable) {
                signingConfig =
                    signingConfigs.getByName(
                        "release",
                    )
            }

            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}