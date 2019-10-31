plugins {
    kotlin("jvm")
}

val artifactsForCidrDir: File by rootProject.extra
val mobileMppPluginDir: File by rootProject.extra

val mainModule = ":kotlin-ultimate:ide:android-studio-native"

dependencies {
    embedded(project(mainModule)) { isTransitive = false }
}

val mobileMppPluginTask: Copy = task<Copy>("mobileMppPlugin") {
    into(mobileMppPluginDir)

    into("lib") {
        val jarTask = project("$mainModule").tasks.findByName("jar")!!
        dependsOn(jarTask)
        from(jarTask.outputs.files.singleFile)
    }
}

val zipMobileMppPluginTask: Zip = task<Zip>("zipMobileMppPlugin") {
    val pluginNumber: String = findProperty("mobileMppPluginNumber")?.toString() ?: "SNAPSHOT"
    val destinationFile: File = artifactsForCidrDir.resolve("mobile-mpp-plugin-$pluginNumber.zip").canonicalFile

    destinationDirectory.set(destinationFile.parentFile)
    archiveFileName.set(destinationFile.name)

    from(mobileMppPluginTask)
    into("mobile-mpp")

    doLast {
        logger.lifecycle("Mobile MPP plugin artifacts are packed to $destinationFile")
    }
}

fun addPlugin(oldArgs: List<String>, pluginLocation: String): List<String> {
    for (oldArg in oldArgs) {
        if (oldArg.startsWith("-Dplugin.path=")) {
            return listOf("$oldArg,$pluginLocation")
        }
    }

    return listOf("-Dplugin.path=$pluginLocation")
}

if (rootProject.findProperty("versions.androidStudioRelease") != null) {
    (rootProject.tasks.findByPath("idea-runner:runIde") as? JavaExec)?.apply {
        dependsOn(":kotlin-ultimate:prepare:mobile-mpp-plugin:mobileMppPlugin")
        this.setJvmArgs(addPlugin(this.getAllJvmArgs(), mobileMppPluginDir.absolutePath))
    }
}
