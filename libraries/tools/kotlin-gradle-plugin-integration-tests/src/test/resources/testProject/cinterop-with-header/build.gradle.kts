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
                val cinterop = create("cinterop").apply {
                    headers("libs/include/dummy.h")
                    compilerOpts.add("-Ilibs/include")
                    packageName("cinterop")
                }
            }
        }
    }
}
