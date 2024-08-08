plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    val commonMain by sourceSets.getting
    commonMain.dependencies {
        implementation(project(":p1"))
    }
}