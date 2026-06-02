plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxArm64("linuxArm").compilations.getByName("main").cinterops.create("w")
    linuxX64("linux").compilations.getByName("main").cinterops.create("w")

    sourceSets {
        val commonMain = getByName("commonMain")
        val commonTest = getByName("commonTest")
        val linuxMain = getByName("linuxMain")
        val linuxTest = getByName("linuxTest")
        val linuxArmMain = getByName("linuxArmMain")
        val linuxArmTest = getByName("linuxArmTest")

        val nativeMain = create("nativeMain")
        nativeMain.dependsOn(commonMain)
        linuxArmMain.dependsOn(nativeMain)

        val nativeTest = create("nativeTest")
        nativeTest.dependsOn(commonTest)
        linuxArmTest.dependsOn(nativeTest)

        val linuxIntermediateMain = create("linuxIntermediateMain").apply {
            this.dependsOn(nativeMain)
            linuxMain.dependsOn(this)

            dependencies {
                implementation(project(":dep-with-cinterop"))
            }
        }
        val linuxIntermediateTest = create("linuxIntermediateTest").apply {
            this.dependsOn(nativeTest)
            linuxTest.dependsOn(this)
        }
    }
}
