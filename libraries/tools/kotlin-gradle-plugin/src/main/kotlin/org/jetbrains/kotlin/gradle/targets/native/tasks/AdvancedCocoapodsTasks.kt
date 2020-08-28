/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.FileTree
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.plugin.cocoapods.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency.PodLocation.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.konan.target.Family
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.util.*
import kotlin.concurrent.thread

private val Family.platformLiteral: String
    get() = when (this) {
        Family.OSX -> "macos"
        Family.IOS -> "ios"
        Family.TVOS -> "tvos"
        Family.WATCHOS -> "watchos"
        else -> throw IllegalArgumentException("Bad family ${this.name}")
    }

/**
 * The task takes the path to the Podfile and calls `pod install`
 * to obtain sources or artifacts for the declared dependencies.
 * This task is a part of CocoaPods integration infrastructure.
 */
open class PodInstallTask : DefaultTask() {
    init {
        onlyIf { cocoapodsExtension?.podfile != null }
    }

    @get:Optional
    @get:Nested
    internal var cocoapodsExtension: CocoapodsExtension? = null

    @get:Optional
    @get:OutputDirectory
    internal val podsXcodeProjDirProvider: Provider<File>?
        get() = cocoapodsExtension?.podfile?.let {
            project.provider { it.parentFile.resolve("Pods").resolve("Pods.xcodeproj") }
        }


    @TaskAction
    fun doPodInstall() {
        cocoapodsExtension?.podfile?.parentFile?.also { podfileDir ->
            val podInstallProcess = ProcessBuilder("pod", "install").apply {
                directory(podfileDir)
            }.start()
            val podInstallRetCode = podInstallProcess.waitFor()
            val podInstallOutput = podInstallProcess.inputStream.use { it.reader().readText() }

            check(podInstallRetCode == 0) {
                listOf(
                    "Executing of 'pod install' failed with code $podInstallRetCode.",
                    "Error message:",
                    podInstallOutput
                ).joinToString("\n")
            }
            with(podsXcodeProjDirProvider) {
                check(this != null && get().exists() && get().isDirectory) {
                    "The directory 'Pods/Pods.xcodeproj' was not created as a result of the `pod install` call."
                }
            }
        }
    }
}


abstract class CocoapodsWithSyntheticTask : DefaultTask() {
    init {
        onlyIf {
            cocoapodsExtension.pods.isNotEmpty()
        }
    }

    @get:Nested
    internal lateinit var cocoapodsExtension: CocoapodsExtension
}

abstract class PodDownloadTask : CocoapodsWithSyntheticTask() {
    @get:Input
    internal lateinit var podName: Provider<String>
}


open class PodDownloadUrlTask : PodDownloadTask() {

    @get:Nested
    internal lateinit var podSource: Provider<Url>

    @get:Internal
    internal val urlDir = project.cocoapodsBuildDirs.externalSources("url")


    @get:OutputDirectory
    internal val podSourceDir = project.provider {
        urlDir.resolve(podName.get())
    }

    @get:Internal
    internal val permittedFileExtensions = listOf("zip", "tar", "tgz", "tbz", "txz", "gzip", "tar.gz", "tar.bz2", "tar.xz", "jar")

    @TaskAction
    fun download() {
        val podLocation = podSource.get()
        val url = podLocation.url.toString()
        val repoUrl = url.substringBeforeLast("/")
        val fileName = url.substringAfterLast("/")
        val fileNameWithoutExtension = fileName.substringBefore(".")
        val extension = fileName.substringAfter(".")
        require(permittedFileExtensions.contains(extension)) { "Unknown file extension" }

        val repo = setupRepo(repoUrl)
        val dependency = createDependency(fileNameWithoutExtension, extension)
        val configuration = project.configurations.detachedConfiguration(dependency)
        val artifact = configuration.singleFile
        copyArtifactToUrlDir(artifact, extension, podLocation.flatten)
        project.repositories.remove(repo)
    }

    private fun setupRepo(repoUrl: String): ArtifactRepository {
        return project.repositories.ivy { repo ->
            repo.setUrl(repoUrl)
            repo.patternLayout {
                it.artifact("[artifact].[ext]")
            }
            repo.metadataSources {
                it.artifact()
            }
        }
    }

