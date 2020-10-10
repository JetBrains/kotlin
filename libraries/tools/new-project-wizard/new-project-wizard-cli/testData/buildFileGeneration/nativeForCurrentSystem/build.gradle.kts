plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}

group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("KOTLIN_REPO")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val myNativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("myNative")
        hostOs == "Linux" -> linuxX64("myNative")
        isMingwX64 -> mingwX64("myNative")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    
    sourceSets {
        val myNativeMain by getting
        val myNativeTest by getting
    }
}