plugins {
    kotlin("multiplatform")
}

repositories {
    maven("<localRepo>")
}


kotlin {
    applyDefaultHierarchyTemplate()

    jvm()
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain {
            dependencies {
                api("test:libWithDefaultLayout:1.0")
            }
        }
    }
}