    private fun createDependency(fileNameWithoutExtension: String, extension: String) = project.dependencies.create(
        mapOf(
            "name" to fileNameWithoutExtension,
            "version" to "1.0",
            "ext" to extension
        )
    )

    private fun copyArtifactToUrlDir(artifact: File, extension: String, flatten: Boolean) {
        val archiveTree = archiveTree(artifact.absolutePath, extension)
        project.copy {
            val destinationDir = podSourceDir.get()
            it.into(destinationDir)
            it.from(archiveTree)
            if (extension == "jar") {
                it.exclude("META-INF/")
            }
            if (!flatten) {
                it.eachFile { file ->
                    file.relativePath = RelativePath(true, *file.relativePath.segments.drop(1).toTypedArray())
                }
                it.includeEmptyDirs = false
            }
        }
    }

    private fun archiveTree(path: String, extension: String): FileTree {
        return if (extension == "zip" || extension == "jar") {
            project.zipTree(path)
        } else {
            project.tarTree(path)
        }
    }
}


open class PodDownloadGitTask : PodDownloadTask() {

    @get:Nested
    internal lateinit var podSource: Provider<Git>

    @get:Internal
    internal val gitDir = project.cocoapodsBuildDirs.externalSources("git")

    @get:OutputDirectory
    internal val repo = project.provider {
        gitDir.resolve(podName.get())
    }

    @TaskAction
    fun download() {
        repo.get().deleteRecursively()
        val git = podSource.get()
        val branch = git.tag ?: git.branch
        val commit = git.commit
        val url = git.url
        try {
            when {
                commit != null -> {
                    retrieveCommit(url, commit)
                }
                branch != null -> {
                    cloneShallow(url, branch)
                }
                else -> {
                    cloneHead(git)
                }
            }
        } catch (e: IllegalStateException) {
            fallback(git)
        }
    }

    private fun retrieveCommit(url: URI, commit: String) {
        val initCommand = listOf(
            "git",
            "init"
        )
        val repo = repo.get()
        repo.mkdir()
        val configProcess: ProcessBuilder.() -> Unit = { directory(repo) }
        runCommand(initCommand, configProcess)

        val fetchCommand = listOf(
            "git",
            "fetch",
            "--depth", "1",
            "$url",
            commit
        )
        runCommand(fetchCommand, configProcess)

        val checkoutCommand = listOf(
            "git",
            "checkout",
            "FETCH_HEAD"
        )
        runCommand(checkoutCommand, configProcess)
    }

    private fun cloneShallow(url: URI, branch: String) {
        val shallowCloneCommand = listOf(
            "git",
            "clone",
            "$url",
            podName.get(),
            "--branch", branch,
            "--depth", "1"
        )
        val configProcess: ProcessBuilder.() -> Unit = { directory(gitDir) }
        runCommand(shallowCloneCommand, configProcess)
    }

    private fun cloneHead(podspecLocation: Git) {
        val cloneHeadCommand = listOf(
            "git",
            "clone",
            "${podspecLocation.url}",
            podName.get(),
            "--depth", "1"
        )
        val configProcess: ProcessBuilder.() -> Unit = { directory(gitDir) }
        runCommand(cloneHeadCommand, configProcess)
    }

    private fun fallback(podspecLocation: Git) {
        // removing any traces of other commands
        gitDir.resolve(podName.get()).deleteRecursively()
        val cloneAllCommand = listOf(
            "git",
            "clone",
            "${podspecLocation.url}",
            podName.get()
        )
        val configProcess: ProcessBuilder.() -> Unit = { directory(gitDir) }
        runCommand(cloneAllCommand, configProcess)
    }
}

private fun runCommand(
    command: List<String>,
    processConfiguration: ((ProcessBuilder.() -> Unit))? = null,
    errorHandler: ((retCode: Int, process: Process) -> Unit)? = null
): String {
    val process = ProcessBuilder(command)
        .apply {
            if (processConfiguration != null) {
                this.processConfiguration()
            }
        }.start()

    var inputText = ""
    var errorText = ""

    val inputThread = thread {
        inputText = process.inputStream.use {
            it.reader().readText()
        }
    }

    val errorThread = thread {
        errorText = process.errorStream.use {
            it.reader().readText()
        }
    }

    inputThread.join()
    errorThread.join()

    val retCode = process.waitFor()
    check(retCode == 0) {
        errorHandler?.invoke(retCode, process)
            ?: "Executing of '${command.joinToString(" ")}' failed with code $retCode and message: $errorText"
    }

    return inputText
}

