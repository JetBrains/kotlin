import org.jetbrains.kotlin.ultimate.*
import java.net.URL

plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extra
val enableTasksIfAtLeast: (Project, String, Int) -> Unit by ultimateTools
val enableTasksIfOsIsNot: (Project, List<String>) -> Unit by ultimateTools

val cidrPluginTools: Map<String, Any> by rootProject.extra
val packageCidrPlugin: (Project, String, File, Task, Task, File) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools
val zipCidrPlugin: (Project, Task, File) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Zip> by cidrPluginTools
val cidrUpdatePluginsXml: (Project, Task, String, File, URL) -> NamedDomainObjectContainerCreatingDelegateProvider<Task> by cidrPluginTools

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
    cidrPlugin(project(":kotlin-ultimate:prepare:cidr-plugin"))
    embedded(project(":kotlin-ultimate:ide:appcode-native")) { isTransitive = false }
}

val preparePluginXml: Task by preparePluginXml(
        ":kotlin-ultimate:ide:appcode-native",
        appcodeVersion,
        appcodeVersionStrict,
        appcodePluginVersionFull
)

val pluginJar: Task = pluginJar(cidrPlugin, listOf(preparePluginXml))

val platformDepsJar: Task by platformDepsJar("AppCode", appcodePlatformDepsDir)

val appcodePlugin: Task by packageCidrPlugin(
        project,
        ":kotlin-ultimate:ide:appcode-native",
        appcodePluginDir,
        pluginJar,
        platformDepsJar,
        appcodePlatformDepsDir
)

val zipAppCodePlugin: Task by zipCidrPlugin(project, appcodePlugin, appcodePluginZipPath)

val appcodeUpdatePluginsXml: Task by cidrUpdatePluginsXml(
        project,
        preparePluginXml,
        appcodeFriendlyVersion,
        appcodePluginZipPath,
        appcodeCustomPluginRepoUrl
)

enableTasksIfAtLeast(project, appcodeVersion, 191)
enableTasksIfOsIsNot(project, listOf("Windows"))
