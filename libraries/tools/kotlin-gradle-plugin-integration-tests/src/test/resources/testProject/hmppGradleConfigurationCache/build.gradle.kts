plugins {
    kotlin("multiplatform") version "<pluginMarkerVersion>"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("${rootProject.projectDir}/repo")
    }
}

kotlin {
    jvm()
    linuxX64 {
        binaries { sharedLib() }
        compilations.getByName("main").apply {
            cinterops {
                val foo by creating {
                    defFile(project.file("foo.def"))
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("test:lib:1.0")
            }
        }
    }
}
