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

	ios()

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common"))
			}
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
	}
}

publishing {
	repositories {
		maven("$rootDir/repo")
	}
}

tasks {
	val skipCompilationOfTargets = setOf(
		"iosArm64",
		"iosX64"
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