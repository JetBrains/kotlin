import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import org.gradle.jvm.tasks.Jar
import java.net.URL

plugins {
    kotlin("jvm")
    id("com.github.jk1.tcdeps") version "1.2"
}

val cidrPluginTools: Map<String, Any> by rootProject.extensions
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val preparePluginXml: (Project, String, String, Boolean, String, Boolean) -> Copy by cidrPluginTools
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
val kotlinNativeBackendVersion: String by rootProject.extra

val ultimateTools: Map<String, Any> by rootProject.extensions
val handleSymlink: (FileCopyDetails, File) -> Boolean by ultimateTools

val cidrPlugin: Configuration by configurations.creating

repositories {
    maven("https://maven.google.com")
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
    }
}

dependencies {
    runtime(project(":kotlin-ultimate:ide:common-cidr-swift-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:mobile-native")) { isTransitive = false }
    runtime("com.jetbrains.intellij.cidr:cidr-cocoa-common:$clionVersion") { isTransitive = false }
    runtime("com.jetbrains.intellij.cidr:cidr-cocoa:$clionVersion") { isTransitive = false }
    runtime("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$clionVersion") { isTransitive = false }
    runtime("com.jetbrains.intellij.cidr:cidr-xctest:$clionVersion") { isTransitive = false }
    runtime("com.android.tools.ddms:ddmlib:26.0.0") {
        exclude("com.google.guava", "guava")
    }
    runtime("org.xerial:sqlite-jdbc:3.21.0.1") { isTransitive = false }
    runtime(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api")) { isTransitive = false }
    runtime("com.jetbrains.intellij.swift:swift:$clionVersion") { isTransitive = false }
    runtime("com.jetbrains.intellij.swift:swift-doc:$clionVersion") { isTransitive = false }
    runtime(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxBundle:${kotlinNativeBackendVersion}:backend.native.jar"))
    runtime(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxBundle:${kotlinNativeBackendVersion}:konan.serializer.jar")) // required for backend.native
}

val preparePluginXmlTask: Task = preparePluginXml(
        project,
        ":kotlin-ultimate:ide:mobile-native",
        clionVersion,
        clionVersionStrict,
        mobilePluginVersionFull,
        true
)

val pluginXmlPath = "META-INF/plugin.xml"
val pluginJarTask: Task by tasks.named<Jar>("jar") {
    dependsOn(preparePluginXmlTask)
    from(preparePluginXmlTask)

    configurations.findByName("embedded")?.let { embedded ->
        dependsOn(embedded)
        from(provider { embedded.map(::zipTree) }) { exclude(pluginXmlPath) }
    }

    archiveBaseName.set(project.the<BasePluginConvention>().archivesBaseName)
    archiveFileName.set("mobile-plugin.jar")
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", archiveBaseName.get())
    }
}

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

val mobilePlugin: Task by tasks.creating(Copy::class) {
    duplicatesStrategy = DuplicatesStrategy.FAIL

    into(mobilePluginDir)

    into("lib") {
        dependsOn(pluginJarTask)
        from(pluginJarTask)
    }

    dependsOn(
        copyNativeDeps,
        copyRuntimeDeps,
        ":kotlin-ultimate:prepare:clion-plugin:clionPlugin"
    )
}

val zipMobilePluginTask: Task = zipCidrPlugin(project, mobilePlugin, mobilePluginZipPath)

val mobileUpdatePluginsXmlTask: Task = cidrUpdatePluginsXml(
        project,
        preparePluginXmlTask,
        clionFriendlyVersion,
        mobilePluginZipPath,
        mobileCustomPluginRepoUrl,
        clionJavaPluginDownloadUrl
)
