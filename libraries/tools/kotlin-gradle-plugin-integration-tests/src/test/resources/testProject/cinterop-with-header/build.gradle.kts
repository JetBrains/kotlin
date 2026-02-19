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
                    headers("libs/include/dummy.h")
                    compilerOpts.add("-Ilibs/include")
                    packageName("cinterop")
                }
            }
        }
    }
}