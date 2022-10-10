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
import org.gradle.api.Project
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
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.IOException
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

open class CocoapodsTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }
}


open class PodInstallTask : CocoapodsTask() {
    init {
        onlyIf { podfile.isPresent }
    }

    @get:Internal
    internal lateinit var frameworkName: Provider<String>

    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    internal val podfile = project.objects.property(File::class.java)

    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    internal val podspec = project.objects.property(File::class.java)

    @get:Optional
    @get:OutputDirectory
    internal val podsXcodeProjDirProvider: Provider<File>?
        get() = podfile.orNull?.let {
            project.provider { it.parentFile.resolve("Pods").resolve("Pods.xcodeproj") }
        }

    @TaskAction
    fun doPodInstall() {
        podfile.orNull?.parentFile?.also { podfileDir ->
            val podInstallCommand = listOf("pod", "install")

            runCommand(podInstallCommand,
                       logger,
                       errorHandler = { returnCode, output, _ ->
                           CocoapodsErrorHandlingUtil.handlePodInstallError(
                               podInstallCommand.joinToString(" "),
                               returnCode,
                               output,
                               project,
                               frameworkName.get()
                           )
                       },
                       exceptionHandler = { e: IOException ->
                           CocoapodsErrorHandlingUtil.handle(e, podInstallCommand)
                       },
                       processConfiguration = {
                           directory(podfileDir)
                       })

            with(podsXcodeProjDirProvider) {
                check(this != null && get().exists() && get().isDirectory) {
                    "The directory 'Pods/Pods.xcodeproj' was not created as a result of the `pod install` call."
                }
            }
        }
    }
}

/**
 * The task generates a synthetic project with all cocoapods dependencies
 */
open class PodGenTask : CocoapodsTask() {

    init {
        onlyIf {
            pods.get().isNotEmpty()
        }
    }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    internal lateinit var podspec: Provider<File>

    @get:Input
    internal lateinit var podName: Provider<String>

    @get:Input
    internal lateinit var useLibraries: Provider<Boolean>

    @get:Internal
    lateinit var family: Family

    @get:Nested
    internal lateinit var platformSettings: PodspecPlatformSettings

    @get:Nested
    internal lateinit var specRepos: Provider<SpecRepos>

    @get:Nested
    val pods = project.objects.listProperty(CocoapodsDependency::class.java)

    @get:OutputDirectory
    internal val podsXcodeProjDir: Provider<File>
        get() = project.provider {
            project.cocoapodsBuildDirs.synthetic(family)
                .resolve("Pods")
                .resolve("Pods.xcodeproj")
        }

    @TaskAction
    fun generate() {
        val syntheticDir = project.cocoapodsBuildDirs.synthetic(family).apply { mkdirs() }
        val specRepos = specRepos.get().getAll()

        val projResource = "/cocoapods/project.pbxproj"
        val projDestination = syntheticDir.resolve("synthetic.xcodeproj").resolve("project.pbxproj")

        projDestination.parentFile.mkdirs()
        projDestination.outputStream().use { file ->
            javaClass.getResourceAsStream(projResource).use { resource ->
                resource.copyTo(file)
            }
        }

        val podfile = syntheticDir.resolve("Podfile")
        podfile.createNewFile()

        val podfileContent = getPodfileContent(specRepos, family.platformLiteral)
        podfile.writeText(podfileContent)
        val podInstallCommand = listOf("pod", "install")

        runCommand(
            podInstallCommand,
            project.logger,
            exceptionHandler = { e: IOException ->
                CocoapodsErrorHandlingUtil.handle(e, podInstallCommand)
            },
            errorHandler = { retCode, output, _ ->
                CocoapodsErrorHandlingUtil.handlePodInstallSyntheticError(
                    podInstallCommand.joinToString(" "),
                    retCode,
                    output,
                    family,
                    podName.get()
                )
            },
            processConfiguration = {
                directory(syntheticDir)
            })

        val podsXcprojFile = podsXcodeProjDir.get()
        check(podsXcprojFile.exists() && podsXcprojFile.isDirectory) {
            "Synthetic project '${podsXcprojFile.path}' was not created."
        }
    }

    private fun getPodfileContent(specRepos: Collection<String>, xcodeTarget: String) =
        buildString {

            specRepos.forEach {
                appendLine("source '$it'")
            }

            appendLine("target '$xcodeTarget' do")
            if (useLibraries.get().not()) {
                appendLine("\tuse_frameworks!")
            }
            val deploymentTarget = platformSettings.deploymentTarget
            if (deploymentTarget != null) {
                appendLine("\tplatform :${platformSettings.name}, '$deploymentTarget'")
            } else {
                appendLine("\tplatform :${platformSettings.name}")
            }
            pods.get().mapNotNull {
                buildString {
                    append("pod '${it.name}'")

                    val version = it.version
                    val source = it.source

                    if (source != null) {
                        append(", ${source.getPodSourcePath()}")
                    } else if (version != null) {
                        append(", '$version'")
                    }

                }
            }.forEach { appendLine("\t$it") }
            appendLine("end\n")
            //disable signing for all synthetic pods KT-54314
            append(
                """
                post_install do |installer|
                  installer.pods_project.targets.each do |target|
                    target.build_configurations.each do |config|
                      config.build_settings['EXPANDED_CODE_SIGN_IDENTITY'] = ""
                      config.build_settings['CODE_SIGNING_REQUIRED'] = "NO"
                      config.build_settings['CODE_SIGNING_ALLOWED'] = "NO"
                    end
                  end
                end
                """.trimIndent()
            )
        }
}


