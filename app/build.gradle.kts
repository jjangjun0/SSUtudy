plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ssutudy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ssutudy"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    viewBinding {
        enable = true
    }
}

dependencies {
    // Firebase BOM 설정
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))

    // Firebase 라이브러리 (BOM에서 자동으로 버전을 관리)
    implementation("com.google.firebase:firebase-firestore-ktx") // Firestore (Kotlin 확장)
    implementation("com.google.firebase:firebase-auth") // Firebase 인증
    implementation("com.google.firebase:firebase-database") // Firebase Realtime Database

    // FirebaseUI Firestore
    implementation("com.firebaseui:firebase-ui-firestore:8.0.0")

//    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1") {
        exclude(group = "com.android.support") // Support 라이브러리 제외
    }

    // Support Library를 AndroidX로 교체
    implementation("androidx.appcompat:appcompat:1.6.1") // 최신 AndroidX AppCompat 라이브러리
    implementation("androidx.core:core-ktx:1.12.0")      // 최신 AndroidX Core 라이브러리

    // 기타 라이브러리
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // 테스트 라이브러리
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}