@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets.commonMain.get().dependencies {
        implementation("org.jetbrains.sample:producerA:1.0.0-SNAPSHOT")
    }
}