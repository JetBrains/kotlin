plugins {
    kotlin("jvm")
}

val artifactsForCidrDir: File by rootProject.extra
val clionCocoaCommonBinariesDir: File by rootProject.extra
val kmmPluginDir: File by rootProject.extra
val lldbFrontendMacosDir: File by rootProject.extra
val lldbFrameworkDir: File by rootProject.extra

val ultimateTools: Map<String, Any> by rootProject.extensions
val handleSymlink: (FileCopyDetails, File) -> Boolean by ultimateTools

val mainModule = ":kotlin-ultimate:ide:android-studio-native"
val binariesDir = "bin"

dependencies {
    embedded(project(mainModule)) { isTransitive = false }
}

val copyAppCodeBinaries: Task by tasks.creating(Copy::class) {
    val targetDir = File(kmmPluginDir, binariesDir)

    from(clionCocoaCommonBinariesDir)
    eachFile {
        handleSymlink(this, targetDir)
    }
    into(targetDir)
}

val copyAppCodeModules: Task by tasks.creating(Copy::class) {
    into(File(kmmPluginDir, "lib"))
    project("$mainModule").configurations["compileClasspath"].forEach {
        if (it.name.startsWith("cidr")) {
            from(it.absolutePath)
        }
    }
}

val copyLLDBFrontend: Task by tasks.creating(Copy::class) {
    from(lldbFrontendMacosDir)
    into(File(kmmPluginDir, binariesDir))
}

val copyLLDBFramework: Task by tasks.creating(Copy::class) {
    from(lldbFrameworkDir)
    into(File(kmmPluginDir, binariesDir))
}

val kmmPluginTask: Copy = task<Copy>("kmmPlugin") {
    dependsOn(
        copyLLDBFramework,
        copyLLDBFrontend,
        copyAppCodeBinaries,
        copyAppCodeModules
    )
    val jarTask = project(mainModule).tasks.findByName("jar")!!
    dependsOn(jarTask)

    from(jarTask.outputs.files.singleFile)
    into(File(kmmPluginDir, "lib"))
}

val zipKmmPluginTask: Zip = task<Zip>("zipKmmPlugin") {
    dependsOn(kmmPluginTask)
    val kmmPluginVersion: String = findProperty("kmmPluginVersion")?.toString() ?: "SNAPSHOT"
    val destinationFile: File = artifactsForCidrDir.resolve("kmm-plugin-$kmmPluginVersion.zip").canonicalFile

    destinationDirectory.set(destinationFile.parentFile)
    archiveFileName.set(destinationFile.name)

    from(kmmPluginDir)
    into("kmm")

    doLast {
        logger.lifecycle("KMM plugin artifacts are packed to $destinationFile")
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
        dependsOn(":kotlin-ultimate:prepare:kmm-plugin:kmmPlugin")
        this.setJvmArgs(addPlugin(this.allJvmArgs, kmmPluginDir.absolutePath))
    }
}