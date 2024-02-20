plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
//    linuxArm64()
//    macosX64()
//    macosArm64()
    jvm {
        withJava()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":litmuskt:core"))
            }
        }
    }
}
