//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//
//    kotlin("plugin.serialization") version "1.9.23"
//
//    alias(libs.plugins.hilt)
//    alias(libs.plugins.kotlin.kapt)
//}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}


android {
    namespace = "com.kim.minemind"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.kim.minemind"
        minSdk = 35
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}


kapt {
    correctErrorTypes = true
}

dependencies {

    implementation("com.google.dagger:hilt-android:2.51")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui)
    implementation("androidx.compose.foundation:foundation:1.x.x") // Replace 1.x.x with the correct version

    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // needed for view model
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    testImplementation("junit:junit:4.13.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // VERY useful for solver debugging
    testImplementation("com.google.truth:truth:1.4.2")

    implementation("androidx.datastore:datastore-preferences:1.2.0")

    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")


    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(libs.androidx.datastore.core)

    kapt("com.google.dagger:hilt-compiler:2.51")
}
