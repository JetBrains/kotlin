/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis") // All tasks should be inherited only by Gradle

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency.PodLocation.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.plugin.cocoapods.platformLiteral
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingCocoapodsMessage
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingSpecReposMessage
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.IOException
import java.io.Reader
import java.util.*

val CocoapodsDependency.schemeName: String
    get() = name.split("/")[0]


open class CocoapodsTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }
}


/**
 * The task takes the path to the Podfile and calls `pod install`
 * to obtain sources or artifacts for the declared dependencies.
 * This task is a part of CocoaPods integration infrastructure.
 */
abstract class AbstractPodInstallTask : CocoapodsTask() {
    init {
        onlyIf { podfile.isPresent }
    }

    @get:Optional
    @get:InputFile
    abstract val podfile: Property<File?>

    @get:Internal
    protected val workingDir: Provider<File> = podfile.map { file: File? ->
        requireNotNull(file) { "Task outputs shouldn't be queried if it's skipped" }.parentFile
    }

    @get:OutputDirectory
    internal val podsDir: Provider<File> = workingDir.map { it.resolve("Pods") }

    @get:Internal
    internal val podsXcodeProjDirProvider: Provider<File> = podsDir.map { it.resolve("Pods.xcodeproj") }

    @TaskAction
    open fun doPodInstall() {
        val podInstallCommand = listOf("pod", "install")

        runCommand(podInstallCommand,
                   logger,
                   errorHandler = ::handleError,
                   exceptionHandler = { e: IOException ->
                       CocoapodsErrorHandlingUtil.handle(e, podInstallCommand)
                   },
                   processConfiguration = {
                       directory(workingDir.get())
                   })

        with(podsXcodeProjDirProvider.get()) {
            check(exists() && isDirectory) {
                "The directory 'Pods/Pods.xcodeproj' was not created as a result of the `pod install` call."
            }
        }
    }

    abstract fun handleError(retCode: Int, error: String, process: Process): String?
}

abstract class PodInstallTask : AbstractPodInstallTask() {

    @get:Optional
    @get:InputFile
    abstract val podspec: Property<File?>

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Nested
    abstract val specRepos: Property<SpecRepos>

    @get:Nested
    abstract val pods: ListProperty<CocoapodsDependency>

    @get:InputDirectory
    abstract val dummyFramework: Property<File>

    private val framework = project.provider { project.cocoapodsBuildDirs.framework.resolve("${frameworkName.get()}.framework") }
    private val tmpFramework = dummyFramework.map { dummy -> dummy.parentFile.resolve("tmp.framework").also { it.deleteOnExit() } }

    override fun doPodInstall() {
        // We always need to execute 'pod install' with the dummy framework because the one left from a previous build
        // may have a wrong linkage type. So we temporarily swap them, run 'pod install' and then swap them back
        framework.rename(tmpFramework)
        dummyFramework.rename(framework)
        super.doPodInstall()
        framework.rename(dummyFramework)
        tmpFramework.rename(framework)
    }

    private fun Provider<File>.rename(dest: Provider<File>) = get().rename(dest.get())

    private fun File.rename(dest: File) {
        if (!exists()) {
            mkdirs()
        }

        check(renameTo(dest)) { "Can't rename '${this}' to '${dest}'" }
    }

    override fun handleError(retCode: Int, error: String, process: Process): String? {
        val specReposMessages = MissingSpecReposMessage(specRepos.get()).missingMessage
        val cocoapodsMessages = pods.get().map { MissingCocoapodsMessage(it).missingMessage }

        return listOfNotNull(
            "'pod install' command failed with code $retCode.",
            "Error message:",
            error.lines().filter { it.isNotBlank() }.joinToString("\n"),
            """
            |        Please, check that podfile contains following lines in header:
            |        $specReposMessages
            |
            """.trimMargin(),
            """
            |        Please, check that each target depended on ${frameworkName.get()} contains following dependencies:
            |        ${cocoapodsMessages.joinToString("\n")}
            |        
            """.trimMargin()

        ).joinToString("\n")
    }
}

abstract class PodInstallSyntheticTask : AbstractPodInstallTask() {

    @get:Input
    abstract val family: Property<Family>

    @get:Input
    abstract val podName: Property<String>

    @get:OutputDirectory
    internal val syntheticXcodeProject: Provider<File> = workingDir.map { it.resolve("synthetic.xcodeproj") }

    override fun doPodInstall() {
        val projResource = "/cocoapods/project.pbxproj"
        val projDestination = syntheticXcodeProject.get().resolve("project.pbxproj")

        syntheticXcodeProject.get().mkdirs()
        projDestination.outputStream().use { file ->
            javaClass.getResourceAsStream(projResource)!!.use { resource ->
                resource.copyTo(file)
            }
        }

        super.doPodInstall()
    }

    override fun handleError(retCode: Int, error: String, process: Process): String? {
        var message = """
            |'pod install' command on the synthetic project failed with return code: $retCode
            |
            |        Error: ${error.lines().filter { it.contains("[!]") }.joinToString("\n")}
            |       
        """.trimMargin()

        if (
            error.contains("deployment target") ||
            error.contains("no platform was specified") ||
            error.contains(Regex("The platform of the target .+ is not compatible with `${podName.get()}"))
        ) {
            message += """
                |
                |        Possible reason: ${family.get().platformLiteral} deployment target is not configured
                |        Configure deployment_target for ALL targets as follows:
                |        cocoapods {
                |           ...
                |           ${family.get().platformLiteral}.deploymentTarget = "..."
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
        } else {
            return null
        }
    }
}

/**
 * The task generates a synthetic project with all cocoapods dependencies
 */
abstract class PodGenTask : CocoapodsTask() {

    init {
        onlyIf {
            pods.get().isNotEmpty()
        }
    }

    @get:InputFile
    internal abstract val podspec: Property<File>

    @get:Input
    internal abstract val podName: Property<String>

    @get:Input
    internal abstract val useLibraries: Property<Boolean>

    @get:Input
    internal abstract val family: Property<Family>

    @get:Nested
    internal abstract val platformSettings: Property<PodspecPlatformSettings>

    @get:Nested
    internal abstract val specRepos: Property<SpecRepos>

    @get:Nested
    internal abstract val pods: ListProperty<CocoapodsDependency>

    @get:OutputFile
    val podfile: Provider<File> = family.map { project.cocoapodsBuildDirs.synthetic(it).resolve("Podfile") }

    @TaskAction
    fun generate() {
        val specRepos = specRepos.get().getAll()

        val podfile = this.podfile.get()
        podfile.createNewFile()

        val podfileContent = getPodfileContent(specRepos, family.get().platformLiteral)
        podfile.writeText(podfileContent)
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
            val settings = platformSettings.get()
            val deploymentTarget = settings.deploymentTarget
            if (deploymentTarget != null) {
                appendLine("\tplatform :${settings.name}, '$deploymentTarget'")
            } else {
                appendLine("\tplatform :${settings.name}")
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
            appendLine()
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

    @Suppress("unused") // declares an ouptut
    @get:OutputFiles
    internal val buildResult: Provider<FileCollection> = project.provider {
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
                @Suppress("BlockingMethodInNonBlockingContext") // It's ok to do blocking call here
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

}
