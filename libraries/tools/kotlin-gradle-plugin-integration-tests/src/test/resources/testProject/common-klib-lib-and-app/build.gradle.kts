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

	// Linux-specific targets – embedded:
	@Suppress("DEPRECATION_ERROR")
	linuxMips32()
	@Suppress("DEPRECATION_ERROR")
	linuxMipsel32()

	// macOS-specific targets - created by the ios() shortcut:
	ios()

	// Windows-specific targets:
	mingwX64()
	@Suppress("DEPRECATION_ERROR")
	mingwX86()

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common"))
			}
		}

		val linuxMain by creating {
			dependsOn(commonMain)
		}


		configure(listOf(linuxX64(), linuxArm64())) {
			compilations["main"].defaultSourceSet.dependsOn(linuxMain)
		}

		val jvmAndJsMain by creating {
			dependsOn(commonMain)
		}

		val jvmMain by getting {
			dependsOn(jvmAndJsMain) 
			dependencies {
				implementation(kotlin("stdlib-jdk8"))
			}
		}

		val jsMain by getting {
			dependsOn(jvmAndJsMain) 
			dependencies {
				implementation(kotlin("stdlib-js"))
			}
		}

		val embeddedMain by creating {
			dependsOn(commonMain)
		}

		@Suppress("DEPRECATION_ERROR")
		configure(listOf(linuxMips32(), linuxMipsel32())) {
			compilations["main"].defaultSourceSet.dependsOn(embeddedMain)
		}

		val windowsMain by creating {
			dependsOn(commonMain)
		}

		@Suppress("DEPRECATION_ERROR")
		configure(listOf(mingwX64(), mingwX86())) {
			compilations["main"].defaultSourceSet.dependsOn(windowsMain)
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