// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jcenter.bintray.com")
    }
}

