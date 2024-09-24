plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            isStatic = <is_static>
            baseName = "shared"
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}