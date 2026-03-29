plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.logix"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.logix"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        multiDexEnabled = true
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
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
    defaultConfig {
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.material:material:1.11.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("com.airbnb.android:lottie:3.4.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// Color Picker
    implementation("com.github.yukuku:ambilwarna:2.0.1")
// Constraint Layout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
// CardView
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("androidx.viewpager2:viewpager2:1.0.0")

    implementation("com.github.yukuku:ambilwarna:2.0.1")

    // SVG Support
    implementation("com.caverock:androidsvg:1.4")

    implementation("androidx.gridlayout:gridlayout:1.1.0")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.caverock:androidsvg:1.4")

    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation("com.github.yukuku:ambilwarna:2.0.1")

}