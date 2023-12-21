plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "zone.ien.shampoo"
    compileSdk = 34

    defaultConfig {
        applicationId = "zone.ien.shampoo"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "0.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "versionName", versionName ?: "-")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-oss-licenses:17.0.1")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.akexorcist:round-corner-progress-bar:2.1.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.opencsv:opencsv:5.8")
    implementation("com.github.bumptech.glide:glide:4.14.2")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.dinuscxj:circleprogressbar:1.3.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}