plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64 {
        binaries.staticLib {
            export("com.example:lib:1.0")
        }
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                api("com.example:lib:1.0")
            }
        }
    }
}
