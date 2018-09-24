
import java.io.File

val buildVersionFilePath = "${rootProject.extra["distDir"]}/build.txt"

val buildVersion by configurations.creating
val buildNumber: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

val writeBuildNumber by tasks.creating {
    val versionFile = File(buildVersionFilePath)
    inputs.property("version", buildNumber)
    outputs.file(versionFile)
    doLast {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(buildNumber)
    }
}

fun replaceVersion(versionFile: File, versionPattern: String, replacement: (MatchResult) -> String) {
    check(versionFile.isFile) { "Version file $versionFile is not found" }
    val text = versionFile.readText()
    val pattern = Regex(versionPattern)
    val match = pattern.find(text) ?: error("Version pattern is missing in file $versionFile")
    val newValue = replacement(match)
    versionFile.writeText(text.replaceRange(match.groups[1]!!.range, newValue))
}

val writeStdlibVersion by tasks.creating {
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

val writeCompilerVersion by tasks.creating {
    val versionFile = rootDir.resolve("core/util.runtime/src/org/jetbrains/kotlin/config/KotlinCompilerVersion.java")
    inputs.property("version", kotlinVersion)
    outputs.file(versionFile)
    doLast {
        replaceVersion(versionFile, """public static final String VERSION = "([^"]+)"""") {
            logger.lifecycle("Writing new compiler version: $kotlinVersion")
            kotlinVersion
        }
    }
}

val writePluginVersion by tasks.creating {
    val versionFile = project(":idea").projectDir.resolve("src/META-INF/plugin.xml")
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

val writeVersions by tasks.creating {
    dependsOn(writeBuildNumber, writeStdlibVersion, writeCompilerVersion)
}


artifacts.add(buildVersion.name, file(buildVersionFilePath)) {
    builtBy(writeBuildNumber)
}

val distKotlinHomeDir: String by rootProject.extra

val dist by task<Copy> {
    from(writeBuildNumber)
    into(File(distKotlinHomeDir))
}
