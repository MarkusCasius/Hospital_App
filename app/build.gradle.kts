plugins { // Gradle plugins applied to this module
    alias(libs.plugins.android.application) // Use the Android Application plugin via the version catalogue alias
}
android { // Android-specific build configuration for the app module
    namespace = "com.example.hospimanagmenetapp" // Package namespace used for R classes & manifest
    compileSdk = 36 // Compile against Android API level 36 (must match an installed SDK)
    defaultConfig { // Default settings for all build variants
        applicationId = "com.example.hospimanagmenetapp" // Unique app ID used on devices/Play Store
        minSdk = 24 // Minimum Android version the app supports (Android 7.0)
        targetSdk = 36 // Target behaviour for API level 36 (opt-in to latest platform changes)
        versionCode = 1 // Internal version (integer) used for updates
        versionName = "1.0" // Human-readable version shown to users
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // Runner for instrumented (device) tests
    }

    buildTypes { // Definitions of build variants like debug/release
        release { // Settings for the release (shipping) build
            isMinifyEnabled = false // Disable code shrinking/obfuscation (set true for production usually)
            proguardFiles( // ProGuard/R8 rules used when shrinking/optimising
                getDefaultProguardFile("proguard-android-optimize.txt"), // Standard optimised rules supplied by the Android plugin
                "proguard-rules.pro" // Your custom rules file
            )
        }
    }

    buildFeatures { // Toggle optional Android build features
        viewBinding = true // Generate binding classes for views to avoid findViewById
    }

    compileOptions { // Java language level the compiler targets/accepts
        sourceCompatibility = JavaVersion.VERSION_11 // Allow Java 11 language features in source
        targetCompatibility = JavaVersion.VERSION_11 // Compile bytecode compatible with Java 11
    }
}
dependencies { // Libraries this module depends on

    implementation(libs.appcompat) // AndroidX AppCompat for backwards-compatible UI components
    implementation(libs.material) // Material Components for Android
    implementation(libs.activity) // AndroidX Activity KTX utilities
    implementation(libs.constraintlayout) // ConstraintLayout for flexible UI layouts

    // Lab 1 UI lists
    implementation("androidx.recyclerview:recyclerview:1.3.2") // RecyclerView for efficient scrolling lists/grids

    // Room (Java -> annotationProcessor)
    implementation("androidx.room:room-runtime:2.6.1") // Room runtime for SQLite ORM
    annotationProcessor("androidx.room:room-compiler:2.6.1") // Annotation processor generating Room DAOs/entities

    // Lifecycle (already had runtime + livedata) + ViewModel for Lab 2
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.6") // Lifecycle-aware components base runtime
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.6") // LiveData for observable data holders
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.6") // ViewModel for UI-related state

    // SQLCipher (Lab 1 basic-at-rest security placeholder)
    implementation("net.zetetic:android-database-sqlcipher:4.5.4") // Encrypted SQLite via SQLCipher
    implementation("androidx.sqlite:sqlite:2.4.0") // AndroidX SQLite wrappers/utilities

    // --- Lab 2: Network stack (Retrofit/OkHttp/Gson) ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0") // Retrofit HTTP client for type-safe APIs
    implementation("com.squareup.retrofit2:converter-gson:2.11.0") // Gson converter for JSON <-> objects
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // OkHttp core HTTP client
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // Interceptor for request/response logging

    // --- Lab 2: Biometric authentication ---
    implementation("androidx.biometric:biometric:1.2.0-alpha05") // BiometricPrompt APIs for fingerprint/face auth

    // Tests (version catalogue)
    testImplementation(libs.junit) // JUnit 4 for local unit tests
    androidTestImplementation(libs.ext.junit) // AndroidX JUnit extensions for instrumented tests
    androidTestImplementation(libs.espresso.core) // Espresso UI testing framework

}