plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}


group = "a"
version = "1.0"

kotlin {
    iosX64 {
        compilations.getByName("main") {
            cinterops {
                create("myinterop") {
                    headers("libs/include/myinterop.h")
                }
            }
        }
    }
    linuxX64 {
        compilations.getByName("main") {
            cinterops {
                create("myinterop") {
                    headers("libs/include/myinterop.h")
                }
            }
        }
    }
}
