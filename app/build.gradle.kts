import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Load values from local.properties
val properties = Properties()
val propertiesFile = rootProject.file("local.properties")

if (propertiesFile.exists()) {
    properties.load(propertiesFile.inputStream())
}

val googleMapsApiKey =
    properties.getProperty("GOOGLE_MAPS_API_KEY", "")

android {
    namespace = "com.intellinav"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.intellinav"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // Google Maps API Key
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] =
            googleMapsApiKey

        buildConfigField(
            "String",
            "GOOGLE_MAPS_API_KEY",
            "\"$googleMapsApiKey\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // Google Maps + Location
    implementation(
        "com.google.android.gms:play-services-maps:19.2.0"
    )

    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )

    // TensorFlow Lite
    implementation(
        "org.tensorflow:tensorflow-lite:2.14.0"
    )

    implementation(
        "org.tensorflow:tensorflow-lite-support:0.4.4"
    )

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.8.5"
    )

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.okhttp)

    // Testing
    testImplementation("junit:junit:4.13.2")
}