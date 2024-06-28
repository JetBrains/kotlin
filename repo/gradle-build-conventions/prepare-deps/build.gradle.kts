@file:Suppress("PropertyName", "HasPlatformType", "UnstableApiUsage")

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.Closeable
import java.io.OutputStreamWriter
import java.net.URI
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.xml.stream.XMLOutputFactory

plugins {
    base
    id("org.jetbrains.kotlin.jvm")
}

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

project.apply {
    from(rootProject.file("../../gradle/versions.gradle.kts"))
}

val isTeamcityBuild = kotlinBuildProperties.isTeamcityBuild

val intellijSeparateSdks by extra(project.getBooleanProperty("intellijSeparateSdks") ?: false)
val intellijReleaseType: String by extra {
    when {
        extra["versions.intellijSdk"]?.toString()?.contains("-EAP-") == true -> "snapshots"
        extra["versions.intellijSdk"]?.toString()?.endsWith("SNAPSHOT") == true -> "nightly"
        else -> "releases"
    }
}
val customDepsOrg: String by extra("kotlin.build")

val intellijVersion = project.extra["versions.intellijSdk"] as String
val intellijVersionForIde = rootProject.intellijSdkVersionForIde()
val asmVersion = project.findProperty("versions.jar.asm-all") as String?
val androidStudioRelease = project.findProperty("versions.androidStudioRelease") as String?
val androidStudioBuild = project.findProperty("versions.androidStudioBuild") as String?

fun checkIntellijVersion(intellijVersion: String) {
    val intellijVersionDelimiterIndex = intellijVersion.indexOfAny(charArrayOf('.', '-'))
    if (intellijVersionDelimiterIndex == -1) {
        error("Invalid IDEA version $intellijVersion")
    }
}
checkIntellijVersion(intellijVersion)
intellijVersionForIde?.let { checkIntellijVersion(it) }

logger.info("intellijVersion: $intellijVersion")
logger.info("intellijVersionForIde: $intellijVersionForIde")
logger.info("androidStudioRelease: $androidStudioRelease")
logger.info("androidStudioBuild: $androidStudioBuild")
logger.info("intellijSeparateSdks: $intellijSeparateSdks")

val androidStudioOs by lazy {
    when {
        OperatingSystem.current().isWindows -> "windows"
        OperatingSystem.current().isMacOsX -> "mac"
        OperatingSystem.current().isLinux -> "linux"
        else -> {
            logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
            ""
        }
    }
}

