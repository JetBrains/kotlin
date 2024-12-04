plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxArm64()
    linuxX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation("kt50925:lib:1.0")
                implementation(project(":subproject"))
            }
        }
    }
}
