// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Google Services 플러그인 의존성을 최신 방식으로 추가
        classpath("com.google.gms:google-services:4.3.15") // 최신 버전
        // Android Gradle Plugin 추가
        classpath("com.android.tools.build:gradle:8.1.0") // 최신 버전
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}