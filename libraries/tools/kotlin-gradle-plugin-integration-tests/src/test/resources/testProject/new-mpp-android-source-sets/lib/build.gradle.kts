import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

android {
    compileSdk = 34
    namespace = "io.sellmair.mpp"
    defaultConfig {
        minSdk = 22
        targetSdk = 34
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("pricing", "releaseType")

    productFlavors {
        create("beta") {
            dimension = "releaseType"
        }
        create("production") {
            dimension = "releaseType"
        }
        create("free") {
            dimension = "pricing"
        }
        create("paid") {
            dimension = "pricing"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        maybeCreate("beta").apply {
            setRoot("betaSrc/beta")
        }
        maybeCreate("freeBeta").apply {
            setRoot("betaSrc/freeBeta")
        }
        maybeCreate("freeBetaDebug").apply {
            setRoot("betaSrc/freeBetaDebug")
        }
        maybeCreate("freeBetaRelease").apply {
            setRoot("betaSrc/freeBetaRelease")
        }
        maybeCreate("paidBeta").apply {
            setRoot("betaSrc/paidBeta")
        }
        maybeCreate("paidBetaDebug").apply {
            setRoot("betaSrc/paidBetaDebug")
        }
        maybeCreate("paidBetaRelease").apply {
            setRoot("betaSrc/paidBetaRelease")
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }
    macosX64()

    sourceSets {
        getByName("commonMain").dependencies {
            implementation(kotlin("stdlib-common"))
        }

        getByName("commonTest").dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-annotations-common"))
        }

        getByName("androidMain").dependencies {
            implementation(kotlin("stdlib-jdk8"))
        }

        getByName("androidInstrumentedTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation("com.android.support.test:runner:1.0.2")
        }
    }
}
