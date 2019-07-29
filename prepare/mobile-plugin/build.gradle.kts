import org.gradle.jvm.tasks.Jar
import java.net.URL

plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extensions
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val preparePluginXml: (Project, String, String, Boolean, String, Boolean) -> Copy by cidrPluginTools
val packageCidrPlugin: (Project, String, File, List<Task>) -> Copy by cidrPluginTools
val zipCidrPlugin: (Project, Task, File) -> Zip by cidrPluginTools
val cidrUpdatePluginsXml: (Project, Task, String, File, URL, URL?) -> Task by cidrPluginTools

val clionRepo: String by rootProject.extra
val clionVersion: String by rootProject.extra
val clionFriendlyVersion: String by rootProject.extra
val clionVersionStrict: Boolean by rootProject.extra
val mobilePluginDir: File by rootProject.extra
val mobilePluginVersionFull: String by rootProject.extra
val mobilePluginZipPath: File by rootProject.extra
val mobileCustomPluginRepoUrl: URL by rootProject.extra
val clionJavaPluginDownloadUrl: URL? by rootProject.extra

val cidrPlugin: Configuration by configurations.creating

dependencies {
    cidrPlugin(project(":kotlin-ultimate:prepare:cidr-plugin"))
    embedded(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:mobile-native")) { isTransitive = false }
    embedded("com.android.tools.ddms:ddmlib:22.0.2")
}

val preparePluginXmlTask: Task = preparePluginXml(
        project,
        ":kotlin-ultimate:ide:mobile-native",
        clionVersion,
        clionVersionStrict,
        mobilePluginVersionFull,
        true
)

val pluginJarTask: Task = pluginJar(project, cidrPlugin, listOf(preparePluginXmlTask))

val mobilePluginTask: Task = packageCidrPlugin(
        project,
        ":kotlin-ultimate:ide:mobile-native",
        mobilePluginDir,
        listOf(pluginJarTask)
)

val zipMobilePluginTask: Task = zipCidrPlugin(project, mobilePluginTask, mobilePluginZipPath)

val mobileUpdatePluginsXmlTask: Task = cidrUpdatePluginsXml(
        project,
        preparePluginXmlTask,
        clionFriendlyVersion,
        mobilePluginZipPath,
        mobileCustomPluginRepoUrl,
        clionJavaPluginDownloadUrl
)
