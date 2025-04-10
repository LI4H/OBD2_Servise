plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.obd_servise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.obd_servise"
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
}

dependencies {
    // Kolin OBD API
    implementation("com.github.eltonvs:kotlin-obd-api:1.3.0")
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

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
