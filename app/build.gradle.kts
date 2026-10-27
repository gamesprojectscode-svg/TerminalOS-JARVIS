plugins {
    id("com.android.application")
    id("com.chaquo.python")
}

android {
    namespace = "com.terminalos.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.terminalos.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
