plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    <SingleNativeTarget>("native") {
        compilations.getByName("main") {
            cinterops {
                create("cinterop") {
                    // Relative library paths, both from the .def file and from -libraryPath, are resolved against -Xproject-dir
                    extraOpts("-Xproject-dir", projectDir.absolutePath)
                    extraOpts("-libraryPath", "libs")
                    extraOpts("-staticLibrary", "libA.a,libB.a")
                }
                create("noProjectDir") {
                    // Without -Xproject-dir, relative library paths are resolved against the working directory of the tool
                    extraOpts("-libraryPath", "libs")
                    extraOpts("-staticLibrary", "libC.a")
                }
            }
        }
    }
}
