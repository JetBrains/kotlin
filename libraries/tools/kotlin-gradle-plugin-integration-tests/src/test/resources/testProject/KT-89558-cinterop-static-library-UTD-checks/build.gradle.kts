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
            executable {
                entryPoint = "main"
                val staticLibrary = projectDir.resolve("lib.a").absolutePath
                linkerOpts(staticLibrary)
            }
        }
        compilations.getByName("main") {
            cinterops {
                create("myInterop")
            }
        }
    }
}
