/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.cocoapods

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.daemon.common.trimQuotes
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency.PodLocation.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.asValidTaskName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import java.io.File
import java.util.*

internal val Project.cocoapodsBuildDirs: CocoapodsBuildDirs
    get() = CocoapodsBuildDirs(this)

internal class CocoapodsBuildDirs(val project: Project) {
    val root: File
        get() = project.buildDir.resolve("cocoapods")

    val framework: File
        get() = root.resolve("framework")

    val defs: File
        get() = root.resolve("defs")

    val buildSettings: File
        get() = root.resolve("buildSettings")

    val synthetic: File
        get() = root.resolve("synthetic")

    fun synthetic(family: Family) = synthetic.resolve(family.name)

    val externalSources: File
        get() = root.resolve("externalSources")

    fun externalSources(fileName: String) = externalSources.resolve(fileName)

    fun fatFramework(buildType: String) =
        root.resolve("fat-frameworks/${buildType.toLowerCase()}")
}

internal fun String.asValidFrameworkName() = replace('-', '_')

private val Family.toPodGenTaskName: String
    get() = lowerCamelCaseName(
        KotlinCocoapodsPlugin.POD_GEN_TASK_NAME,
        name
    )

private fun String.toSetupBuildTaskName(pod: CocoapodsDependency): String = lowerCamelCaseName(
    KotlinCocoapodsPlugin.POD_SETUP_BUILD_TASK_NAME,
    pod.name.asValidTaskName(),
    this
)

private fun String.toBuildDependenciesTaskName(pod: CocoapodsDependency): String = lowerCamelCaseName(
    KotlinCocoapodsPlugin.POD_BUILD_TASK_NAME,
    pod.name.asValidTaskName(),
    this
)

private val CocoapodsDependency.toPodDownloadTaskName: String
    get() = lowerCamelCaseName(
        KotlinCocoapodsPlugin.POD_DOWNLOAD_TASK_NAME,
        name.asValidTaskName()
    )

