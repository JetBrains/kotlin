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
val packageCidrPlugin: (Project, String, File, List<Any>) -> Copy by cidrPluginTools
val zipCidrPlugin: (Project, Task, File) -> Zip by cidrPluginTools
val cidrUpdatePluginsXml: (Project, Task, String, File, URL, URL?) -> Task by cidrPluginTools

val clionRepo: String by rootProject.extra
val clionVersion: String by rootProject.extra
val clionFriendlyVersion: String by rootProject.extra
val clionVersionStrict: Boolean by rootProject.extra
val clionPlatformDepsOrJavaPluginDir: File by rootProject.extra
val clionPluginVersionFull: String by rootProject.extra
val clionPluginZipPath: File by rootProject.extra
val clionCustomPluginRepoUrl: URL by rootProject.extra
val clionJavaPluginDownloadUrl: URL? by rootProject.extra
val cidrPluginDir: File by rootProject.extra

val cidrPlugin: Configuration by configurations.creating
val cidrGradleTooling: Configuration by configurations.creating

dependencies {
    cidrPlugin(project(":kotlin-ultimate:prepare:common-cidr-plugin"))
    cidrGradleTooling(project(":kotlin-ultimate:ide:cidr-gradle-tooling"))
    embedded(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:common-cidr-native")) { isTransitive = false }
}

val preparePluginXmlTask: Task = preparePluginXml(
        project,
        ":kotlin-ultimate:ide:common-cidr-native",
        clionVersion,
        clionVersionStrict,
        clionPluginVersionFull,
        true
)

val pluginJarTask: Task = pluginJar(project, cidrPlugin, listOf(preparePluginXmlTask))

val additionalJars = listOf(pluginJarTask, cidrGradleTooling)

val cidrPluginTask: Task = packageCidrPlugin(
        project,
        ":kotlin-ultimate:ide:common-cidr-native",
        cidrPluginDir,
        additionalJars
)

val zipCidrPluginTask: Task = zipCidrPlugin(project, cidrPluginTask, clionPluginZipPath)

val cidrUpdatePluginsXmlTask: Task = cidrUpdatePluginsXml(
        project,
        preparePluginXmlTask,
        clionFriendlyVersion,
        clionPluginZipPath,
        clionCustomPluginRepoUrl,
        clionJavaPluginDownloadUrl
)