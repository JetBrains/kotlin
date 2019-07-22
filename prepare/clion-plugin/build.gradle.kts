import org.gradle.jvm.tasks.Jar
import java.net.URL

plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extensions
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val preparePluginXml: (Project, String, String, Boolean, String, Boolean) -> Copy by cidrPluginTools
val patchedPlatformDepsJar: (Project, File) -> Zip by cidrPluginTools
val otherPlatformDepsJars: (Project, File) -> Task by cidrPluginTools
val packageCidrPlugin: (Project, String, File, List<Task>) -> Copy by cidrPluginTools
val zipCidrPlugin: (Project, Task, File) -> Zip by cidrPluginTools
val cidrUpdatePluginsXml: (Project, Task, String, File, URL, URL?) -> Task by cidrPluginTools

val clionRepo: String by rootProject.extra
val clionVersion: String by rootProject.extra
val clionFriendlyVersion: String by rootProject.extra
val clionVersionStrict: Boolean by rootProject.extra
val clionPlatformDepsOrJavaPluginDir: File by rootProject.extra
val clionPluginDir: File by rootProject.extra
val clionPluginVersionFull: String by rootProject.extra
val clionPluginZipPath: File by rootProject.extra
val clionCustomPluginRepoUrl: URL by rootProject.extra
val clionUseJavaPlugin: Boolean by rootProject.extra
val clionJavaPluginDownloadUrl: URL? by rootProject.extra

val cidrPlugin: Configuration by configurations.creating

dependencies {
    cidrPlugin(project(":kotlin-ultimate:prepare:cidr-plugin"))
    embedded(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:clion-native")) { isTransitive = false }
}

val preparePluginXmlTask: Task = preparePluginXml(
        project,
        ":kotlin-ultimate:ide:clion-native",
        clionVersion,
        clionVersionStrict,
        clionPluginVersionFull,
        clionUseJavaPlugin
)

val pluginJarTask: Task = pluginJar(project, cidrPlugin, listOf(preparePluginXmlTask))

val jarTasks = if (clionUseJavaPlugin)
    listOf(pluginJarTask)
else {
    val patchedPlatformDepsJar: Task = patchedPlatformDepsJar(project, clionPlatformDepsOrJavaPluginDir)
    val otherPlatformDepsJars: Task = otherPlatformDepsJars(project, clionPlatformDepsOrJavaPluginDir)
    listOf(pluginJarTask, patchedPlatformDepsJar, otherPlatformDepsJars)
}

val clionPluginTask: Task = packageCidrPlugin(
        project,
        ":kotlin-ultimate:ide:clion-native",
        clionPluginDir,
        jarTasks
)

val zipCLionPluginTask: Task = zipCidrPlugin(project, clionPluginTask, clionPluginZipPath)

val clionUpdatePluginsXmlTask: Task = cidrUpdatePluginsXml(
        project,
        preparePluginXmlTask,
        clionFriendlyVersion,
        clionPluginZipPath,
        clionCustomPluginRepoUrl,
        clionJavaPluginDownloadUrl
)
