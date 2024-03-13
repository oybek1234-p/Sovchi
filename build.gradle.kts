// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
    repositories {
        maven {
            url = uri("https://www.jitpack.io")
            url = uri("https://github.com/psiegman/mvn-repo/raw/master/releases")
        }
        mavenCentral()
        jcenter()
        google()
    }

}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}


