plugins {
	kotlin("multiplatform")
	id("maven-publish")
}

group = "com.example"
version = "1.0"

kotlin {
	jvm()
	js()

	linuxX64()
	linuxArm64()

	iosX64()
	iosArm64()
	iosSimulatorArm64()

	mingwX64()

	sourceSets {
		val commonMain = getByName("commonMain")

		val windowsAndLinuxMain = create("windowsAndLinuxMain") {
			dependsOn(commonMain)
		}

		val linuxMain = create("linuxMain") {
			dependsOn(commonMain)
		}

		val mingwX64Main = getByName("mingwX64Main")
        mingwX64Main.dependsOn(windowsAndLinuxMain)

		val linuxX64Main = getByName("linuxX64Main")
        linuxX64Main.dependsOn(linuxMain)
        linuxX64Main.dependsOn(windowsAndLinuxMain)

		val linuxArm64Main = getByName("linuxArm64Main")
        linuxArm64Main.dependsOn(linuxMain)

		val jvmAndJsMain = create("jvmAndJsMain")
        jvmAndJsMain.dependsOn(commonMain)

		val jvmMain = getByName("jvmMain")
        jvmMain.dependsOn(jvmAndJsMain)

		val jsMain = getByName("jsMain")
        jsMain.dependsOn(jvmAndJsMain)

		val iosMain = create("iosMain")
        iosMain.dependsOn(commonMain)

		val iosX64Main = getByName("iosX64Main")
        iosX64Main.dependsOn(iosMain)

		val iosArm64Main = getByName("iosArm64Main")
        iosArm64Main.dependsOn(iosMain)

		val iosSimulatorArm64Main = getByName("iosSimulatorArm64Main")
        iosSimulatorArm64Main.dependsOn(iosMain)

		all {
			languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
		}
	}
}

publishing {
	repositories {
		maven("<localRepo>")
	}
}

tasks {
	val skipCompilationOfTargets = kotlin.targets.matching { it.platformType.toString() == "native" }.names
	withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
		val target = name.removePrefix("compileKotlin").decapitalize()
		if (target in skipCompilationOfTargets) {
			actions.clear()
			val destinationDirProvider = project.layout.buildDirectory.dir("classes/kotlin/$target/main/klib/${project.name}.klib")
			doLast {
				val destinationFile = destinationDirProvider.get().asFile
				destinationFile.parentFile.mkdirs()
				println("Writing a dummy klib to $destinationFile")
				destinationFile.createNewFile()
			}
		}
	}
    withType<Zip> {
        val target = name.removeSuffix("Klib")
        if (target in skipCompilationOfTargets) {
            from("build.gradle.kts") // to make the task run
            actions.clear()
            doLast {
                val destinationFile = archiveFile.get().asFile
                destinationFile.parentFile.mkdirs()
                println("Writing a dummy klib to $destinationFile")
                destinationFile.createNewFile()
            }
        }
    }
}
