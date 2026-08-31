import org.gradle.kotlin.dsl.implementation
import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics.plugin)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.example.linee_langer"
    compileSdk = 36



    defaultConfig {
        applicationId = "com.example.linee_langer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties()

        // 2. Carica il file in sicurezza
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                localProperties.load(stream)
            }
        }

        // 3. Recupera la chiave (gestendo il caso in cui manchi)
        val googleId = localProperties.getProperty("GOOGLE_SERVER_CLIENT_ID") ?: ""

        // 4. Crea il campo BuildConfig
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleId\"")

    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        jniLibs {
            // This force-aligns native libraries to 16 KB boundaries
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildToolsVersion = "36.0.0"

    kotlin{
        compilerOptions{
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

detekt {
    toolVersion = "1.23.5"
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}


dependencies {

    implementation(project(":opencv"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    //noinspection LoginCredentials
    implementation(libs.gms)
    implementation(libs.crop)
    //noinspection LoginCredentials
    implementation(libs.credentials)
    //noinspection LoginCredentials
    implementation(libs.credentials.services)
    //noinspection LoginCredentials
    implementation(libs.identity.google)
    implementation(libs.work)
    implementation(libs.androidx.hilt.work)
    implementation(libs.camerax)
    implementation(libs.camera2)
    implementation(libs.cameralifecycle)
    implementation(libs.lottie)
    implementation(libs.hilt)
    implementation(libs.androidx.hilt.common)
    implementation(libs.kotlinx.json)
    implementation(libs.foundation.layout)
    implementation(libs.browser)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler) // 👈 FONDAMENTALE per i Worker
    implementation(libs.androidx.hilt)
    implementation(libs.mediapipe)
    implementation(libs.glide)
    implementation(libs.photoview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.runtime)
    implementation(libs.androidx.material3.window.size.class1)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.camera.view)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.datastore)
    implementation(libs.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.coil)
    implementation(libs.androidx.graphics)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.crashlytics)
    implementation(libs.analytics)

    debugImplementation(libs.leakcanary.android)


}