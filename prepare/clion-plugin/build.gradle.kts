import org.gradle.jvm.tasks.Jar
import java.net.URL

plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extensions
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val preparePluginXml: (Project, String, String, Boolean, String) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools
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

val preparePluginXmlTask: Task by preparePluginXml(
        project,
        ":kotlin-ultimate:ide:clion-native",
        clionVersion,
        clionVersionStrict,
        clionPluginVersionFull
)

val pluginJarTask: Task = pluginJar(project, cidrPlugin, listOf(preparePluginXmlTask))

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
        preparePluginXmlTask,
        clionFriendlyVersion,
        clionPluginZipPath,
        clionCustomPluginRepoUrl
)
