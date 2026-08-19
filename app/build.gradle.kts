plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jeremy.glasspanel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jeremy.glasspanel"
        minSdk = 28 // Lower bound to support hooked system processes
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // LibXposed API 102 dependency (compileOnly so it isn't bundled into the APK runtime)
    compileOnly("io.github.libxposed:api:102.0.0")
implementation("com.google.android.material:material:1.12.0")
    // AndroidX Core support
    implementation("androidx.core:core-ktx:1.13.1")
}
