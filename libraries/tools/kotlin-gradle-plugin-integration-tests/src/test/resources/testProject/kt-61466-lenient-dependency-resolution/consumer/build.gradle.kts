plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    sourceSets.commonMain.dependencies {
        implementation(project(":producer")) // <- does not support any native target
        implementation("this:does:not-exist:1.0.0") // <- Obviously, does not exist :)
    }
}