repositories {
    if (androidStudioRelease != null) {
        ivy {
            url = URI("https://dl.google.com/dl/android/studio/ide-zips/$androidStudioRelease")

            patternLayout {
                artifact("[artifact]-[revision]-$androidStudioOs.[ext]")
            }

            metadataSources {
                artifact()
            }
        }
    }

    maven("https://www.jetbrains.com/intellij-repository/$intellijReleaseType")
    maven("https://plugins.jetbrains.com/maven")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

val intellij by configurations.creating
val intellijForIde by configurations.creating
val androidStudio by configurations.creating
val sources by configurations.creating
val sourcesForIde by configurations.creating
val jpsStandalone by configurations.creating
val jpsStandaloneForIde by configurations.creating
val intellijCore by configurations.creating
val intellijCoreForIde by configurations.creating

/**
 * Special repository for annotations.jar required for idea runtime only.
 *
 * See IntellijDependenciesKt.intellijRuntimeAnnotations for more details.
 */
val intellijRuntimeAnnotations = "intellij-runtime-annotations"

val dependenciesDir = (findProperty("kotlin.build.dependencies.dir") as String?)?.let(::File)
    ?: rootProject.gradle.gradleUserHomeDir.resolve("kotlin-build-dependencies")

val customDepsRepoDir = dependenciesDir.resolve("repo")

val repoDir = File(customDepsRepoDir, customDepsOrg)

dependencies {
    if (androidStudioRelease != null) {
        val extension = if (androidStudioOs == "linux")
            "tar.gz"
        else
            "zip"

        androidStudio("google:android-studio-ide:$androidStudioBuild@$extension")
    } else {
        intellij("com.jetbrains.intellij.idea:ideaIC:$intellijVersion")
        intellijVersionForIde?.let { intellijForIde("com.jetbrains.intellij.idea:ideaIC:$it") }
    }

    if (asmVersion != null) {
        sources("org.jetbrains.intellij.deps:asm-all:$asmVersion:sources@jar")
    }

    sources("com.jetbrains.intellij.idea:ideaIC:$intellijVersion:sources@jar")
    intellijVersionForIde?.let { sourcesForIde("com.jetbrains.intellij.idea:ideaIC:$it:sources@jar") }
    jpsStandalone("com.jetbrains.intellij.idea:jps-standalone:$intellijVersion")
    intellijVersionForIde?.let { jpsStandaloneForIde("com.jetbrains.intellij.idea:jps-standalone:$it") }
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$intellijVersion")
    intellijVersionForIde?.let { intellijCoreForIde("com.jetbrains.intellij.idea:intellij-core:$it") }
}

fun prepareDeps(
    intellij: Configuration,
    intellijCore: Configuration,
    sources: Configuration,
    jpsStandalone: Configuration,
    intellijVersion: String
) {
    val makeIntellijCore = buildIvyRepositoryTask(intellijCore, customDepsOrg, customDepsRepoDir)

    val makeIntellijAnnotations = tasks.register("makeIntellijAnnotations${intellij.name.replaceFirstChar(Char::uppercase)}", Copy::class) {
        dependsOn(makeIntellijCore)

        val intellijCoreRepo = CleanableStore[repoDir.resolve("intellij-core").absolutePath][intellijVersion].use()
        from(intellijCoreRepo.resolve("artifacts/annotations.jar"))

        val annotationsStore = CleanableStore[repoDir.resolve(intellijRuntimeAnnotations).absolutePath]
        val targetDir = annotationsStore[intellijVersion].use()
        into(targetDir)

        val ivyFile = File(targetDir, "$intellijRuntimeAnnotations.ivy.xml")
        outputs.files(ivyFile)

        doFirst {
            annotationsStore.cleanStore()
        }

        doLast {
            writeIvyXml(
                customDepsOrg,
                intellijRuntimeAnnotations,
                intellijVersion,
                intellijRuntimeAnnotations,
                targetDir,
                targetDir,
                targetDir,
                allowAnnotations = true
            )
        }
    }

    val mergeSources = tasks.create("mergeSources${intellij.name.replaceFirstChar(Char::uppercase)}", Jar::class.java) {
        dependsOn(sources)
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        isZip64 = true
        if (!kotlinBuildProperties.isTeamcityBuild) {
            from(provider { sources.map(::zipTree) })
        }
        destinationDirectory.set(File(repoDir, sources.name))
        archiveBaseName.set("intellij")
        archiveClassifier.set("sources")
        archiveVersion.set(intellijVersion)
    }

    val sourcesFile = mergeSources.outputs.files.singleFile

    val makeIde = if (androidStudioBuild != null) {
        buildIvyRepositoryTask(
            androidStudio,
            customDepsOrg,
            customDepsRepoDir,
            if (androidStudioOs == "mac")
                ::skipContentsDirectory
            else
                ::skipToplevelDirectory
        )
    } else {
        val task = buildIvyRepositoryTask(intellij, customDepsOrg, customDepsRepoDir, null, sourcesFile)

        task.configure {
            dependsOn(mergeSources)
        }

        task
    }

    val buildJpsStandalone = buildIvyRepositoryTask(jpsStandalone, customDepsOrg, customDepsRepoDir, null, sourcesFile)

    tasks.named("build") {
        dependsOn(
            makeIntellijCore,
            makeIde,
            buildJpsStandalone,
            makeIntellijAnnotations
        )
    }
}

when(kotlinBuildProperties.getOrNull("attachedIntellijVersion")) {
    null -> {}
    "master" -> {} // for intellij/kt-master, intellij maven artifacts are used instead of manual unpacked dependencies
    else -> {
        val intellijVersionForIde = intellijVersionForIde
            ?: error("intellijVersionForIde should not be null as attachedIntellijVersion is present")
        prepareDeps(intellijForIde, intellijCoreForIde, sourcesForIde, jpsStandaloneForIde, intellijVersionForIde)
    }
}


tasks.named<Delete>("clean") {
    delete(customDepsRepoDir)
}

fun buildIvyRepositoryTask(
    configuration: Configuration,
    organization: String,
    repoDirectory: File,
    pathRemap: ((String) -> String)? = null,
    sources: File? = null
): TaskProvider<Task> {
    fun ResolvedArtifact.storeDirectory(): CleanableStore =
        CleanableStore[repoDirectory.resolve("$organization/${moduleVersion.id.name}").absolutePath]

    fun ResolvedArtifact.moduleDirectory(): File =
        storeDirectory()[moduleVersion.id.version].use()

    return tasks.register("buildIvyRepositoryFor${configuration.name.replaceFirstChar(Char::uppercase)}") {
        dependsOn(configuration)
        inputs.files(configuration)

        outputs.upToDateWhen {
            val repoMarker = configuration.resolvedConfiguration.resolvedArtifacts.single().moduleDirectory().resolve(".marker")
            repoMarker.exists()
        }

        doFirst {
            val artifact = configuration.resolvedConfiguration.resolvedArtifacts.single()
            val moduleDirectory = artifact.moduleDirectory()

            artifact.storeDirectory().cleanStore()

            val repoMarker = File(moduleDirectory, ".marker")
            if (repoMarker.exists()) {
                logger.info("Path ${repoMarker.absolutePath} already exists, skipping unpacking.")
                return@doFirst
            }

            with(artifact) {
                val artifactsDirectory = File(moduleDirectory, "artifacts")
                logger.info("Unpacking ${file.name} into ${artifactsDirectory.absolutePath}")
                copy {
                    val fileTree = when (extension) {
                        "tar.gz" -> tarTree(file)
                        "zip" -> zipTree(file)
                        else -> error("Unsupported artifact extension: $extension")
                    }

                    from(
                        fileTree.matching {
                            exclude("**/plugins/Kotlin/**")
                        }
                    )

                    into(artifactsDirectory)

                    if (pathRemap != null) {
                        eachFile {
                            path = pathRemap(path)
                        }
                    }

                    includeEmptyDirs = false
                }

                writeIvyXml(
                    organization,
                    moduleVersion.id.name,
                    moduleVersion.id.version,
                    moduleVersion.id.name,
                    File(artifactsDirectory, "lib"),
                    File(artifactsDirectory, "lib"),
                    File(moduleDirectory, "ivy"),
                    *listOfNotNull(sources).toTypedArray()
                )

                val pluginsDirectory = File(artifactsDirectory, "plugins")
                if (pluginsDirectory.exists()) {
                    file(File(artifactsDirectory, "plugins"))
                        .listFiles { file: File -> file.isDirectory }
                        .forEach {
                            writeIvyXml(
                                organization,
                                it.name,
                                moduleVersion.id.version,
                                it.name,
                                File(it, "lib"),
                                File(it, "lib"),
                                File(moduleDirectory, "ivy"),
                                *listOfNotNull(sources).toTypedArray()
                            )
                        }
                }

                repoMarker.createNewFile()
            }
        }
    }
}

fun CleanableStore.cleanStore() = cleanDir(Instant.now().minus(Duration.ofDays(30)))

fun writeIvyXml(
    organization: String,
    moduleName: String,
    version: String,
    fileName: String,
    baseDir: File,
    artifactDir: File,
    targetDir: File,
    vararg sourcesJar: File,
    allowAnnotations: Boolean = false
) {
    fun shouldIncludeIntellijJar(jar: File) =
        jar.isFile
                && jar.extension == "jar"
                && !jar.name.startsWith("kotlin-")
                && (allowAnnotations || jar.name != "annotations.jar") // see comments for [intellijAnnotations] above

    val ivyFile = targetDir.resolve("$fileName.ivy.xml")
    ivyFile.parentFile.mkdirs()
    with(XMLWriter(ivyFile.writer())) {
        document("UTF-8", "1.0") {
            element("ivy-module") {
                attribute("version", "2.0")
                attribute("xmlns:m", "http://ant.apache.org/ivy/maven")

                emptyElement("info") {
                    attributes(
                        "organisation" to organization,
                        "module" to moduleName,
                        "revision" to version,
                        "publication" to SimpleDateFormat("yyyyMMddHHmmss").format(Date())
                    )
                }

                element("configurations") {
                    listOf("default", "sources").forEach { configurationName ->
                        emptyElement("conf") {
                            attributes("name" to configurationName, "visibility" to "public")
                        }
                    }
                }

                element("publications") {
                    artifactDir.listFiles()
                        ?.filter(::shouldIncludeIntellijJar)
                        ?.sortedBy { it.name.lowercase() }
                        ?.forEach { jarFile ->
                            val relativeName = jarFile.toRelativeString(baseDir).removeSuffix(".jar")
                            emptyElement("artifact") {
                                attributes(
                                    "name" to relativeName,
                                    "type" to "jar",
                                    "ext" to "jar",
                                    "conf" to "default"
                                )
                            }
                    }

                    sourcesJar.forEach { jarFile ->
                        emptyElement("artifact") {
                            val sourcesArtifactName = jarFile.name.substringBefore("-$version")
                            attributes(
                                "name" to sourcesArtifactName,
                                "type" to "jar",
                                "ext" to "jar",
                                "conf" to "sources",
                                "m:classifier" to "sources"
                            )
                        }
                    }
                }
            }
        }

        close()
    }
}

fun skipToplevelDirectory(path: String) = path.substringAfter('/')

fun skipContentsDirectory(path: String) = path.substringAfter("Contents/")

fun Project.intellijSdkVersionForIde(): String? {
    val majorVersion = kotlinBuildProperties.getOrNull("attachedIntellijVersion") as? String ?: return null
    return rootProject.findProperty("versions.intellijSdk.forIde.$majorVersion") as? String
}

class XMLWriter(private val outputStreamWriter: OutputStreamWriter) : Closeable {

    private val xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStreamWriter)

    private var depth = 0
    private val indent = "  "

    fun document(encoding: String, version: String, init: XMLWriter.() -> Unit) = apply {
        xmlStreamWriter.writeStartDocument(encoding, version)

        init()

        xmlStreamWriter.writeEndDocument()
    }

    fun element(name: String, init: XMLWriter.() -> Unit) = apply {
        writeIndent()
        xmlStreamWriter.writeStartElement(name)
        depth += 1

        init()

        depth -= 1
        writeIndent()
        xmlStreamWriter.writeEndElement()
    }

    fun emptyElement(name: String, init: XMLWriter.() -> Unit) = apply {
        writeIndent()
        xmlStreamWriter.writeEmptyElement(name)
        init()
    }

    fun attribute(name: String, value: String): Unit = xmlStreamWriter.writeAttribute(name, value)

    fun attributes(vararg attributes: Pair<String, String>) {
        attributes.forEach { attribute(it.first, it.second) }
    }

    private fun writeIndent() {
        xmlStreamWriter.writeCharacters("\n")
        repeat(depth) {
            xmlStreamWriter.writeCharacters(indent)
        }
    }

    override fun close() {
        xmlStreamWriter.flush()
        xmlStreamWriter.close()
        outputStreamWriter.close()
    }
}