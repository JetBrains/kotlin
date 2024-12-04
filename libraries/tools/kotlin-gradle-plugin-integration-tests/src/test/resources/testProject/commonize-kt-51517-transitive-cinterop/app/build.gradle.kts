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
        val commonMain by getting
        val linuxX64Main by getting
        val linuxArm64Main by getting
        val mingwX64Main by getting

        val linuxMain by creating {
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }
        val nativeMain by creating {
            this.dependsOn(commonMain)
            linuxMain.dependsOn(this)
            mingwX64Main.dependsOn(this)
            dependencies {
                implementation(project(":lib"))
            }
        }
        val commonTest by getting
        val linuxX64Test by getting
        val linuxArm64Test by getting
        val mingwX64Test by getting

        val linuxTest by creating {
            linuxX64Test.dependsOn(this)
            linuxArm64Test.dependsOn(this)
        }
        val nativeTest by creating {
            this.dependsOn(commonTest)
            linuxTest.dependsOn(this)
            mingwX64Test.dependsOn(this)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
