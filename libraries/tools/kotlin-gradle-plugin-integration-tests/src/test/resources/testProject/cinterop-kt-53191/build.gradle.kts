plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64("linux") {
        compilations.getByName("main") {
            cinterops.create("nlib") {
                defFile(file("$projectDir/native_lib/nlib.def"))
                includeDirs("$projectDir/native_lib")
            }
        }
    }
}