/**
 * The task takes the path to the .podspec file and calls `pod gen`
 * to create synthetic xcode project and workspace.
 */
open class PodGenTask : CocoapodsWithSyntheticTask() {

    @get:InputFile
    internal lateinit var podspecProvider: Provider<File>

    @get:Internal
    lateinit var family: Family

    @get:OutputDirectory
    internal val podsXcodeProjDirProvider: Provider<File>
        get() = project.provider {
            project.cocoapodsBuildDirs.synthetic(family)
                .resolve(podspecProvider.get().nameWithoutExtension)
                .resolve("Pods")
                .resolve("Pods.xcodeproj")
        }

    @TaskAction
    fun generate() {
        val syntheticDir = project.cocoapodsBuildDirs.synthetic(family).apply { mkdirs() }
        val localPodspecPaths = cocoapodsExtension.pods.mapNotNull { it.source?.getLocalPath(project, it.name) }

        val sources = cocoapodsExtension.specRepos.getAll().toMutableList()
        sources += URI("https://cdn.cocoapods.org")

        val podGenProcessArgs = listOfNotNull(
            "pod", "gen",
            "--platforms=${family.platformLiteral}",
            "--gen-directory=${syntheticDir.absolutePath}",
            localPodspecPaths.takeIf { it.isNotEmpty() }?.joinToString(separator = ",")?.let { "--local-sources=$it" },
            sources.takeIf { it.isNotEmpty() }?.joinToString(separator = ",")?.let { "--sources=$it" },
            podspecProvider.get().absolutePath
        )

        val podGenProcess = ProcessBuilder(podGenProcessArgs).apply {
            directory(syntheticDir)
        }.start()
        val podGenRetCode = podGenProcess.waitFor()
        val outputText = podGenProcess.inputStream.use { it.reader().readText() }

        check(podGenRetCode == 0) {
            listOfNotNull(
                "Executing of '${podGenProcessArgs.joinToString(" ")}' failed with code $podGenRetCode and message:",
                outputText,
                outputText.takeIf {
                    it.contains("deployment target")
                            || it.contains("requested platforms: [\"${family.platformLiteral}\"]")
                }?.let {
                    """
                        Tip: try to configure deployment_target for ALL targets as follows:
                        cocoapods {
                            ...
                            ${family.name.toLowerCase()}.deploymentTarget = "..."
                            ...
                        }
                    """.trimIndent()
                }
            ).joinToString("\n")
        }

        val podsXcprojFile = podsXcodeProjDirProvider.get()
        check(podsXcprojFile.exists() && podsXcprojFile.isDirectory) {
            "The directory '${podsXcprojFile.path}' was not created as a result of the `pod gen` call."
        }
    }
}


open class PodSetupBuildTask : CocoapodsWithSyntheticTask() {

    @get:InputDirectory
    internal lateinit var podsXcodeProjDirProvider: Provider<File>

    @get:Input
    internal lateinit var sdk: Provider<String>

    @Input
    lateinit var schemeName: Provider<String>

    @get:OutputFile
    internal val buildSettingsFileProvider: Provider<File> = project.provider {
        project.cocoapodsBuildDirs
            .buildSettings
            .resolve(sdk.get().toBuildSettingsFileName)
    }

    private val String.toBuildSettingsFileName: String
        get() = "build-settings-$this.properties"

    @TaskAction
    fun setupBuild() {
        val podsXcodeProjDir = podsXcodeProjDirProvider.get()

        val buildSettingsReceivingCommand = listOf(
            "xcodebuild", "-showBuildSettings",
            "-project", podsXcodeProjDir.name,
            "-scheme", schemeName.get(),
            "-sdk", sdk.get()
        )

        val buildSettingsProcess = ProcessBuilder(buildSettingsReceivingCommand)
            .apply {
                directory(podsXcodeProjDir.parentFile)
            }.start()

        val buildSettingsRetCode = buildSettingsProcess.waitFor()
        check(buildSettingsRetCode == 0) {
            listOf(
                "Executing of '${buildSettingsReceivingCommand.joinToString(" ")}' failed with code $buildSettingsRetCode and message:",
                buildSettingsProcess.errorStream.use { it.reader().readText() }
            ).joinToString("\n")
        }

        val stdOut = buildSettingsProcess.inputStream

        val buildSettingsProperties = PodBuildSettingsProperties.readSettingsFromStream(stdOut)
        buildSettingsFileProvider.get().let { buildSettingsProperties.writeSettings(it) }
    }
}

