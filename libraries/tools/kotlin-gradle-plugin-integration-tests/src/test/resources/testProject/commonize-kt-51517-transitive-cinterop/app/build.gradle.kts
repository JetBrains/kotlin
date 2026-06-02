plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    listOf(linuxX64(), linuxArm64(), mingwX64()).forEach {
        it.compilations.getByName("main") {
            cinterops.create("yummy") {
                val nativeLibs = rootDir.resolve("native-libs")
                defFile = nativeLibs.resolve("yummy.def")
                compilerOpts += "-I" + nativeLibs.absolutePath
            }
        }
    }

    sourceSets {
        val commonMain = getByName("commonMain")
        val linuxX64Main = getByName("linuxX64Main")
        val linuxArm64Main = getByName("linuxArm64Main")
        val mingwX64Main = getByName("mingwX64Main")

        val linuxMain = create("linuxMain").apply {
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }
        val nativeMain = create("nativeMain").apply {
            this.dependsOn(commonMain)
            linuxMain.dependsOn(this)
            mingwX64Main.dependsOn(this)
            dependencies {
                implementation(project(":lib"))
            }
        }
        val commonTest = getByName("commonTest")
        val linuxX64Test = getByName("linuxX64Test")
        val linuxArm64Test = getByName("linuxArm64Test")
        val mingwX64Test = getByName("mingwX64Test")

        val linuxTest = create("linuxTest").apply {
            linuxX64Test.dependsOn(this)
            linuxArm64Test.dependsOn(this)
        }
        val nativeTest = create("nativeTest").apply {
            this.dependsOn(commonTest)
            linuxTest.dependsOn(this)
            mingwX64Test.dependsOn(this)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
