plugins {
	kotlin("multiplatform").version("<pluginMarkerVersion>")
	id("maven-publish")
}

group = "com.example"
version = "1.0"

repositories {
	mavenLocal()
	mavenCentral()
}

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
		maven("$rootDir/repo")
	}
}

tasks {
	val skipCompilationOfTargets = kotlin.targets.matching { it.platformType.toString() == "native" }.names
	all { 
		val target = name.removePrefix("compileKotlin").decapitalize()
		if (target in skipCompilationOfTargets) {
			actions.clear()
			doLast { 
				val destinationFile = project.buildDir.resolve("classes/kotlin/$target/main/klib/${project.name}.klib")
				destinationFile.parentFile.mkdirs()
				println("Writing a dummy klib to $destinationFile")
				destinationFile.createNewFile()
			}
		}
	}
}