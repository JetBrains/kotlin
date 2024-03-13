plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    iosX64()
    iosArm64()

    sourceSets.commonMain.get().dependencies {
        implementation("org.jetbrains.sample:producerA:1.0.0-SNAPSHOT")
    }
}