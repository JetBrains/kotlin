plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    iosX64 {
        compilations.getByName("main") {
            cinterops.create("nlib") {
                defFile(file("$projectDir/native_lib/nlib.def"))
                includeDirs("$projectDir/native_lib")
            }
        }
    }
}
