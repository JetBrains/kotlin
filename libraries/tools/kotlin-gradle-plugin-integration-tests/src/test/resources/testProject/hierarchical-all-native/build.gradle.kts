plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
	val mingwTargetName: String = providers.gradleProperty("mingwTargetName").get()
	val linuxTargetName: String = providers.gradleProperty("linuxTargetName").get()
	val macosTargetName: String = providers.gradleProperty("macosTargetName").get()
	val currentHostTargetName: String = providers.gradleProperty("currentHostTargetName").get()

    val mingw = mingwX64(mingwTargetName) { }
    val linux = linuxX64(linuxTargetName) { }
    val macos = macosX64(macosTargetName) { }
    val linuxArm = linuxArm64()

	sourceSets {
		val allNative = create("allNative")
		allNative.dependsOn(getByName("commonMain"))
		listOf(mingw, linux, macos).forEach {
			it.compilations["main"].defaultSourceSet.dependsOn(allNative)
		}

    	val currentHostAndLinux = create("currentHostAndLinux")
        currentHostAndLinux.dependsOn(allNative)

    	configure(listOf(linuxArm, targets.getByName(currentHostTargetName))) {
			compilations["main"].defaultSourceSet.dependsOn(currentHostAndLinux)
    	}
    }
}
