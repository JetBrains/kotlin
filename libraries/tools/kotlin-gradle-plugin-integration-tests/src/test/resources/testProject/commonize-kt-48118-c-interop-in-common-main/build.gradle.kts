plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxArm64() {
        compilations.getByName("main") {
            cinterops {
                create("dummy") {
                    headers("libs/include/dummy/dummy.h")
                    compilerOpts.add("-Ilibs/include")
                }
            }
        }
    }
    linuxX64(){
        compilations.getByName("main") {
            cinterops {
                create("dummy") {
                    headers("libs/include/dummy/dummy.h")
                    compilerOpts.add("-Ilibs/include")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting

        val linuxArm64Main by getting
        val linuxX64Main by getting

        val upperMain by creating {
            dependsOn(commonMain)
        }

        val lowerMain by creating {
            dependsOn(upperMain)
            linuxArm64Main.dependsOn(this)
            linuxX64Main.dependsOn(this)
        }
    }
}

