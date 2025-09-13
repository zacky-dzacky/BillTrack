plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.ksp) // Added KSP plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.billtrack"
    compileSdk = 35 // Updated to required SDK

    defaultConfig {
        applicationId = "com.billtrack"
        minSdk = 26 // As per your existing gradle file
        targetSdk = 35 // Updated to required SDK
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

    implementation(libs.androidx.core.ktx)
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Import the BoM for the Firebase platform
    implementation(platform(libs.firebase.bom))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.firebase.auth)
//    implementation("com.google.firebase:firebase-auth-ktx")

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.mlkit.genai.image.description) // This was for a different ML Kit feature, GenAI
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)

    // CameraX dependencies
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Chucker for network monitoring
    debugImplementation("com.github.chuckerteam.chucker:library:4.2.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.2.0")

    // ML Kit Text Recognition
    implementation(libs.mlkit.text.recognition)

    // ML Kit Image Labeling
    implementation(libs.mlkit.image.labeling)
    implementation("androidx.navigation:navigation-fragment:2.9.4")
    implementation("androidx.navigation:navigation-ui:2.9.4")

    // Room Persistence Library
    implementation(libs.androidx.room.runtime) // Assuming libs.androidx.room.runtime is defined
    implementation(libs.androidx.room.ktx)     // Assuming libs.androidx.room.ktx is defined (for Coroutines)
    ksp(libs.androidx.room.compiler)           // Assuming libs.androidx.room.compiler is defined

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}