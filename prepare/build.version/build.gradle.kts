@file:Suppress("HasPlatformType")

import java.io.File

val buildVersionFilePath = "$buildDir/build.txt"
val buildVersion by configurations.creating
val buildNumber: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

val writeBuildNumber by tasks.registering {
    val versionFile = File(buildVersionFilePath)
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

fun replaceVersion(versionFile: File, versionPattern: String, replacement: (MatchResult) -> String) {
    check(versionFile.isFile) { "Version file $versionFile is not found" }
    val text = versionFile.readText()
    val pattern = Regex(versionPattern)
    val match = pattern.find(text) ?: error("Version pattern is missing in file $versionFile")
    val newValue = replacement(match)
    versionFile.writeText(text.replaceRange(match.groups[1]!!.range, newValue))
}

val writeStdlibVersion by tasks.registering {
    val versionFile = rootDir.resolve("libraries/stdlib/src/kotlin/util/KotlinVersion.kt")
    inputs.property("version", kotlinVersion)
    outputs.file(versionFile)
    doLast {
        replaceVersion(versionFile, """val CURRENT: KotlinVersion = KotlinVersion\((\d+, \d+, \d+)\)""") {
            val (major, minor, _, optPatch) = Regex("""^(\d+)\.(\d+)(\.(\d+))?""").find(kotlinVersion)?.destructured ?: error("Cannot parse current version $kotlinVersion")
            val newVersion = "$major, $minor, ${optPatch.takeIf { it.isNotEmpty() } ?: "0" }"
            logger.lifecycle("Writing new standard library version components: $newVersion")
            newVersion
        }
    }
}

val writePluginVersion by tasks.registering {
    val versionFile = project(":idea").projectDir.resolve("resources/META-INF/plugin.xml")
    val pluginVersion = rootProject.findProperty("pluginVersion") as String?
    inputs.property("version", pluginVersion)
    outputs.file(versionFile)
    doLast {
        requireNotNull(pluginVersion) { "Specify 'pluginVersion' property" }
        replaceVersion(versionFile, """<version>([^<]+)</version>""") {
            logger.lifecycle("Writing new plugin version: $pluginVersion")
            pluginVersion!!
        }
    }
}

val writeVersions by tasks.registering {
    dependsOn(writeBuildNumber, writeStdlibVersion)
}
