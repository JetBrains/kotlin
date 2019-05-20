import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.ultimate.preparePluginXml
import java.net.URL

plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extra
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val platformDepsJar: (Project, String, File) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Zip> by cidrPluginTools
val packageCidrPlugin: (Project, String, File, Task, Task, File) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools
val zipCidrPlugin: (Project, Task, File) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Zip> by cidrPluginTools
val cidrUpdatePluginsXml: (Project, Task, String, File, URL) -> NamedDomainObjectContainerCreatingDelegateProvider<Task> by cidrPluginTools

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
    cidrPlugin(project(":kotlin-ultimate:prepare:cidr-plugin"))
    embedded(project(":kotlin-ultimate:ide:clion-native")) { isTransitive = false }
}

val preparePluginXml: Task by preparePluginXml(
        ":kotlin-ultimate:ide:clion-native",
        clionVersion,
        clionVersionStrict,
        clionPluginVersionFull
)

val pluginJarTask: Task = pluginJar(project, cidrPlugin, listOf(preparePluginXml))

val platformDepsJarTask: Task by platformDepsJar(project,"CLion", clionPlatformDepsDir)

val clionPlugin: Task by packageCidrPlugin(
        project,
        ":kotlin-ultimate:ide:clion-native",
        clionPluginDir,
        pluginJarTask,
        platformDepsJarTask,
        clionPlatformDepsDir
)

val zipCLionPlugin: Task by zipCidrPlugin(project, clionPlugin, clionPluginZipPath)

val clionUpdatePluginsXml: Task by cidrUpdatePluginsXml(
        project,
        preparePluginXml,
        clionFriendlyVersion,
        clionPluginZipPath,
        clionCustomPluginRepoUrl
)
