import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.lsplugin.resopt)
}

configure<ApplicationExtension> {
    namespace = "org.lsposed.corepatch"
    compileSdk = 37

    val releaseSigningPropertyNames = listOf(
        "releaseStoreFile",
        "releaseStorePassword",
        "releaseKeyAlias",
        "releaseKeyPassword",
    )
    val hasReleaseSigningProperties =
        releaseSigningPropertyNames.all { providers.gradleProperty(it).isPresent }

    defaultConfig {
        applicationId = "org.lsposed.corepatch"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningProperties) {
                storeFile = rootProject.file(providers.gradleProperty("releaseStoreFile").get())
                storePassword = providers.gradleProperty("releaseStorePassword").get()
                keyAlias = providers.gradleProperty("releaseKeyAlias").get()
                keyPassword = providers.gradleProperty("releaseKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            @Suppress("UnstableApiUsage")
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs["release"].takeIf { hasReleaseSigningProperties }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

val deleteAppMetadata = tasks.register("deleteAppMetadata") {
    val appMetadataFile =
        file("build/intermediates/app_metadata/release/writeReleaseAppMetadata/app-metadata.properties")
    doLast {
        appMetadataFile.writeText(
            ""
        )
    }
}

afterEvaluate {
    tasks.named("writeReleaseAppMetadata") {
        finalizedBy(deleteAppMetadata)
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
}
