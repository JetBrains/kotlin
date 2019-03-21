import org.jetbrains.kotlin.ultimate.*
import java.net.URL

plugins {
    kotlin("jvm")
}

val clionVersion: String by rootProject.extra
val clionFriendlyVersion: String by rootProject.extra
val clionVersionStrict: Boolean by rootProject.extra
val clionPlatformDepsDir: File by rootProject.extra
val clionPluginDir: File by rootProject.extra
val clionPluginVersionFull: String by rootProject.extra
val clionPluginZipPath: File by rootProject.extra
val clionCustomPluginRepoUrl: URL by rootProject.extra

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

val pluginJar: Task = pluginJar(cidrPlugin, listOf(preparePluginXml), projectsToShadow)

val platformDepsJar: Task by platformDepsJar("CLion", clionPlatformDepsDir)

val clionPlugin: Task by packageCidrPlugin(
        ultimatePath(":clion-native"),
        clionPluginDir,
        pluginJar,
        platformDepsJar,
        clionPlatformDepsDir
)

val zipCLionPlugin: Task by zipCidrPlugin(clionPlugin, clionPluginZipPath)

val clionUpdatePluginsXml: Task by cidrUpdatePluginsXml(
        preparePluginXml,
        clionFriendlyVersion,
        clionPluginZipPath,
        clionCustomPluginRepoUrl
)
