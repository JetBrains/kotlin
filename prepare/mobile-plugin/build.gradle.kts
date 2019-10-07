
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
val clionCocoaCommonBinariesDir: File by rootProject.extra

val ultimateTools: Map<String, Any> by rootProject.extensions
val handleSymlink: (FileCopyDetails, File) -> Boolean by ultimateTools

val cidrPlugin: Configuration by configurations.creating

repositories {
    maven("https://maven.google.com")
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
}

dependencies {
    cidrPlugin(project(":kotlin-ultimate:prepare:cidr-plugin"))
    embedded(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    runtime(project(":kotlin-ultimate:ide:mobile-native")) { isTransitive = false } // we need our own jar, so we can register additional gradle model builder service
    runtime("com.jetbrains.intellij.cidr:cidr-cocoa-common:$clionVersion") { isTransitive = false }
    runtime("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$clionVersion") { isTransitive = false }
    runtime("com.jetbrains.intellij.cidr:cidr-xctest:$clionVersion") { isTransitive = false }
    runtime("com.android.tools.ddms:ddmlib:26.0.0")
    runtime(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api")) { isTransitive = false }
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

val copyNativeDeps: Task by tasks.creating(Copy::class) {
    from(clionCocoaCommonBinariesDir)
    val targetDir = File(mobilePluginDir, "native/mac")
    into(targetDir)
    eachFile {
        handleSymlink(this, targetDir)
    }
}

val copyRuntimeDeps: Task by tasks.creating(Copy::class) {
    from(configurations.runtime)
    into(File(mobilePluginDir, "lib"))
}

val mobilePluginTask: Task = packageCidrPlugin(
        project,
        ":kotlin-ultimate:ide:mobile-native",
        mobilePluginDir,
        listOf(pluginJarTask)
)
mobilePluginTask.dependsOn(
    copyNativeDeps,
    copyRuntimeDeps
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
