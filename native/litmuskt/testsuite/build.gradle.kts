plugins {
    kotlin("multiplatform")
}

kotlin {
    // targets have to be the same as in :cli (because it depends on this subproject)
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
