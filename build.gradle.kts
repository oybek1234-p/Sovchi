buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false

    id ("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    kotlin("jvm") version "1.9.20" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.9.20" apply true
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

