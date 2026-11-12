// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP's built-in Kotlin ships KGP 2.2, which cannot read Kotlin 2.4 metadata (miuix artifacts).
// Per https://kotl.in/gradle/agp-built-in-kotlin, a higher KGP version is selected by putting it
// on the top-level buildscript classpath. Keep in sync with kotlin in gradle/libs.versions.toml.
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}