import org.gradle.jvm.tasks.Jar
import java.net.URL
import java.util.*

plugins {
    kotlin("jvm")
}

repositories {
    ivy {
        url = uri("https://buildserver.labs.intellij.net/guestAuth/repository/download")
        patternLayout {
            ivy("[module]/[revision]/teamcity-ivy.xml")
            artifact("[module]/[revision]/[artifact](.[ext])")
        }
    }
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val ijProductBranch: (String) -> Int by ultimateTools
val disableBuildTasks: Project.(String) -> Unit by ultimateTools
val addKotlinNativeDeps: (Project, String) -> Unit by ultimateTools

val cidrPluginTools: Map<String, Any> by rootProject.extensions
val preparePluginXml: (Project, String, String, Boolean, String) -> Copy by cidrPluginTools
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val packageCidrPlugin: (Project, String, File, List<Any>) -> Copy by cidrPluginTools
val zipCidrPlugin: (Project, Task, File) -> Zip by cidrPluginTools
val cidrUpdatePluginsXml: (Project, Task, String, File, URL, URL) -> Task by cidrPluginTools

val appcodeVersion: String by rootProject.extra
val appcodeFriendlyVersion: String by rootProject.extra
val appcodeVersionStrict: Boolean by rootProject.extra
val appcodePluginDir: File by rootProject.extra
val appcodePluginVersionFull: String by rootProject.extra
val appcodePluginZipPath: File by rootProject.extra
val appcodeCustomPluginRepoUrl: URL by rootProject.extra
val appcodeJavaPluginDownloadUrl: URL by rootProject.extra

val cidrPlugin: Configuration by configurations.creating
val cidrGradleTooling: Configuration by configurations.creating

dependencies {
    cidrPlugin(project(":kotlin-ultimate:prepare:cidr-plugin"))
    cidrGradleTooling(project(":kotlin-ultimate:ide:cidr-gradle-tooling"))
    embedded(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:common-cidr-swift-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:appcode-native")) { isTransitive = false }
    addKotlinNativeDeps(project, "runtime")
}

val copyRuntimeDeps: Task by tasks.creating(Copy::class) {
    from(configurations.runtime)
    into(temporaryDir)
}

val preparePluginXmlTask: Task = preparePluginXml(
    project,
    ":kotlin-ultimate:ide:appcode-native",
    appcodeVersion,
    appcodeVersionStrict,
    appcodePluginVersionFull
)

val pluginJarTask: Task = pluginJar(project, cidrPlugin, listOf(preparePluginXmlTask))

val additionalJars = listOf(pluginJarTask, copyRuntimeDeps, cidrGradleTooling)

val appcodePluginTask: Task = packageCidrPlugin(
        project,
        ":kotlin-ultimate:ide:appcode-native",
        appcodePluginDir,
        additionalJars
)

val zipAppCodePluginTask: Task = zipCidrPlugin(project, appcodePluginTask, appcodePluginZipPath)

val appcodeUpdatePluginsXmlTask: Task = cidrUpdatePluginsXml(
        project,
        preparePluginXmlTask,
        appcodeFriendlyVersion,
        appcodePluginZipPath,
        appcodeCustomPluginRepoUrl,
        appcodeJavaPluginDownloadUrl
)

if (ijProductBranch(appcodeVersion) < 193)
    disableBuildTasks("Too old AppCode version: $appcodeVersion")
else
    System.getProperty("os.name")!!.toLowerCase(Locale.US).takeIf { "windows" in it }?.let {
        disableBuildTasks("Can't build AppCode plugin under Windows")
    }