open class PodSetupBuildTask : CocoapodsTask() {

    @get:Input
    lateinit var frameworkName: Provider<String>

    @get:Input
    internal lateinit var sdk: Provider<String>

    @get:Nested
    lateinit var pod: Provider<CocoapodsDependency>

    @get:OutputFile
    val buildSettingsFile: Provider<File> = project.provider {
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
open class PodBuildTask : CocoapodsTask() {

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    lateinit var buildSettingsFile: Provider<File>
        internal set

    @get:Nested
    internal lateinit var pod: Provider<CocoapodsDependency>

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
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


data class PodBuildSettingsProperties(
    internal val buildDir: String,
    internal val configuration: String,
    val configurationBuildDir: String,
    internal val podsTargetSrcRoot: String,
    internal val cflags: String? = null,
    internal val headerPaths: String? = null,
    internal val publicHeadersFolderPath: String? = null,
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
            publicHeadersFolderPath?.let { appendText("$PUBLIC_HEADERS_FOLDER_PATH=$it\n") }
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
        const val PUBLIC_HEADERS_FOLDER_PATH = "PUBLIC_HEADERS_FOLDER_PATH"
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
                    readNullableProperty(PUBLIC_HEADERS_FOLDER_PATH),
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

private object CocoapodsErrorHandlingUtil {
    fun handle(e: IOException, command: List<String>) {
        if (e.message?.contains("No such file or directory") == true) {
            val message = """ 
               |'${command.take(2).joinToString(" ")}' command failed with an exception:
               | ${e.message}
               |        
               |        Full command: ${command.joinToString(" ")}
               |        
               |        Possible reason: CocoaPods is not installed
               |        Please check that CocoaPods v1.10 or above is installed.
               |        
               |        To check CocoaPods version type 'pod --version' in the terminal
               |        
               |        To install CocoaPods execute 'sudo gem install cocoapods'
               |
            """.trimMargin()
            throw IllegalStateException(message)
        } else {
            throw e
        }
    }

    fun handlePodInstallError(command: String, retCode: Int, error: String, project: Project, frameworkName: String): String {
        val specReposMessages = retrieveSpecRepos(project)?.let { MissingSpecReposMessage(it).missingMessage }
        val cocoapodsMessages = retrievePods(project)?.map { MissingCocoapodsMessage(it, project).missingMessage }

        return listOfNotNull(
            "'pod install' command failed with code $retCode.",
            "Full command: $command",
            "Error message:",
            error.lines().filter { it.isNotBlank() }.joinToString("\n"),
            specReposMessages?.let {
                """
                    |        Please, check that podfile contains following lines in header:
                    |        $it
                    |
                """.trimMargin()
            },
            cocoapodsMessages?.let {
                """
                   |         Please, check that each target depended on $frameworkName contains following dependencies:
                   |         ${it.joinToString("\n")}
                   |        
                """.trimMargin()
            }

        ).joinToString("\n")
    }

    fun handlePodInstallSyntheticError(command: String, retCode: Int, error: String, family: Family, podName: String): String? {
        var message = """
            |'pod install' command on the synthetic project failed with return code: $retCode
            |
            |        Full command: $command
            |
            |        Error: ${error.lines().filter { it.contains("[!]") }.joinToString("\n")}
            |       
        """.trimMargin()

        if (
            error.contains("deployment target") ||
            error.contains("no platform was specified") ||
            error.contains(Regex("The platform of the target .+ is not compatible with `$podName"))
        ) {
            message += """
                |
                |        Possible reason: ${family.name.toLowerCase()} deployment target is not configured
                |        Configure deployment_target for ALL targets as follows:
                |        cocoapods {
                |           ...
                |           ${family.name.toLowerCase()}.deploymentTarget = "..."
                |           ...
                |        }
                |       
            """.trimMargin()
            return message
        } else if (
            error.contains("Unable to add a source with url") ||
            error.contains("Couldn't determine repo name for URL") ||
            error.contains("Unable to find a specification")
        ) {
            message += """
                |
                |        Possible reason: spec repos are not configured correctly.
                |        Ensure that spec repos are correctly configured for all private pod dependencies:
                |        cocoapods {
                |           specRepos {
                |               url("<private spec repo url>")
                |           }
                |        }
                |       
            """.trimMargin()
            return message
        }
        return null
    }

    fun handlePodDownloadError(podName: String, command: String, retCode: Int, error: String): String {
        return """
            |'git clone' command failed with return code: $retCode
            |
            |       Full command: $command
            |
            |       Error: ${error.lines().filter { it.contains("fatal", true) }.joinToString("\n")}
            |       
            |       Possible reason: source of a pod '$podName' is invalid or inaccessible
            |       
        """.trimMargin()
    }
}
