plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxArm64("linuxArm").compilations.getByName("main").cinterops.create("w")
    linuxX64("linux").compilations.getByName("main").cinterops.create("w")

    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val linuxMain by getting
        val linuxTest by getting
        val linuxArmMain by getting
        val linuxArmTest by getting

        val nativeMain by creating {
            this.dependsOn(commonMain)
            linuxArmMain.dependsOn(this)
        }
        val nativeTest by creating {
            this.dependsOn(commonTest)
            linuxArmTest.dependsOn(this)
        }

        val linuxIntermediateMain by creating {
            this.dependsOn(nativeMain)
            linuxMain.dependsOn(this)

            dependencies {
                implementation(project(":dep-with-cinterop"))
            }
        }
        val linuxIntermediateTest by creating {
            this.dependsOn(nativeTest)
            linuxTest.dependsOn(this)
        }
    }
}
