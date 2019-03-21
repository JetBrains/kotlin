import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val appcodeVersion: String by rootProject.extra
val appcodeVersionStrict: Boolean by rootProject.extra
val appcodePlatformDepsDir: File by rootProject.extra
val appcodePluginDir: File by rootProject.extra

// Do not rename, used in pill importer
val projectsToShadow: List<String> by extra(listOf(ultimatePath(":appcode-native")))

val cidrPlugin: Configuration by configurations.creating

dependencies {
    cidrPlugin(ultimateProjectDep(":prepare:cidr-plugin"))
}

val preparePluginXml: Task by preparePluginXml(
        ultimatePath(":appcode-native"),
        appcodeVersion,
        appcodeVersionStrict,
        appcodePluginVersionFull
)

val pluginJar: Task = pluginJar(cidrPlugin, listOf(preparePluginXml), projectsToShadow)

val platformDepsJar: Task by platformDepsJar("AppCode", appcodePlatformDepsDir)

val appcodePlugin: Task by packageCidrPlugin(
        ultimatePath(":appcode-native"),
        appcodePluginDir,
        pluginJar,
        platformDepsJar,
        appcodePlatformDepsDir
)

val appcodeUpdatePluginsXml: Task by cidrUpdatePluginsXml(
        preparePluginXml,
        appcodeHumanFriendlyVersion,
        appcodePluginZipPath,
        appcodeCustomPluginRepoUrl
)
