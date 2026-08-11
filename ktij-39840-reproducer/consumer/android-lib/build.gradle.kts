plugins {
    id("com.android.library")
}

android {
    namespace = "ru.quickresto.consumer.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// AGP 9 built-in Kotlin: jvmTarget defaults to compileOptions.targetCompatibility.
// Optional explicit compilerOptions remain available via the built-in kotlin extension.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Root KMP coordinate — Gradle/IDE should select the JVM variant for Android.
    // Reporter also tried the explicit -jvm artifact; both go through mavenLocal only.
    implementation("ru.quickresto:kkm-contract:3.0.0")
}
