import org.jetbrains.kotlin.ultimate.*
import java.net.URL

plugins {
    kotlin("jvm")
}

val appcodeVersion: String by rootProject.extra
val appcodeFriendlyVersion: String by rootProject.extra
val appcodeVersionStrict: Boolean by rootProject.extra
val appcodePlatformDepsDir: File by rootProject.extra
val appcodePluginDir: File by rootProject.extra
val appcodePluginVersionFull: String by rootProject.extra
val appcodePluginZipPath: File by rootProject.extra
val appcodeCustomPluginRepoUrl: URL by rootProject.extra

val cidrPlugin: Configuration by configurations.creating

dependencies {
    cidrPlugin(ultimateProjectDep(":prepare:cidr-plugin"))
    embedded(ultimateProjectDep(":ide:appcode-native")) { isTransitive = false }
}

val preparePluginXml: Task by preparePluginXml(
        ultimatePath(":ide:appcode-native"),
        appcodeVersion,
        appcodeVersionStrict,
        appcodePluginVersionFull
)

val pluginJar: Task = pluginJar(cidrPlugin, listOf(preparePluginXml))

val platformDepsJar: Task by platformDepsJar("AppCode", appcodePlatformDepsDir)

val appcodePlugin: Task by packageCidrPlugin(
        ultimatePath(":ide:appcode-native"),
        appcodePluginDir,
        pluginJar,
        platformDepsJar,
        appcodePlatformDepsDir
)

val zipAppCodePlugin: Task by zipCidrPlugin(appcodePlugin, appcodePluginZipPath)

val appcodeUpdatePluginsXml: Task by cidrUpdatePluginsXml(
        preparePluginXml,
        appcodeFriendlyVersion,
        appcodePluginZipPath,
        appcodeCustomPluginRepoUrl
)

enableTasksIfAtLeast(appcodeVersion, 191)
