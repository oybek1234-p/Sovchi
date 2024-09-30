plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.uz.sovchi"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.uz.sovchi"
        minSdk = 24
        targetSdk = 34
        versionCode = 311
        versionName = "1."

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
//        debug {
//            isMinifyEnabled = true
//            isShrinkResources = true
//        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
    }

}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-common-ktx:2.7.7")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-firestore:25.1.0")
    implementation("com.google.firebase:firebase-inappmessaging-display:21.0.0")
    implementation("com.google.firebase:firebase-messaging:24.0.1")
    implementation("com.google.firebase:firebase-storage:21.0.0")
    implementation("com.google.firebase:firebase-analytics:22.1.0")
    implementation("com.recombee:api-client:4.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    //Navigation
    val nav_version = "2.7.6"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

    implementation("com.github.vacxe:phonemask:1.0.5")
    implementation("com.google.firebase:firebase-auth")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    kapt("androidx.room:room-compiler:$room_version")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug:18.0.0")

    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:review:2.0.1")
    implementation ("top.zibin:Luban-turbo:1.0.0")
    implementation("io.coil-kt:coil:2.6.0")

    implementation("com.google.firebase:firebase-functions")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("com.github.stfalcon-studio:StfalconImageViewer:v1.0.1")

    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-auth")
    implementation ("com.facebook.android:facebook-android-sdk:latest.release")

    //Blue transform glide
    implementation ("jp.wasabeef:glide-transformations:4.3.0")

    implementation ("io.fotoapparat:fotoapparat:2.7.0")

    implementation("com.tbuonomo:dotsindicator:5.0")
    implementation("com.google.dagger:dagger:2.16")
    kapt("com.google.dagger:dagger-compiler:2.16")

    api("io.reactivex.rxjava2:rxandroid:2.1.1")
    api("org.jetbrains.kotlin:kotlin-reflect:1.8.22")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
    implementation("io.ktor:ktor-client-android:1.5.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    //implement lottie
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation ("id.zelory:compressor:3.0.1")
    implementation ("androidx.core:core-splashscreen:1.0.1")

    implementation ("androidx.work:work-runtime:2.9.1")

}