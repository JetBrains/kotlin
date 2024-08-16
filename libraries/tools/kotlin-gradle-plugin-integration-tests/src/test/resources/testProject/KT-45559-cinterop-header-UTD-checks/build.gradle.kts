plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    <SingleNativeTarget>("native") {
        binaries {
            executable()
        }
        compilations.getByName("main") {
            cinterops {
                val cinterop by creating {
                    includeDirs("includeDirs")
                }
            }
        }
    }
}