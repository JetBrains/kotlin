plugins {
    kotlin("multiplatform") version "1.3.61"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("myNative")
        hostOs == "Linux" -> linuxX64("myNative")
        isMingwX64 -> mingwX64("myNative")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "MAIN CLASS"
            }
        }
    }
    sourceSets {
        val myNativeMain by getting
        val myNativeTest by getting
    }
}