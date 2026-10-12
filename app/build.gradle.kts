plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rawaj.smartclick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rawaj.smartclick"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
