plugins {
    kotlin("multiplatform")
}


group = "test"
version = "1.0"

kotlin {
    jvm()
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain {
            dependencies {
                api("test:lib:1.0-SNAPSHOT")
            }
        }
    }

}
