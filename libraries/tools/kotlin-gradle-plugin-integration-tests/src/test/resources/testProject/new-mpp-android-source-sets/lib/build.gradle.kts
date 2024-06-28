plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

android {
    compileSdkVersion(28)
    namespace = "io.sellmair.mpp"
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
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
    android()
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