open class KotlinCocoapodsPlugin : Plugin<Project> {
    private fun KotlinMultiplatformExtension.supportedTargets() = targets
        .withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }

    private val KotlinNativeTarget.toValidSDK: String
        get() = when (konanTarget) {
            IOS_X64 -> "iphonesimulator"
            IOS_ARM32, IOS_ARM64 -> "iphoneos"
            WATCHOS_X86, WATCHOS_X64 -> "watchsimulator"
            WATCHOS_ARM32, WATCHOS_ARM64 -> "watchos"
            TVOS_X64 -> "appletvsimulator"
            TVOS_ARM64 -> "appletvos"
            MACOS_X64 -> "macosx"
            else -> throw IllegalArgumentException("Bad target ${konanTarget.name}.")
        }

    /**
     * Splits a string using a whitespace characters as delimiters.
     * Ignores whitespaces in quotes and drops quotes, e.g. a string
     * `foo "bar baz" qux="quux"` will be split into ["foo", "bar baz", "qux=quux"].
     */
    private fun String.splitQuotedArgs(): List<String> =
        Regex("""(?:[^\s"]|(?:"[^"]*"))+""").findAll(this).map {
            it.value.replace("\"", "")
        }.toList()

    private fun KotlinMultiplatformExtension.targetsForPlatform(requestedPlatform: KonanTarget) =
        supportedTargets().matching { it.konanTarget == requestedPlatform }

    private fun createDefaultFrameworks(kotlinExtension: KotlinMultiplatformExtension, cocoapodsExtension: CocoapodsExtension) {
        kotlinExtension.supportedTargets().all { target ->
            target.binaries.framework {
                baseNameProvider = project.provider { cocoapodsExtension.frameworkName }
                isStatic = true
            }
        }
    }

    private fun Project.createSyncFrameworkTask(originalDirectory: Provider<File>, buildingTask: TaskProvider<*>) =
        registerTask<Sync>(SYNC_TASK_NAME) {
            it.group = TASK_GROUP
            it.description = "Copies a framework for given platform and build type into the CocoaPods build directory"

            it.dependsOn(buildingTask)
            it.from(originalDirectory)
            it.destinationDir = cocoapodsBuildDirs.framework
        }

    private fun createSyncForFatFramework(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        requestedBuildType: String,
        requestedPlatforms: List<KonanTarget>
    ) {
        val fatTargets = requestedPlatforms.associate { it to kotlinExtension.targetsForPlatform(it) }

        check(fatTargets.values.any { it.isNotEmpty() }) {
            "The project must have a target for at least one of the following platforms: " +
                    "${requestedPlatforms.joinToString { it.visibleName }}."
        }
        fatTargets.forEach { platform, targets ->
            check(targets.size <= 1) {
                "The project has more than one target for the requested platform: `${platform.visibleName}`"
            }
        }

        val fatFrameworkTask = project.registerTask<FatFrameworkTask>("fatFramework") { task ->
            task.group = TASK_GROUP
            task.description = "Creates a fat framework for ARM32 and ARM64 architectures"
            task.destinationDir = project.cocoapodsBuildDirs.fatFramework(requestedBuildType)

            fatTargets.forEach { _, targets ->
                targets.singleOrNull()?.let {
                    task.from(it.binaries.getFramework(requestedBuildType))
                }
            }
        }

        project.createSyncFrameworkTask(fatFrameworkTask.map { it.destinationDir }, fatFrameworkTask)
    }

    private fun createSyncForRegularFramework(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        requestedBuildType: String,
        requestedPlatform: KonanTarget
    ) {
        val targets = kotlinExtension.targetsForPlatform(requestedPlatform)

        check(targets.isNotEmpty()) { "The project doesn't contain a target for the requested platform: `${requestedPlatform.visibleName}`" }
        check(targets.size == 1) { "The project has more than one target for the requested platform: `${requestedPlatform.visibleName}`" }

        val frameworkLinkTask = targets.single().binaries.getFramework(requestedBuildType).linkTaskProvider
        project.createSyncFrameworkTask(frameworkLinkTask.map { it.destinationDir }, frameworkLinkTask)
    }

    private fun createSyncTask(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension
    ) = project.whenEvaluated {
        val requestedTargetName = project.findProperty(TARGET_PROPERTY)?.toString() ?: return@whenEvaluated
        val requestedBuildType = project.findProperty(CONFIGURATION_PROPERTY)?.toString()?.toUpperCase() ?: return@whenEvaluated

        // We create a fat framework only for device platforms which have several
        // device architectures: iosArm64, iosArm32, watchosArm32 and watchosArm64.
        val frameworkPlatforms: List<KonanTarget> = when (requestedTargetName) {
            KOTLIN_TARGET_FOR_IOS_DEVICE -> listOf(IOS_ARM64, IOS_ARM32)
            KOTLIN_TARGET_FOR_WATCHOS_DEVICE -> listOf(WATCHOS_ARM32, WATCHOS_ARM64)
            else -> listOf(HostManager().targetByName(requestedTargetName)) // A requested target doesn't require building a fat framework.
        }

        val frameworkTargets = frameworkPlatforms.flatMap { kotlinExtension.targetsForPlatform(it) }
        if (frameworkTargets.size == 1) {
            // Fast path: there is only one device target. There is no need to build a fat framework.
            createSyncForRegularFramework(project, kotlinExtension, requestedBuildType, frameworkTargets.single().konanTarget)
        } else {
            // There are several device targets so we need to build a fat framework.
            createSyncForFatFramework(project, kotlinExtension, requestedBuildType, frameworkPlatforms)
        }
    }

    private fun createInterops(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val moduleNames = mutableSetOf<String>()

        cocoapodsExtension.pods.all { pod ->
            if (moduleNames.contains(pod.moduleName)) {
                return@all
            }
            moduleNames.add(pod.moduleName)

            val defTask = project.registerTask<DefFileTask>(
                lowerCamelCaseName("generateDef", pod.moduleName).asValidTaskName()
            ) {
                it.pod = project.provider { pod }
                it.description = "Generates a def file for CocoaPods dependencies with module ${pod.moduleName}"
                // This task is an implementation detail so we don't add it in any group
                // to avoid showing it in the `tasks` output.
            }

            kotlinExtension.supportedTargets().all { target ->
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.create(pod.moduleName) { interop ->

                    val interopTask = project.tasks.getByPath(interop.interopProcessingTaskName)

                    interopTask.dependsOn(defTask)

                    with(interop) {
                        defFileProperty.set(defTask.map { it.outputFile })
                        _packageNameProp.set(project.provider { pod.packageName })
                        _extraOptsProp.addAll(project.provider { pod.extraOpts })
                    }

                    if (
                        isAvailableToProduceSynthetic
                        && project.findProperty(TARGET_PROPERTY) == null
                        && project.findProperty(CONFIGURATION_PROPERTY) == null
                    ) {
                        val podBuildTaskProvider =
                            project.tasks.named(target.toValidSDK.toBuildDependenciesTaskName(pod), PodBuildTask::class.java)
                        interopTask.inputs.file(podBuildTaskProvider.get().buildSettingsFile)
                        interopTask.dependsOn(podBuildTaskProvider)
                    }

                    project.findProperty(CFLAGS_PROPERTY)?.toString()?.let { args ->
                        // Xcode quotes around paths with spaces.
                        // Here and below we need to split such paths taking this into account.
                        interop.compilerOpts.addAll(args.splitQuotedArgs())
                    }
                    project.findProperty(HEADER_PATHS_PROPERTY)?.toString()?.let { args ->
                        interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-I$it" })
                    }
                    project.findProperty(FRAMEWORK_PATHS_PROPERTY)?.toString()?.let { args ->
                        interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-F$it" })
                    }

                    interopTask.doFirst { _ ->
                        // Since we cannot expand the configuration phase of interop tasks
                        // receiving the required environment variables happens on execution phase.
                        // TODO This needs to be fixed to improve UP-TO-DATE checks.
                        if (
                            isAvailableToProduceSynthetic
                            && project.findProperty(TARGET_PROPERTY) == null
                            && project.findProperty(CONFIGURATION_PROPERTY) == null
                        ) {
                            val podBuildTaskProvider =
                                project.tasks.named(target.toValidSDK.toBuildDependenciesTaskName(pod), PodBuildTask::class.java)
                            val buildSettings =
                                podBuildTaskProvider.get().buildSettingsFile.get()
                                    .inputStream()
                                    .use {
                                        PodBuildSettingsProperties.readSettingsFromStream(it)
                                    }

                            buildSettings.cflags?.let { args ->
                                // Xcode quotes around paths with spaces.
                                // Here and below we need to split such paths taking this into account.
                                interop.compilerOpts.addAll(args.splitQuotedArgs())
                            }
                            buildSettings.headerPaths?.let { args ->
                                interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-I$it" })
                            }

                            val frameworkPaths = buildSettings.frameworkPaths
                            val configurationBuildDir = buildSettings.configurationBuildDir
                            val frameworkPathsSelfIncluding = mutableListOf<String>()
                            frameworkPathsSelfIncluding += configurationBuildDir.trimQuotes()
                            frameworkPaths?.let { frameworkPathsSelfIncluding.addAll(it.splitQuotedArgs()) }

                            interop.compilerOpts.addAll(frameworkPathsSelfIncluding.map { "-F$it" })
                        }
                    }
                }
            }
        }
    }

    private fun registerDummyFrameworkTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
        project.tasks.register(DUMMY_FRAMEWORK_TASK_NAME, DummyFrameworkTask::class.java) {
            it.frameworkName = project.provider { cocoapodsExtension.frameworkName }
        }
    }

    private fun registerPodspecTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val dummyFrameworkTaskProvider = project.tasks.named(DUMMY_FRAMEWORK_TASK_NAME)

        project.tasks.register(POD_SPEC_TASK_NAME, PodspecTask::class.java) {
            it.group = TASK_GROUP
            it.description = "Generates a podspec file for CocoaPods import"
            it.needPodspec = project.provider { cocoapodsExtension.needPodspec }
            it.pods.set(cocoapodsExtension.pods)
            it.version = project.provider { cocoapodsExtension.version }
            it.homepage.set(cocoapodsExtension.homepage)
            it.license.set(cocoapodsExtension.license)
            it.authors.set(cocoapodsExtension.authors)
            it.summary.set(cocoapodsExtension.summary)
            it.frameworkName = project.provider { cocoapodsExtension.frameworkName }
            it.ios = project.provider { cocoapodsExtension.ios }
            it.osx = project.provider { cocoapodsExtension.osx }
            it.tvos = project.provider { cocoapodsExtension.tvos }
            it.watchos = project.provider { cocoapodsExtension.watchos }
            it.dependsOn(dummyFrameworkTaskProvider)
            val generateWrapper = project.findProperty(GENERATE_WRAPPER_PROPERTY)?.toString()?.toBoolean() ?: false
            if (generateWrapper) {
                it.dependsOn(":wrapper")
            }
        }
    }

    private fun registerPodInstallTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val podspecTaskProvider = project.tasks.named(POD_SPEC_TASK_NAME, PodspecTask::class.java)
        project.tasks.register(POD_INSTALL_TASK_NAME, PodInstallTask::class.java) {
            it.group = TASK_GROUP
            it.description = "Invokes `pod install` call within Podfile location directory"
            it.podfile.set(cocoapodsExtension.podfile)
            it.frameworkName = project.provider { cocoapodsExtension.frameworkName }
            it.onlyIf { isAvailableToProduceSynthetic }
            it.dependsOn(podspecTaskProvider)
        }
    }

    private fun registerPodDownloadTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val downloadAllTask = project.tasks.register(POD_DOWNLOAD_TASK_NAME) {
            it.group = TASK_GROUP
            it.description = "Downloads CocoaPods dependencies from external sources"
        }

        cocoapodsExtension.pods.all { pod ->
            val podSource = pod.source
            val downloadPodTask = when (podSource) {
                is Git -> project.tasks.register(pod.toPodDownloadTaskName, PodDownloadGitTask::class.java) {
                    it.podName = project.provider { pod.name.asValidTaskName() }
                    it.podSource = project.provider { podSource as Git }
                }
                is Url -> project.tasks.register(pod.toPodDownloadTaskName, PodDownloadUrlTask::class.java) {
                    it.podName = project.provider { pod.name.asValidTaskName() }
                    it.podSource = project.provider { podSource as Url }
                }
                else -> return@all
            }

            downloadAllTask.dependsOn(downloadPodTask)
        }
    }

    private fun registerPodGenTask(
        project: Project, kotlinExtension: KotlinMultiplatformExtension, cocoapodsExtension: CocoapodsExtension
    ) {
        val families = mutableSetOf<Family>()

        val podspecTaskProvider = project.tasks.named(POD_SPEC_TASK_NAME, PodspecTask::class.java)
        val downloadPods = project.tasks.named(POD_DOWNLOAD_TASK_NAME)
        kotlinExtension.supportedTargets().all { target ->

            val family = target.konanTarget.family
            if (family in families) {
                return@all
            }
            families += family

            project.tasks.register(family.toPodGenTaskName, PodGenTask::class.java) {
                it.description = "Сreates a synthetic Xcode project to retrieve CocoaPods dependencies"
                it.podspec = podspecTaskProvider.map { task -> task.outputFileProvider.get() }
                it.useLibraries = project.provider { cocoapodsExtension.useLibraries }
                it.specRepos = project.provider { cocoapodsExtension.specRepos }
                it.family = family
                it.pods.set(cocoapodsExtension.pods)
                it.dependsOn(downloadPods)
                it.onlyIf { isAvailableToProduceSynthetic }
            }
        }
    }

    private fun registerPodSetupBuildTasks(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val schemeNames = mutableSetOf<String>()

        cocoapodsExtension.pods.all { pod ->

            if (schemeNames.contains(pod.schemeName)) {
                return@all
            }
            schemeNames.add(pod.schemeName)

            val sdks = mutableSetOf<String>()

            kotlinExtension.supportedTargets().all loop@{ target ->

                val sdk = target.toValidSDK
                if (sdk in sdks) {
                    return@loop
                }
                sdks += sdk

                val podGenTaskProvider = project.tasks.named(target.konanTarget.family.toPodGenTaskName, PodGenTask::class.java)
                project.tasks.register(sdk.toSetupBuildTaskName(pod), PodSetupBuildTask::class.java) {
                    it.group = TASK_GROUP
                    it.description = "Collect environment variables from .xcworkspace file"
                    it.pod = project.provider { pod }
                    it.sdk = project.provider { sdk }
                    it.podsXcodeProjDir = podGenTaskProvider.map { podGen -> podGen.podsXcodeProjDir.get() }
                    it.frameworkName = project.provider { cocoapodsExtension.frameworkName }
                    it.dependsOn(podGenTaskProvider)
                    it.onlyIf { isAvailableToProduceSynthetic }
                }
            }
        }
    }

    private fun registerPodBuildTasks(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val schemeNames = mutableSetOf<String>()

        cocoapodsExtension.pods.all { pod ->

            if (schemeNames.contains(pod.schemeName)) {
                return@all
            }
            schemeNames.add(pod.schemeName)

            val sdks = mutableSetOf<String>()

            kotlinExtension.supportedTargets().all loop@{ target ->

                val sdk = target.toValidSDK

                if (sdk in sdks) {
                    return@loop
                }
                sdks += sdk

                val podSetupBuildTaskProvider =
                    project.tasks.named(sdk.toSetupBuildTaskName(pod), PodSetupBuildTask::class.java)

                project.tasks.register(sdk.toBuildDependenciesTaskName(pod), PodBuildTask::class.java) {
                    it.group = TASK_GROUP
                    it.description = "Calls `xcodebuild` on xcworkspace for the pod scheme"
                    it.sdk = project.provider { sdk }
                    it.pod = project.provider { pod }
                    it.podsXcodeProjDir = podSetupBuildTaskProvider.map { task -> task.podsXcodeProjDir.get() }
                    it.buildSettingsFile = podSetupBuildTaskProvider.map { task -> task.buildSettingsFile.get() }
                    it.onlyIf { isAvailableToProduceSynthetic }
                }
            }
        }
    }

    private fun registerPodImportTask(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension
    ) {
        val podInstallTaskProvider = project.tasks.named(POD_INSTALL_TASK_NAME, PodInstallTask::class.java)
        project.tasks.register(POD_IMPORT_TASK_NAME) {
            it.group = TASK_GROUP
            it.description = "Called on Gradle sync, depends on Cinterop tasks for every used pod"
            it.dependsOn(podInstallTaskProvider)
            it.onlyIf { isAvailableToProduceSynthetic }

            kotlinExtension.supportedTargets().all { target ->
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.all { interop ->
                    val interopTaskProvider = project.tasks.named(interop.interopProcessingTaskName)
                    it.dependsOn(interopTaskProvider)
                }
            }
        }
    }

    override fun apply(project: Project): Unit = with(project) {

        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.multiplatformExtension
            val cocoapodsExtension = CocoapodsExtension(this)
            kotlinExtension.addExtension(COCOAPODS_EXTENSION_NAME, cocoapodsExtension)
            createDefaultFrameworks(kotlinExtension, cocoapodsExtension)
            registerDummyFrameworkTask(project, cocoapodsExtension)
            createSyncTask(project, kotlinExtension)
            registerPodspecTask(project, cocoapodsExtension)

            registerPodDownloadTask(project, cocoapodsExtension)
            registerPodGenTask(project, kotlinExtension, cocoapodsExtension)
            registerPodInstallTask(project, cocoapodsExtension)
            registerPodSetupBuildTasks(project, kotlinExtension, cocoapodsExtension)
            registerPodBuildTasks(project, kotlinExtension, cocoapodsExtension)
            registerPodImportTask(project, kotlinExtension)

            if (HostManager.hostIsMac && !isAvailableToProduceSynthetic) {
                logger.quiet(
                    """
                    To take advantage of the new functionality for Cocoapods Integration like synchronizing with the Xcode project 
                    and supporting dependencies on pods, please install the `cocoapods-generate` plugin for CocoaPods 
                    by calling `gem install cocoapods-generate` in terminal. 
                    
                    More details are available by https://github.com/square/cocoapods-generate
                """.trimIndent()
                )
            }
            createInterops(project, kotlinExtension, cocoapodsExtension)
        }
    }

    companion object {
        const val COCOAPODS_EXTENSION_NAME = "cocoapods"
        const val TASK_GROUP = "CocoaPods"
        const val SYNC_TASK_NAME = "syncFramework"
        const val POD_SPEC_TASK_NAME = "podspec"
        const val DUMMY_FRAMEWORK_TASK_NAME = "generateDummyFramework"
        const val POD_INSTALL_TASK_NAME = "podInstall"
        const val POD_DOWNLOAD_TASK_NAME = "podDownload"
        const val POD_GEN_TASK_NAME = "podGen"
        const val POD_SETUP_BUILD_TASK_NAME = "podSetupBuild"
        const val POD_BUILD_TASK_NAME = "podBuild"
        const val POD_IMPORT_TASK_NAME = "podImport"

        // We don't move these properties in PropertiesProvider because
        // they are not intended to be overridden in local.properties.
        const val TARGET_PROPERTY = "kotlin.native.cocoapods.target"
        const val CONFIGURATION_PROPERTY = "kotlin.native.cocoapods.configuration"

        const val CFLAGS_PROPERTY = "kotlin.native.cocoapods.cflags"
        const val HEADER_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.headers"
        const val FRAMEWORK_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.frameworks"

        const val GENERATE_WRAPPER_PROPERTY = "kotlin.native.cocoapods.generate.wrapper"

        // Used in Xcode script phase to indicate that the framework is being built for a device
        // so we should generate a fat framework with arm32 and arm64 binaries.
        const val KOTLIN_TARGET_FOR_IOS_DEVICE = "ios_arm"
        const val KOTLIN_TARGET_FOR_WATCHOS_DEVICE = "watchos_arm"

        val isAvailableToProduceSynthetic: Boolean by lazy {
            if (!HostManager.hostIsMac) {
                return@lazy false
            }

            val gemListProcess = ProcessBuilder("gem", "list").start()
            val gemListRetCode = gemListProcess.waitFor()
            val gemListOutput = gemListProcess.inputStream.use {
                it.reader().readText()
            }
            gemListRetCode == 0 && gemListOutput.contains("cocoapods-generate")
        }
    }
}