import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

android {
    compileSdkVersion(34)
    namespace = "io.sellmair.mpp"
    defaultConfig {
        minSdkVersion(22)
        targetSdkVersion(34)
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions("pricing", "releaseType")

    productFlavors {
        create("beta") {
            setDimension("releaseType")
        }
        create("production") {
            setDimension("releaseType")
        }
        create("free") {
            setDimension("pricing")
        }
        create("paid") {
            setDimension("pricing")
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
    macosX64("macos")

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
