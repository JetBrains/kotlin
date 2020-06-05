plugins {
    kotlin("jvm")
}

val artifactsForCidrDir: File by rootProject.extra
val clionCocoaCommonBinariesDir: File by rootProject.extra
val mobileMppPluginDir: File by rootProject.extra
val lldbFrontendMacosDir: File by rootProject.extra
val lldbFrameworkDir: File by rootProject.extra

val ultimateTools: Map<String, Any> by rootProject.extensions
val handleSymlink: (FileCopyDetails, File) -> Boolean by ultimateTools

val mainModule = ":kotlin-ultimate:ide:android-studio-native"
val binariesDir = "bin"

val kotlinVersion: String by rootProject.extra
val mobilePluginVersion: String = findProperty("mobilePluginDeployVersion")?.toString() ?: "0.1-SNAPSHOT"

dependencies {
    embedded(project(mainModule)) { isTransitive = false }
}

val copyAppCodeBinaries: Task by tasks.creating(Copy::class) {
    val targetDir = File(mobileMppPluginDir, binariesDir)

    from(clionCocoaCommonBinariesDir)
    eachFile {
        handleSymlink(this, targetDir)
    }
    into(targetDir)
}

val copyAppCodeModules: Task by tasks.creating(Copy::class) {
    into(File(mobileMppPluginDir, "lib"))
    project("$mainModule").configurations["compileClasspath"].forEach {
        if (it.name.startsWith("cidr")) {
            from(it.absolutePath)
        }
    }
}

val copyLLDBFrontend: Task by tasks.creating(Copy::class) {
    from(lldbFrontendMacosDir)
    into(File(mobileMppPluginDir, binariesDir))
}

val copyLLDBFramework: Task by tasks.creating(Copy::class) {
    from(lldbFrameworkDir)
    into(File(mobileMppPluginDir, binariesDir))
}

val mobileMppPluginTask: Copy = task<Copy>("mobileMppPlugin") {
    dependsOn(
        copyLLDBFramework,
        copyLLDBFrontend,
        copyAppCodeBinaries,
        copyAppCodeModules
    )
    val jarTask = project("$mainModule").tasks.findByName("jar")!!
    dependsOn(jarTask)

    from(jarTask.outputs.files.singleFile)
    into(File(mobileMppPluginDir, "lib"))
}

val zipMobileMppPluginTask: Zip = task<Zip>("zipMobileMppPlugin") {
    dependsOn(mobileMppPluginTask)
    val pluginNumber: String = findProperty("mobileMppPluginNumber")?.toString() ?: "SNAPSHOT"
    val destinationFile: File = artifactsForCidrDir.resolve("mobile-mpp-plugin-$pluginNumber.zip").canonicalFile

    destinationDirectory.set(destinationFile.parentFile)
    archiveFileName.set(destinationFile.name)

    from(mobileMppPluginDir)
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

fun replaceVersion(versionFile: java.io.File, versionPattern: String, replacement: (MatchResult) -> String) {
    check(versionFile.isFile) { "Version file $versionFile is not found" }
    val text = versionFile.readText()
    val pattern = Regex(versionPattern)
    val match = pattern.find(text) ?: error("Version pattern is missing in file $versionFile")
    val newValue = replacement(match)
    versionFile.writeText(text.replaceRange(match.groups[1]!!.range, newValue))
}

val writePluginVersion by tasks.registering {
    val versionFile = project(":kotlin-ultimate:ide:android-studio-native")
        .projectDir
        .resolve("src/com/jetbrains/mpp/versions/VersionsUtils.kt")

    inputs.property("version", mobilePluginVersion)
    inputs.property("kotlinVersion", kotlinVersion)
    outputs.file(versionFile)

    doLast {
        requireNotNull(mobilePluginVersion) { "Specify 'pluginVersion' property" }

        replaceVersion(versionFile, """const val mobilePluginVersion: String = "([^"]+)"""") {
            mobilePluginVersion
        }

        replaceVersion(versionFile, """const val compiledAgainstKotlin: String = "([^"]+)"""") {
            kotlinVersion
        }
    }
}