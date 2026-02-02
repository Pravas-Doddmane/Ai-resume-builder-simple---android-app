plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.passfamily.airesumebuilder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.passfamily.airesumebuilder"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)

    // Google Mobile Ads SDK
    implementation(libs.play.services.ads)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Country Code Picker
    implementation(libs.ccp)

    // Room Database
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // HTTP Client
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // JSON
    implementation(libs.gson)

    // PDF Generation
    implementation(libs.itext7.core)
    implementation("com.itextpdf:itext7-core:7.2.5")

    // RecyclerView
    implementation(libs.androidx.recyclerview)
    //
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}