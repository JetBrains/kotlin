plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
	val mingwTargetName: String by project
	val linuxTargetName: String by project
	val macosTargetName: String by project
	val currentHostTargetName: String by project

    val mingw = mingwX64(mingwTargetName) { }
    val linux = linuxX64(linuxTargetName) { }
    val macos = macosX64(macosTargetName) { }
    val linuxArm = linuxArm64()

	sourceSets {
		val allNative by creating {
			dependsOn(getByName("commonMain"))
			listOf(mingw, linux, macos).forEach {
				it.compilations["main"].defaultSourceSet.dependsOn(this@creating)
			}
		}

    	val currentHostAndLinux by creating {
    		dependsOn(allNative)
    	}

    	configure(listOf(linuxArm, targets.getByName(currentHostTargetName))) {
			compilations["main"].defaultSourceSet.dependsOn(currentHostAndLinux)
    	}
    }
}