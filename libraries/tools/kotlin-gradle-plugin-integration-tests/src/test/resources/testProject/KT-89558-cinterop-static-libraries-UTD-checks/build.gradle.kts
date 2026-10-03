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
            }
        }
    }
}
