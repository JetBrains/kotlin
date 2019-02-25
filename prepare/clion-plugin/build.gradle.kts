import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val clionVersion: String by rootProject.extra
val clionVersionStrict: Boolean by rootProject.extra
val clionPlatformDepsDir: File by rootProject.extra
val clionPluginDir: File by rootProject.extra

// Do not rename, used in pill importer
val projectsToShadow: List<String> by extra(listOf(ultimatePath(":clion-native")))

val cidrPlugin: Configuration by configurations.creating

dependencies {
    cidrPlugin(ultimateProjectDep(":prepare:cidr-plugin"))
}

val preparePluginXml: Task by preparePluginXml(
        ultimatePath(":clion-native"),
        clionVersion,
        clionVersionStrict,
        clionPluginVersionFull
)

val pluginJar: Task = pluginJar(cidrPlugin, preparePluginXml, projectsToShadow)

val platformDepsJar: Task by platformDepsJar("CLion", clionPlatformDepsDir)

val clionPlugin: Task by packageCidrPlugin(
        ultimatePath(":clion-native"),
        clionPluginDir,
        pluginJar,
        platformDepsJar,
        clionPlatformDepsDir
)
