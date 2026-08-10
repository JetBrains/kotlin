@file:Suppress("HasPlatformType")

import java.io.File

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
}

val buildVersionFilePath = layout.buildDirectory.file("build.txt")
val buildVersion = configurations.create("buildVersion")

val writeBuildNumber = tasks.register("writeBuildNumber") {
    val versionFile = buildVersionFilePath
    val buildNumber = project.kotlinBuildProperties.buildNumber
    inputs.property("version", buildNumber)
    outputs.file(versionFile)
    doLast {
        with(versionFile.get().asFile) {
            parentFile.mkdirs()
            writeText(buildNumber.get())
        }
    }
}

artifacts.add(buildVersion.name, buildVersionFilePath) {
    builtBy(writeBuildNumber)
}



val writeStdlibVersion = tasks.register("writeStdlibVersion") {
    val kotlinVersionLocal = kotlinBuildProperties.kotlinVersion
    val versionFile = rootDir.resolve("libraries/stdlib/src/kotlin/util/KotlinVersion.kt")
    inputs.property("version", kotlinVersionLocal)
    outputs.file(versionFile)

    fun Task.replaceVersion(versionFile: File, versionPattern: String, replacement: (MatchResult) -> String) {
        check(versionFile.isFile) { "Version file $versionFile is not found" }
        val text = versionFile.readText()
        val pattern = Regex(versionPattern)
        val match = pattern.find(text) ?: error("Version pattern is missing in file $versionFile")
        val group = match.groups[1]!!
        val newValue = replacement(match)
        if (newValue != group.value) {
            logger.lifecycle("Writing new standard library version components: $newValue (was: ${group.value})")
            versionFile.writeText(text.replaceRange(group.range, newValue))
        } else {
            logger.info("Standard library version components: ${group.value}")
        }
    }

    doLast {
        replaceVersion(versionFile, """fun get\(\): KotlinVersion = KotlinVersion\((\d+, \d+, \d+)\)""") {
            val (major, minor, _, optPatch) = Regex("""^(\d+)\.(\d+)(\.(\d+))?""").find(kotlinVersionLocal.get())?.destructured ?: error("Cannot parse current version ${kotlinVersionLocal.get()}")
            "$major, $minor, ${optPatch.takeIf { it.isNotEmpty() } ?: "0" }"
        }
    }
}

val writePluginVersion = tasks.register("writePluginVersion") // Remove this task after removing usages in the TeamCity build

val writeVersions = tasks.register("writeVersions") {
    dependsOn(writeBuildNumber, writeStdlibVersion)
}
