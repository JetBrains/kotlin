plugins {
	kotlin("multiplatform").version("<pluginMarkerVersion>")
	id("maven-publish")
}

group = "com.example"
version = "1.0"

repositories {
	mavenLocal()
	jcenter()
}

kotlin {
	jvm()
	js()

	linuxX64()
	linuxArm64()

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common"))
			}
		}

		val linuxMain by creating {
			dependsOn(commonMain)
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

		configure(listOf(linuxX64(), linuxArm64())) {
			compilations["main"].defaultSourceSet.dependsOn(linuxMain)
		}
	}
}

publishing {
	repositories {
		maven("$rootDir/repo")
	}
}

tasks {
	val skipCompilationOfTargets = setOf(
		"linuxX64",
		"linuxArm64"
	)
	all { 
		val target = name.removePrefix("compileKotlin").decapitalize()
		if (target in skipCompilationOfTargets) {
			actions.clear()
			doLast { 
				val destinationFile = project.buildDir.resolve("classes/kotlin/$target/main/${project.name}.klib")
				destinationFile.parentFile.mkdirs()
				println("Writing a dummy klib to $destinationFile")
				destinationFile.createNewFile()
			}
		}
	}
}