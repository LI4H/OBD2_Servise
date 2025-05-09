plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //id("androidx.navigation.safeargs.kotlin") version "2.7.7"
    //id("com.android.application")
    id("com.google.gms.google-services")

    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.example.obd_servise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.LICH.Projects"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{LICENSE.md,LICENSE-notice.md}"
        }
    }
}

dependencies {
    //def room_version = "2.6.1"

    implementation(libs.kotlinx.metadata.jvm)


// Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.room.runtime.android)
    kapt(libs.hilt.compiler)




    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    // Kolin OBD API
    implementation(libs.kotlin.obd.api)
    // lifeChart
    implementation(libs.mpandroidchart)
    // Lifecycle ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v260)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx.v260)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.filament.android)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    implementation(libs.androidx.preference)
    implementation(libs.firebase.database.ktx)
    implementation(libs.screenshot.validation.junit.engine)
    implementation(libs.androidx.room.common.jvm)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
