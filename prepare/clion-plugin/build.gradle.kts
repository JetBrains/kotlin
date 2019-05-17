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

val cidrPlugin: Configuration by configurations.creating

dependencies {
    cidrPlugin(project(ultimatePath(":prepare:cidr-plugin")))
    embedded(project(ultimatePath(":ide:clion-native"))) { isTransitive = false }
}

val preparePluginXml: Task by preparePluginXml(
        ultimatePath(":ide:clion-native"),
        clionVersion,
        clionVersionStrict,
        clionPluginVersionFull
)

val pluginJar: Task = pluginJar(cidrPlugin, listOf(preparePluginXml))

val platformDepsJar: Task by platformDepsJar("CLion", clionPlatformDepsDir)

val clionPlugin: Task by packageCidrPlugin(
        ultimatePath(":ide:clion-native"),
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
