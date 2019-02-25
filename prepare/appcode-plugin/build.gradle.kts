import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val appcodeVersion: String by rootProject.extra
val appcodeVersionStrict: Boolean by rootProject.extra
val appcodePlatformDepsDir: File by rootProject.extra
val appcodePluginDir: File by rootProject.extra

// Do not rename, used in pill importer
val projectsToShadow by extra(listOf(ultimatePath(":appcode-native")))

val cidrPlugin by configurations.creating

dependencies {
    cidrPlugin(ultimateProjectDep(":prepare:cidr-plugin"))
}

val preparePluginXml by preparePluginXml(
        ultimatePath(":appcode-native"),
        appcodeVersion,
        appcodeVersionStrict,
        appcodePluginVersionFull
)

val pluginJar = pluginJar(cidrPlugin, preparePluginXml, projectsToShadow)

val platformDepsJar by platformDepsJar("AppCode", appcodePlatformDepsDir)

val appcodePlugin by packageCidrPlugin(
        ultimatePath(":appcode-native"),
        appcodePluginDir,
        pluginJar,
        platformDepsJar,
        appcodePlatformDepsDir
)
