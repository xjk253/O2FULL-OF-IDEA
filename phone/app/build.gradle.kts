plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.bubblepet"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    signingConfigs {
        create("bubble") {
            val ksFile = file("../bubble-release.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = providers.environmentVariable("BUBBLE_KS_PASSWORD").getOrElse("bubblepet")
                keyAlias = "bubble"
                keyPassword = providers.environmentVariable("BUBBLE_KS_PASSWORD").getOrElse("bubblepet")
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.bubblepet"
        minSdk = 24
        targetSdk = 36
        versionCode = 110
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("bubble")
        }
        release {
            signingConfig = signingConfigs.getByName("bubble")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("org.json:json:20231013")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}