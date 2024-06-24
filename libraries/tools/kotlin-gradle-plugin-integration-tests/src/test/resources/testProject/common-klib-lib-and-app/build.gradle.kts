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

	// macOS-specific targets - created by the ios() shortcut:
	ios()

	mingwX64()

	sourceSets {
		val commonMain by getting

		val windowsAndLinuxMain by creating {
			dependsOn(commonMain)
		}

		val linuxMain by creating {
			dependsOn(commonMain)
		}

		val mingwX64Main by getting {
			dependsOn(windowsAndLinuxMain)
		}
		val linuxX64Main by getting {
			dependsOn(linuxMain)
			dependsOn(windowsAndLinuxMain)
		}
		val linuxArm64Main by getting {
			dependsOn(linuxMain)
		}

		val jvmAndJsMain by creating {
			dependsOn(commonMain)
		}

		val jvmMain by getting {
			dependsOn(jvmAndJsMain)
		}

		val jsMain by getting {
			dependsOn(jvmAndJsMain)
		}

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