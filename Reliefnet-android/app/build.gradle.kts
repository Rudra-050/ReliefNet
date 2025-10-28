plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)


    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")


    //id("com.google.gms.google-services")
}

android {
    namespace = "com.sentrive.reliefnet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sentrive.reliefnet"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Debug: Priority order - ngrok URL > LAN IP > emulator loopback
            val ngrokUrl = project.findProperty("DEV_NGROK_URL") as String? ?: ""
            val devHostIp = project.findProperty("DEV_HOST_IP") as String? ?: ""
            
            val debugBaseUrl = when {
                ngrokUrl.isNotBlank() -> "$ngrokUrl/"
                devHostIp.isNotBlank() -> "http://$devHostIp:5000/"
                else -> "http://10.0.2.2:5000/"
            }
            
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            // Production API on Railway
            buildConfigField("String", "BASE_URL", "\"https://reliefnet-production-e119.up.railway.app/\"")
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
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended") // Use BOM version
    // implementation(libs.androidx.compose.ui.text.google.fonts) // Temporarily removed due to Text composable conflict
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //navigation dependency
    val nav_version = "2.9.5"

    implementation("androidx.navigation:navigation-compose:$nav_version")
    //Coroutine Dependency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")


    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging")

    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Google Pay (Google Wallet API)
    implementation("com.google.android.gms:play-services-wallet:19.3.0")

    // Socket.IO for real-time communication
    implementation("io.socket:socket.io-client:2.1.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    // WebRTC for video/audio calls (maintained artifact on Maven Central)
    implementation("io.github.webrtc-sdk:android:125.6422.04")

    implementation("androidx.compose.runtime:runtime") // Use BOM version
    implementation("androidx.compose.runtime:runtime-livedata") // Use BOM version
    implementation("androidx.compose.runtime:runtime-saveable") // Use BOM version
configurations.all {
    resolutionStrategy {
        force(
            "androidx.compose.ui:ui:1.9.2",
            "androidx.compose.material3:material3:1.4.0",
            "androidx.compose.material:material:1.7.0",
            "androidx.compose.runtime:runtime:1.9.2",
            "androidx.compose.foundation:foundation:1.9.3"
        )
    }
}
}