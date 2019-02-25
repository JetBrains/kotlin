import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val clionVersion: String by rootProject.extra
val clionVersionStrict: Boolean by rootProject.extra
val clionPlatformDepsDir: File by rootProject.extra
val clionPluginDir: File by rootProject.extra

// Do not rename, used in pill importer
val projectsToShadow by extra(listOf(ultimatePath(":clion-native")))

val cidrPlugin by configurations.creating

dependencies {
    cidrPlugin(ultimateProjectDep(":prepare:cidr-plugin"))
}

val preparePluginXml by preparePluginXml(
        ultimatePath(":clion-native"),
        clionVersion,
        clionVersionStrict,
        clionPluginVersionFull
)

val pluginJar = pluginJar(cidrPlugin, preparePluginXml, projectsToShadow)

val platformDepsJar by platformDepsJar("CLion", clionPlatformDepsDir)

val clionPlugin by packageCidrPlugin(
        ultimatePath(":clion-native"),
        clionPluginDir,
        pluginJar,
        platformDepsJar,
        clionPlatformDepsDir
)