/**
 * The task compiles external cocoa pods sources.
 */
open class PodBuildTask : CocoapodsWithSyntheticTask() {

    @get:InputDirectory
    internal lateinit var podsXcodeProjDirProvider: Provider<File>

    @get:InputFile
    internal lateinit var buildSettingsFileProvider: Provider<File>

    @get:Input
    internal lateinit var sdk: Provider<String>

    @get:Optional
    @get:OutputDirectory
    internal var buildDirProvider: Provider<File>? = null

    private val CocoapodsExtension.CocoapodsDependency.schemeName: String
        get() = name.split("/")[0]

    @TaskAction
    fun buildDependencies() {
        val podBuildSettings = PodBuildSettingsProperties.readSettingsFromStream(
            FileInputStream(buildSettingsFileProvider.get())
        )

        val podsXcodeProjDir = podsXcodeProjDirProvider.get()

        cocoapodsExtension.pods.all {

            val podXcodeBuildCommand = listOf(
                "xcodebuild",
                "-project", podsXcodeProjDir.name,
                "-scheme", it.schemeName,
                "-sdk", sdk.get(),
                "-configuration", podBuildSettings.configuration
            )

            val podBuildProcess = ProcessBuilder(podXcodeBuildCommand)
                .apply {
                    directory(podsXcodeProjDir.parentFile)
                    inheritIO()
                }.start()

            val podBuildRetCode = podBuildProcess.waitFor()
            check(podBuildRetCode == 0) {
                listOf(
                    "Executing of '${podXcodeBuildCommand.joinToString(" ")}' failed with code $podBuildRetCode and message:",
                    podBuildProcess.errorStream.use { it.reader().readText() }
                ).joinToString("\n")
            }
        }
        buildDirProvider = project.provider { project.file(podBuildSettings.buildDir) }
    }
}

internal data class PodBuildSettingsProperties(
    internal val buildDir: String,
    internal val configuration: String,
    internal val cflags: String? = null,
    internal val headerPaths: String? = null,
    internal val frameworkPaths: String? = null
) {

    fun writeSettings(buildSettingsFile: File) {
        buildSettingsFile.parentFile.mkdirs()
        buildSettingsFile.createNewFile()

        check(buildSettingsFile.exists()) { "Unable to create file ${buildSettingsFile.path}!" }

        with(Properties()) {
            setProperty(BUILD_DIR, buildDir)
            setProperty(CONFIGURATION, configuration)
            cflags?.let { setProperty(OTHER_CFLAGS, it) }
            headerPaths?.let { setProperty(HEADER_SEARCH_PATHS, it) }
            frameworkPaths?.let { setProperty(FRAMEWORK_SEARCH_PATHS, it) }
            buildSettingsFile.outputStream().use {
                store(it, null)
            }
        }
    }

    companion object {
        const val BUILD_DIR: String = "BUILD_DIR"
        const val CONFIGURATION: String = "CONFIGURATION"
        const val OTHER_CFLAGS: String = "OTHER_CFLAGS"
        const val HEADER_SEARCH_PATHS: String = "HEADER_SEARCH_PATHS"
        const val FRAMEWORK_SEARCH_PATHS: String = "FRAMEWORK_SEARCH_PATHS"

        fun readSettingsFromStream(inputStream: InputStream): PodBuildSettingsProperties {
            with(Properties()) {
                load(inputStream)
                return PodBuildSettingsProperties(
                    getProperty(BUILD_DIR),
                    getProperty(CONFIGURATION),
                    getProperty(OTHER_CFLAGS),
                    getProperty(HEADER_SEARCH_PATHS),
                    getProperty(FRAMEWORK_SEARCH_PATHS)
                )
            }
        }
    }
}
