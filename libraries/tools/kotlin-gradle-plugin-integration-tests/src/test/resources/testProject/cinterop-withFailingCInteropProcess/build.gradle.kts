plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64 {
        compilations.getByName("main") {
            cinterops {
                create("dummy") {
                    headers("libs/include/dummy.h")
                }

                create("failing") {
                    headers("libs/include/failing.h")
                }
            }
        }
    }
}

group = "com.example"
version = "1.0"