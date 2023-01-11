plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    ios {
        compilations.getByName("main").cinterops.create("w")
    }
    linuxX64("linux").compilations.getByName("main").cinterops.create("w")

    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val linuxMain by getting
        val linuxTest by getting

        val nativeMain by creating {
            this.dependsOn(commonMain)
            getByName("iosMain").dependsOn(this)
        }
        val nativeTest by creating {
            this.dependsOn(commonTest)
            getByName("iosTest").dependsOn(this)
        }

        val linuxOtherMain by creating {
            this.dependsOn(nativeMain)
            linuxMain.dependsOn(this)

            dependencies {
                implementation(project(":dep-with-cinterop"))
            }
        }
        val linuxOtherTest by creating {
            this.dependsOn(nativeTest)
            linuxTest.dependsOn(this)
        }
    }
}
