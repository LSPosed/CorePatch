import java.nio.file.Paths

plugins {
    id("com.android.application")
}

val releaseStoreFile: String? by rootProject
val releaseStorePassword: String? by rootProject
val releaseKeyAlias: String? by rootProject
val releaseKeyPassword: String? by rootProject

android {
    compileSdk = 33
    buildToolsVersion = "33.0.0"
    defaultConfig {
        applicationId = "com.coderstory.toolkit"
        minSdk = 29
        targetSdk = 33
        versionCode = 1997
        versionName = "4.2"
    }

    signingConfigs {
        create("config") {
            releaseStoreFile?.also {
                storeFile = rootProject.file(it)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    packagingOptions {
        jniLibs {
            excludes += "META-INF/**"
        }
        resources {
            excludes += "META-INF/**"
        }
    }


    buildTypes {
        all {
            signingConfig =
                if (releaseStoreFile.isNullOrEmpty()) signingConfigs.getByName("debug") else signingConfigs.getByName(
                    "config"
                )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_11)
        targetCompatibility(JavaVersion.VERSION_11)
    }
    lint {
        abortOnError = false
    }
    namespace = "com.coderstory.toolkit"
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
    val aapt2 = Paths.get(
        project.android.sdkDirectory.path,
        "build-tools", project.android.buildToolsVersion, "aapt2"
    )
    val zip = Paths.get(
        project.buildDir.path, "intermediates",
        "optimized_processed_res", "release", "resources-release-optimize.ap_"
    )
    val optimized = File("${zip}.opt")
    val cmd = exec {
        commandLine(aapt2, "optimize", "--collapse-resource-names", "-o", optimized, zip)
        isIgnoreExitValue = true
    }
    if (cmd.exitValue == 0) {
        delete(zip)
        optimized.renameTo(zip.toFile())
    }
}
tasks.whenTaskAdded {
    when (name) {
        "optimizeReleaseResources" -> {
            finalizedBy(optimizeReleaseRes)
        }
    }
}
