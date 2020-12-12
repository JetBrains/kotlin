/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RelativePath
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency.PodLocation.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingCocoapodsMessage
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingSpecReposMessage
import org.jetbrains.kotlin.gradle.tasks.PodspecTask.Companion.retrievePods
import org.jetbrains.kotlin.gradle.tasks.PodspecTask.Companion.retrieveSpecRepos
import org.jetbrains.kotlin.konan.target.Family
import java.io.File
import java.io.Reader
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

val CocoapodsDependency.schemeName: String
    get() = name.split("/")[0]

/**
 * The task takes the path to the Podfile and calls `pod install`
 * to obtain sources or artifacts for the declared dependencies.
 * This task is a part of CocoaPods integration infrastructure.
 */
open class PodInstallTask : DefaultTask() {
    init {
        onlyIf { podfile.isPresent }
    }

    @get:Internal
    internal lateinit var frameworkName: Provider<String>

    @get:Optional
    @get:Input
    internal val podfile = project.objects.property(File::class.java)

    @get:Optional
    @get:OutputDirectory
    internal val podsXcodeProjDirProvider: Provider<File>?
        get() = podfile.orNull?.let {
            project.provider { it.parentFile.resolve("Pods").resolve("Pods.xcodeproj") }
        }


    @TaskAction
    fun doPodInstall() {
        podfile.orNull?.parentFile?.also { podfileDir ->
            val podInstallProcess = ProcessBuilder("pod", "install").apply {
                directory(podfileDir)
            }.start()
            val podInstallRetCode = podInstallProcess.waitFor()
            val podInstallOutput = podInstallProcess.inputStream.use { it.reader().readText() }

            check(podInstallRetCode == 0) {
                val specReposMessages = retrieveSpecRepos(project)?.let { MissingSpecReposMessage(it).missingMessage }
                val cocoapodsMessages = retrievePods(project)?.map { MissingCocoapodsMessage(it, project).missingMessage }

                listOfNotNull(
                    "Executing of 'pod install' failed with code $podInstallRetCode.",
                    "Error message:",
                    podInstallOutput,
                    specReposMessages?.let {
                        """
                            |Please, check that file "${podfile.get().path}" contains following lines in header:
                            |$it
                            |
                        """.trimMargin()
                    },
                    cocoapodsMessages?.let {
                        """
                            |Please, check that each target depended on ${frameworkName.get()} contains following dependencies:
                            |${it.joinToString("\n")}
                            |
                        """.trimMargin()
                    }

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

private interface ExtendedErrorDiagnostic {
    fun isAppropriateOutput(output: String): Boolean
    val message: String
}

abstract class DownloadCocoapodsTask : DefaultTask() {
    @get:Input
    internal lateinit var podName: Provider<String>
}

open class PodDownloadUrlTask : DownloadCocoapodsTask() {

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

open class PodDownloadGitTask : DownloadCocoapodsTask() {

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
        val logger = project.logger
        val initCommand = listOf(
            "git",
            "init"
        )
        val repo = repo.get()
        repo.mkdir()
        runCommand(initCommand, logger) { directory(repo) }

        val fetchCommand = listOf(
            "git",
            "fetch",
            "--depth", "1",
            "$url",
            commit
        )
        runCommand(fetchCommand, logger) { directory(repo) }

        val checkoutCommand = listOf(
            "git",
            "checkout",
            "FETCH_HEAD"
        )
        runCommand(checkoutCommand, logger) { directory(repo) }
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
        runCommand(shallowCloneCommand, project.logger) { directory(gitDir) }
    }

    private fun cloneHead(podspecLocation: Git) {
        val cloneHeadCommand = listOf(
            "git",
            "clone",
            "${podspecLocation.url}",
            podName.get(),
            "--depth", "1"
        )
        runCommand(cloneHeadCommand, project.logger) { directory(gitDir) }
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
        runCommand(cloneAllCommand, project.logger) { directory(gitDir) }
    }
}

private fun runCommand(
    command: List<String>,
    logger: Logger,
    extendedErrorDiagnostic: ExtendedErrorDiagnostic? = null,
    errorHandler: ((retCode: Int, process: Process) -> Unit)? = null,
    processConfiguration: ProcessBuilder.() -> Unit = { }
): String {
    val process = ProcessBuilder(command)
        .apply {
            this.processConfiguration()
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
    logger.info(
        """
            |Information about "${command.joinToString(" ")}" call:
            |
            |${inputText}
        """.trimMargin()
    )

    check(retCode == 0) {
        errorHandler?.invoke(retCode, process)
            ?: """
                |Executing of '${command.joinToString(" ")}' failed with code $retCode and message: 
                |
                |$errorText
                |${extendedErrorDiagnostic?.takeIf { it.isAppropriateOutput(inputText) }?.message ?: ""}
                """.trimMargin()
    }

    return inputText
}

/**
 * The task takes the path to the .podspec file and calls `pod gen`
 * to create synthetic xcode project and workspace.
 */
open class PodGenTask : DefaultTask() {

    init {
        onlyIf {
            pods.get().isNotEmpty()
        }
    }

    @get:InputFile
    internal lateinit var podspec: Provider<File>

    @get:Input
    internal lateinit var useLibraries: Provider<Boolean>

    @get:Internal
    lateinit var family: Family

    @get:Nested
    internal lateinit var specRepos: Provider<SpecRepos>

    @get:Nested
    val pods = project.objects.listProperty(CocoapodsDependency::class.java)

    @get:OutputDirectory
    internal val podsXcodeProjDir: Provider<File>
        get() = project.provider {
            project.cocoapodsBuildDirs.synthetic(family)
                .resolve(podspec.get().nameWithoutExtension)
                .resolve("Pods")
                .resolve("Pods.xcodeproj")
        }

    @TaskAction
    fun generate() {
        val syntheticDir = project.cocoapodsBuildDirs.synthetic(family).apply { mkdirs() }
        val localPodspecPaths = pods.get().mapNotNull { it.source?.getLocalPath(project, it.name) }

        val specRepos = specRepos.get().getAll()

        val podGenProcessArgs = listOfNotNull(
            "pod", "gen",
            "--use-libraries".takeIf { useLibraries.get() },
            "--platforms=${family.platformLiteral}",
            "--gen-directory=${syntheticDir.absolutePath}",
            localPodspecPaths.takeIf { it.isNotEmpty() }?.joinToString(separator = ",")?.let { "--local-sources=$it" },
            specRepos.takeIf { it.isNotEmpty() }?.joinToString(separator = ",")?.let { "--sources=$it" },
            podspec.get().absolutePath
        )

        val podGenDiagnostic = object : ExtendedErrorDiagnostic {
            override fun isAppropriateOutput(output: String): Boolean =
                output.contains("deployment target") || output.contains("requested platforms: [\"${family.platformLiteral}\"]")

            override val message: String
                get() =
                    """
                        |Tip: try to configure deployment_target for ALL targets as follows:
                        |cocoapods {
                        |   ...
                        |   ${family.name.toLowerCase()}.deploymentTarget = "..."
                        |   ...
                        |}
                    """.trimMargin()
        }

        runCommand(podGenProcessArgs, project.logger, podGenDiagnostic) { directory(syntheticDir) }

        val podsXcprojFile = podsXcodeProjDir.get()
        check(podsXcprojFile.exists() && podsXcprojFile.isDirectory) {
            "The directory '${podsXcprojFile.path}' was not created as a result of the `pod gen` call."
        }
    }
}


open class PodSetupBuildTask : DefaultTask() {

    @get:Input
    lateinit var frameworkName: Provider<String>

    @get:Input
    internal lateinit var sdk: Provider<String>

    @get:Nested
    lateinit var pod: Provider<CocoapodsDependency>

    @get:OutputFile
    internal val buildSettingsFile: Provider<File> = project.provider {
        project.cocoapodsBuildDirs
            .buildSettings
            .resolve(getBuildSettingFileName(pod.get(), sdk.get()))
    }

    @get:Internal
    internal lateinit var podsXcodeProjDir: Provider<File>

    @TaskAction
    fun setupBuild() {
        val podsXcodeProjDir = podsXcodeProjDir.get()

        val buildSettingsReceivingCommand = listOf(
            "xcodebuild", "-showBuildSettings",
            "-project", podsXcodeProjDir.name,
            "-scheme", pod.get().schemeName,
            "-sdk", sdk.get()
        )

        val outputText = runCommand(buildSettingsReceivingCommand, project.logger) { directory(podsXcodeProjDir.parentFile) }

        val buildSettingsProperties = PodBuildSettingsProperties.readSettingsFromReader(outputText.reader())
        buildSettingsFile.get().let { bsf ->
            buildSettingsProperties.writeSettings(bsf)
        }
    }
}

private fun getBuildSettingFileName(pod: CocoapodsDependency, sdk: String): String =
    "build-settings-$sdk-${pod.schemeName}.properties"

/**
 * The task compiles external cocoa pods sources.
 */
open class PodBuildTask : DefaultTask() {

    @get:InputFile
    internal lateinit var buildSettingsFile: Provider<File>

    @get:Nested
    internal lateinit var pod: Provider<CocoapodsDependency>

    @get:InputFiles
    internal val srcDir: FileTree
        get() = project.fileTree(
            buildSettingsFile.map { PodBuildSettingsProperties.readSettingsFromReader(it.reader()).podsTargetSrcRoot }
        )

    @get:Internal
    internal var buildDir: Provider<File> = project.provider {
        project.file(PodBuildSettingsProperties.readSettingsFromReader(buildSettingsFile.get().reader()).buildDir)
    }

    @get:Input
    internal lateinit var sdk: Provider<String>

    @get:OutputFiles
    internal val buildResult: Provider<FileCollection>? = project.provider {
        project.fileTree(buildDir.get()) {
            it.include("**/${pod.get().schemeName}.*/")
            it.include("**/${pod.get().schemeName}/")
        }
    }

    @get:Internal
    internal lateinit var podsXcodeProjDir: Provider<File>

    @TaskAction
    fun buildDependencies() {
        val podBuildSettings = PodBuildSettingsProperties.readSettingsFromReader(buildSettingsFile.get().reader())

        val podsXcodeProjDir = podsXcodeProjDir.get()

        val podXcodeBuildCommand = listOf(
            "xcodebuild",
            "-project", podsXcodeProjDir.name,
            "-scheme", pod.get().schemeName,
            "-sdk", sdk.get(),
            "-configuration", podBuildSettings.configuration
        )

        runCommand(podXcodeBuildCommand, project.logger) { directory(podsXcodeProjDir.parentFile) }
    }
}


internal data class PodBuildSettingsProperties(
    internal val buildDir: String,
    internal val configuration: String,
    internal val configurationBuildDir: String,
    internal val podsTargetSrcRoot: String,
    internal val cflags: String? = null,
    internal val headerPaths: String? = null,
    internal val frameworkPaths: String? = null
) {

    fun writeSettings(
        buildSettingsFile: File
    ) {
        buildSettingsFile.parentFile.mkdirs()
        buildSettingsFile.delete()
        buildSettingsFile.createNewFile()

        check(buildSettingsFile.exists()) { "Unable to create file ${buildSettingsFile.path}!" }

        with(buildSettingsFile) {
            appendText("$BUILD_DIR=$buildDir\n")
            appendText("$CONFIGURATION=$configuration\n")
            appendText("$CONFIGURATION_BUILD_DIR=$configurationBuildDir\n")
            appendText("$PODS_TARGET_SRCROOT=$podsTargetSrcRoot\n")
            cflags?.let { appendText("$OTHER_CFLAGS=$it\n") }
            headerPaths?.let { appendText("$HEADER_SEARCH_PATHS=$it\n") }
            frameworkPaths?.let { appendText("$FRAMEWORK_SEARCH_PATHS=$it") }
        }
    }

    companion object {
        const val BUILD_DIR = "BUILD_DIR"
        const val CONFIGURATION = "CONFIGURATION"
        const val CONFIGURATION_BUILD_DIR = "CONFIGURATION_BUILD_DIR"
        const val PODS_TARGET_SRCROOT = "PODS_TARGET_SRCROOT"
        const val OTHER_CFLAGS = "OTHER_CFLAGS"
        const val HEADER_SEARCH_PATHS = "HEADER_SEARCH_PATHS"
        const val FRAMEWORK_SEARCH_PATHS = "FRAMEWORK_SEARCH_PATHS"

        fun readSettingsFromReader(reader: Reader): PodBuildSettingsProperties {
            with(Properties()) {
                load(reader)
                return PodBuildSettingsProperties(
                    readProperty(BUILD_DIR),
                    readProperty(CONFIGURATION),
                    readProperty(CONFIGURATION_BUILD_DIR),
                    readProperty(PODS_TARGET_SRCROOT),
                    readNullableProperty(OTHER_CFLAGS),
                    readNullableProperty(HEADER_SEARCH_PATHS),
                    readNullableProperty(FRAMEWORK_SEARCH_PATHS)
                )
            }
        }

        private fun Properties.readProperty(propertyName: String) =
            readNullableProperty(propertyName) ?: error("$propertyName property is absent")

        private fun Properties.readNullableProperty(propertyName: String) =
            getProperty(propertyName)
    }
}
