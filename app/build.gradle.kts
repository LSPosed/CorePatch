plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.lsplugin.resopt)
}

android {
    namespace = "org.lsposed.corepatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.lsposed.corepatch"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs["debug"]
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
        dex {
            useLegacyPackaging = true
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
}

afterEvaluate {
    tasks.getByPath("mergeReleaseArtProfile").enabled = false
    tasks.getByPath("compileReleaseArtProfile").enabled = false
    tasks.getByPath("extractReleaseVersionControlInfo").enabled = false
}
