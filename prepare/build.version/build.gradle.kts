@file:Suppress("HasPlatformType")

import java.io.File

val buildVersionFilePath = "$buildDir/build.txt"
val buildVersion by configurations.creating
val buildNumber: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

val writeBuildNumber by tasks.registering {
    val versionFile = File(buildVersionFilePath)
    val buildNumber = buildNumber
    inputs.property("version", buildNumber)
    outputs.file(versionFile)
    doLast {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(buildNumber)
    }
}

artifacts.add(buildVersion.name, file(buildVersionFilePath)) {
    builtBy(writeBuildNumber)
}



val writeStdlibVersion by tasks.registering {
    val kotlinVersionLocal = kotlinVersion
    val versionFile = rootDir.resolve("libraries/stdlib/src/kotlin/util/KotlinVersion.kt")
    inputs.property("version", kotlinVersionLocal)
    outputs.file(versionFile)

    fun replaceVersion(versionFile: File, versionPattern: String, replacement: (MatchResult) -> String) {
        check(versionFile.isFile) { "Version file $versionFile is not found" }
        val text = versionFile.readText()
        val pattern = Regex(versionPattern)
        val match = pattern.find(text) ?: error("Version pattern is missing in file $versionFile")
        val newValue = replacement(match)
        versionFile.writeText(text.replaceRange(match.groups[1]!!.range, newValue))
    }

    doLast {
        replaceVersion(versionFile, """fun get\(\): KotlinVersion = KotlinVersion\((\d+, \d+, \d+)\)""") {
            val (major, minor, _, optPatch) = Regex("""^(\d+)\.(\d+)(\.(\d+))?""").find(kotlinVersionLocal)?.destructured ?: error("Cannot parse current version $kotlinVersionLocal")
            val newVersion = "$major, $minor, ${optPatch.takeIf { it.isNotEmpty() } ?: "0" }"
            logger.lifecycle("Writing new standard library version components: $newVersion")
            newVersion
        }
    }
}

val writePluginVersion by tasks.registering // Remove this task after removing usages in the TeamCity build

val writeVersions by tasks.registering {
    dependsOn(writeBuildNumber, writeStdlibVersion)
}
