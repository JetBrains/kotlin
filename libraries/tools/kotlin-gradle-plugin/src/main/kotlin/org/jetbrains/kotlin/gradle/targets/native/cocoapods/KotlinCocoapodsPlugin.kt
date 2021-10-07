/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.cocoapods

import org.gradle.api.DefaultTask
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
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.asValidTaskName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import java.io.File

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

    val publish: File = root.resolve("publish")

    fun externalSources(fileName: String) = externalSources.resolve(fileName)

    fun fatFramework(buildType: NativeBuildType) =
        root.resolve("fat-frameworks/${buildType.toString().toLowerCase()}")
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

private val KotlinNativeTarget.toValidSDK: String
    get() = when (konanTarget) {
        IOS_X64, IOS_SIMULATOR_ARM64 -> "iphonesimulator"
        IOS_ARM32, IOS_ARM64 -> "iphoneos"
        WATCHOS_X86, WATCHOS_X64, WATCHOS_SIMULATOR_ARM64 -> "watchsimulator"
        WATCHOS_ARM32, WATCHOS_ARM64 -> "watchos"
        TVOS_X64, TVOS_SIMULATOR_ARM64 -> "appletvsimulator"
        TVOS_ARM64 -> "appletvos"
        MACOS_X64, MACOS_ARM64 -> "macosx"
        else -> throw IllegalArgumentException("Bad target ${konanTarget.name}.")
    }

internal fun Project.getPodBuildTaskProvider(
    target: KotlinNativeTarget,
    pod: CocoapodsDependency
): TaskProvider<PodBuildTask> {
    return tasks.named(target.toValidSDK.toBuildDependenciesTaskName(pod), PodBuildTask::class.java)
}

internal fun Project.getPodBuildSettingsProperties(
    target: KotlinNativeTarget,
    pod: CocoapodsDependency
): PodBuildSettingsProperties {
    return getPodBuildTaskProvider(target, pod).get().buildSettingsFile.get()
        .reader()
        .use {
            PodBuildSettingsProperties.readSettingsFromReader(it)
        }
}

internal val PodBuildSettingsProperties.frameworkSearchPaths: List<String>
    get() {
        val frameworkPathsSelfIncluding = mutableListOf<String>()
        frameworkPathsSelfIncluding += configurationBuildDir.trimQuotes()
        frameworkPaths?.let { frameworkPathsSelfIncluding.addAll(it.splitQuotedArgs()) }
        return frameworkPathsSelfIncluding
    }

/**
 * Splits a string using a whitespace characters as delimiters.
 * Ignores whitespaces in quotes and drops quotes, e.g. a string
 * `foo "bar baz" qux="quux"` will be split into ["foo", "bar baz", "qux=quux"].
 */
internal fun String.splitQuotedArgs(): List<String> =
    Regex("""(?:[^\s"]|(?:"[^"]*"))+""").findAll(this).map {
        it.value.replace("\"", "")
    }.toList()

internal fun KotlinMultiplatformExtension.supportedTargets() = targets
    .withType(KotlinNativeTarget::class.java)
    .matching { it.konanTarget.family.isAppleFamily }


open class KotlinCocoapodsPlugin : Plugin<Project> {

    private fun KotlinMultiplatformExtension.targetsForPlatform(requestedPlatform: KonanTarget) =
        supportedTargets().matching { it.konanTarget == requestedPlatform }

    private fun createDefaultFrameworks(kotlinExtension: KotlinMultiplatformExtension, cocoapodsExtension: CocoapodsExtension) {
        kotlinExtension.supportedTargets().all { target ->
            target.binaries.framework(POD_FRAMEWORK_PREFIX) {
                baseName = cocoapodsExtension.frameworkNameInternal
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
        requestedBuildType: NativeBuildType,
        requestedPlatforms: List<KonanTarget>
    ) {
        val fatTargets = requestedPlatforms.associateWith { kotlinExtension.targetsForPlatform(it) }

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
            task.description = "Creates a fat framework for requested architectures"
            task.destinationDir = project.cocoapodsBuildDirs.fatFramework(requestedBuildType)

            fatTargets.forEach { (_, targets) ->
                targets.singleOrNull()?.let {
                    task.from(it.binaries.getFramework(POD_FRAMEWORK_PREFIX, requestedBuildType))
                }
            }
        }

        project.createSyncFrameworkTask(fatFrameworkTask.map { it.destinationDir }, fatFrameworkTask)
    }

    private fun createSyncForRegularFramework(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        requestedBuildType: NativeBuildType,
        requestedPlatform: KonanTarget
    ) {
        val targets = kotlinExtension.targetsForPlatform(requestedPlatform)

        check(targets.isNotEmpty()) { "The project doesn't contain a target for the requested platform: `${requestedPlatform.visibleName}`" }
        check(targets.size == 1) { "The project has more than one target for the requested platform: `${requestedPlatform.visibleName}`" }

        val frameworkLinkTask = targets.single().binaries.getFramework(POD_FRAMEWORK_PREFIX, requestedBuildType).linkTaskProvider
        project.createSyncFrameworkTask(frameworkLinkTask.map { it.destinationDir }, frameworkLinkTask)
    }

    private fun createSyncTask(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        cocoapodsExtension: CocoapodsExtension
    ) = project.whenEvaluated {
        val xcodeConfiguration = project.findProperty(CONFIGURATION_PROPERTY)?.toString() ?: return@whenEvaluated
        val platforms = project.findProperty(PLATFORM_PROPERTY)?.toString()?.split(",", " ")?.filter { it.isNotBlank() }
        val archs = project.findProperty(ARCHS_PROPERTY)?.toString()?.split(",", " ")?.filter { it.isNotBlank() }

        if (
            project.findProperty(CFLAGS_PROPERTY) != null ||
            project.findProperty(FRAMEWORK_PATHS_PROPERTY) != null ||
            project.findProperty(HEADER_PATHS_PROPERTY) != null
        ) {
            logger.warn(
                """
                Properties 
                    kotlin.native.cocoapods.cflags
                    kotlin.native.cocoapods.paths.frameworks
                    kotlin.native.cocoapods.paths.headers
                are not supported and will be ignored since Cocoapods plugin generates all required properties automatically.
                """.trimIndent())
        }

        if (platforms == null || archs == null) {
            check(project.findProperty(TARGET_PROPERTY) == null) {
                """
                $TARGET_PROPERTY property was dropped in favor of $PLATFORM_PROPERTY and $ARCHS_PROPERTY. 
                Podspec file might be outdated. Sync project with Gradle files or run the 'podspec' task manually to regenerate it.
                """.trimIndent()
            }
            return@whenEvaluated
        }

        check(platforms.size == 1) {
            "$PLATFORM_PROPERTY has to contain a single value only. If building for multiple platforms is required, consider using XCFrameworks"
        }

        val platform = platforms.first()

        val nativeTargets = AppleSdk.defineNativeTargets(platform, archs)

        check(nativeTargets.isNotEmpty()) { "Could not identify native targets for platform: '$platform' and architectures: '$archs'" }

        val requestedBuildType = cocoapodsExtension.xcodeConfigurationToNativeBuildType[xcodeConfiguration]

        check(requestedBuildType != null) {
            """
            Could not identify build type for Kotlin framework '${cocoapodsExtension.frameworkNameInternal}' built via cocoapods plugin with CONFIGURATION=$xcodeConfiguration.
            Add xcodeConfigurationToNativeBuildType["$xcodeConfiguration"]=NativeBuildType.DEBUG or xcodeConfigurationToNativeBuildType["$xcodeConfiguration"]=NativeBuildType.RELEASE to cocoapods plugin configuration
        """.trimIndent()
        }

        val frameworkTargets = nativeTargets.flatMap { kotlinExtension.targetsForPlatform(it) }
        if (frameworkTargets.size == 1) {
            // Fast path: there is only one device target. There is no need to build a fat framework.
            createSyncForRegularFramework(project, kotlinExtension, requestedBuildType, frameworkTargets.single().konanTarget)
        } else {
            // There are several device targets so we need to build a fat framework.
            createSyncForFatFramework(project, kotlinExtension, requestedBuildType, nativeTargets)
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

                    val podBuildTaskProvider = project.getPodBuildTaskProvider(target, pod)
                    interopTask.inputs.file(podBuildTaskProvider.map {it.buildSettingsFile })
                    interopTask.dependsOn(podBuildTaskProvider)

                    interopTask.doFirst { _ ->
                        // Since we cannot expand the configuration phase of interop tasks
                        // receiving the required environment variables happens on execution phase.
                        // TODO This needs to be fixed to improve UP-TO-DATE checks.
                        val podBuildSettings = project.getPodBuildSettingsProperties(target, pod)

                        podBuildSettings.cflags?.let { args ->
                            // Xcode quotes around paths with spaces.
                            // Here and below we need to split such paths taking this into account.
                            interop.compilerOpts.addAll(args.splitQuotedArgs())
                        }
                        podBuildSettings.headerPaths?.let { args ->
                            interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-I$it" })
                        }

                        interop.compilerOpts.addAll(podBuildSettings.frameworkSearchPaths.map { "-F$it" })

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
            it.frameworkName = project.provider { cocoapodsExtension.frameworkNameInternal }
            it.useDynamicFramework = project.provider { cocoapodsExtension.useDynamicFramework }
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
            it.publishing.set(false)
            it.pods.set(cocoapodsExtension.pods)
            it.version.set(cocoapodsExtension.version ?: project.version.toString())
            it.specName.set(cocoapodsExtension.name)
            it.extraSpecAttributes.set(cocoapodsExtension.extraSpecAttributes)
            it.outputDir.set(project.projectDir)
            it.homepage.set(cocoapodsExtension.homepage)
            it.license.set(cocoapodsExtension.license)
            it.authors.set(cocoapodsExtension.authors)
            it.summary.set(cocoapodsExtension.summary)
            it.frameworkName = project.provider { cocoapodsExtension.frameworkNameInternal }
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
            it.frameworkName = project.provider { cocoapodsExtension.frameworkNameInternal }
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
            val downloadPodTask = when (val podSource = pod.source) {
                is Git -> project.tasks.register(pod.toPodDownloadTaskName, PodDownloadGitTask::class.java) {
                    it.podName = project.provider { pod.name.asValidTaskName() }
                    it.podSource = project.provider<Git> { podSource }
                }
                is Url -> project.tasks.register(pod.toPodDownloadTaskName, PodDownloadUrlTask::class.java) {
                    it.podName = project.provider { pod.name.asValidTaskName() }
                    it.podSource = project.provider<Url> { podSource }
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
                it.podspec = podspecTaskProvider.map { task -> task.outputFile }
                it.useLibraries = project.provider { cocoapodsExtension.useLibraries }
                it.specRepos = project.provider { cocoapodsExtension.specRepos }
                it.family = family
                it.pods.set(cocoapodsExtension.pods)
                it.dependsOn(downloadPods)
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
                    it.frameworkName = project.provider { cocoapodsExtension.frameworkNameInternal }
                    it.dependsOn(podGenTaskProvider)
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

            kotlinExtension.supportedTargets().all { target ->
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.all { interop ->
                    val interopTaskProvider = project.tasks.named(interop.interopProcessingTaskName)
                    it.dependsOn(interopTaskProvider)
                }
            }
        }
    }

    private fun configureTestBinaries(project: Project, cocoapodsExtension: CocoapodsExtension) {
        project.multiplatformExtension.supportedTargets().all { target ->
            target.binaries.withType(TestExecutable::class.java) { testExecutable ->
                cocoapodsExtension.configureLinkingOptions(testExecutable, setRPath = true)
            }
        }
    }

    private fun registerPodXCFrameworkTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension,
        buildType: NativeBuildType
    ): TaskProvider<XCFrameworkTask> =
        with(project) {
            registerTask("podPublish${buildType.name.toLowerCase().capitalize()}XCFramework") { task ->
                multiplatformExtension.supportedTargets().all { target ->
                    target.binaries.matching { it.buildType == buildType }.withType(Framework::class.java) { framework ->
                        task.from(framework)
                    }
                }
                task.outputDir = cocoapodsExtension.publishDir
                task.buildType = buildType
                task.baseName = project.provider { cocoapodsExtension.frameworkNameInternal }
                task.description = "Produces ${buildType.name.toLowerCase().capitalize()} XCFramework for all requested targets"
                task.group = TASK_GROUP
            }
        }

    private fun registerPodspecPublishTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension,
        xcFrameworkTask: TaskProvider<XCFrameworkTask>,
        buildType: NativeBuildType
    ): TaskProvider<PodspecTask> =
        with(project) {
            val task = tasks.register("podSpec${buildType.name.toLowerCase().capitalize()}", PodspecTask::class.java) { task ->
                task.description = "Generates podspec for ${buildType.name.toLowerCase().capitalize()} XCFramework publishing"
                task.outputDir.set(xcFrameworkTask.map { it.outputDir.resolve(it.buildType.getName()) })
                task.needPodspec = provider { true }
                task.publishing.set(true)
                task.pods.set(cocoapodsExtension.pods)
                task.specName.set(cocoapodsExtension.name)
                task.version.set(cocoapodsExtension.version ?: version.toString())
                task.extraSpecAttributes.set(cocoapodsExtension.extraSpecAttributes)
                task.homepage.set(cocoapodsExtension.homepage)
                task.license.set(cocoapodsExtension.license)
                task.authors.set(cocoapodsExtension.authors)
                task.summary.set(cocoapodsExtension.summary)
                task.source.set(cocoapodsExtension.source)
                task.frameworkName = provider { cocoapodsExtension.frameworkNameInternal }
                task.ios = provider { cocoapodsExtension.ios }
                task.osx = provider { cocoapodsExtension.osx }
                task.tvos = provider { cocoapodsExtension.tvos }
                task.watchos = provider { cocoapodsExtension.watchos }
                val generateWrapper = project.findProperty(GENERATE_WRAPPER_PROPERTY)?.toString()?.toBoolean() ?: false
                if (generateWrapper) {
                    task.dependsOn(":wrapper")
                }
            }
            xcFrameworkTask.dependsOn(task)
            return task
        }

    private fun registerPodPublishFatFrameworkTasks(
        project: Project,
        xcFrameworkTask: TaskProvider<XCFrameworkTask>,
        buildType: NativeBuildType
    ) =
        with(project) {
            multiplatformExtension.supportedTargets().all { target ->
                target.binaries.matching { it.buildType == buildType }.withType(Framework::class.java) { framework ->

                    val appleTarget = AppleTarget.values().firstOrNull { it.targets.contains(target.konanTarget) } ?: return@withType
                    val fatFrameworkTaskName =
                        "pod${buildType.name.toLowerCase().capitalize()}${appleTarget.targetName.capitalize()}FatFramework"
                    val fatFrameworkTask = if (fatFrameworkTaskName in tasks.names) {
                        tasks.named(fatFrameworkTaskName, FatFrameworkTask::class.java)
                    } else {
                        tasks.register(fatFrameworkTaskName, FatFrameworkTask::class.java) { fatTask ->
                            fatTask.baseName = framework.baseName
                            fatTask.destinationDir = XCFrameworkTask.fatFrameworkDir(this, fatTask.fatFrameworkName, buildType, appleTarget)
                            fatTask.onlyIf {
                                fatTask.frameworks.size > 1
                            }
                        }
                    }

                    fatFrameworkTask.configure {
                        it.from(framework)
                    }

                    xcFrameworkTask.dependsOn(fatFrameworkTask)
                }
            }
        }

    private fun registerPodPublishTasks(project: Project, cocoapodsExtension: CocoapodsExtension) {

        val xcFrameworkTasks = NativeBuildType.values().map { buildType ->
            val xcFrameworkTask = registerPodXCFrameworkTask(project, cocoapodsExtension, buildType)
            registerPodPublishFatFrameworkTasks(project, xcFrameworkTask, buildType)
            registerPodspecPublishTask(project, cocoapodsExtension, xcFrameworkTask, buildType)
            xcFrameworkTask
        }

        project.registerTask("podPublishXCFramework", DefaultTask::class.java) { task ->
            task.description = "Produces Release and Debug XCFrameworks with respective podspecs"
            task.dependsOn(xcFrameworkTasks)
            task.group = TASK_GROUP
        }
    }

    override fun apply(project: Project): Unit = with(project) {

        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.multiplatformExtension
            val cocoapodsExtension = CocoapodsExtension(this)
            kotlinExtension.addExtension(COCOAPODS_EXTENSION_NAME, cocoapodsExtension)
            createDefaultFrameworks(kotlinExtension, cocoapodsExtension)
            registerDummyFrameworkTask(project, cocoapodsExtension)
            createSyncTask(project, kotlinExtension, cocoapodsExtension)
            registerPodspecTask(project, cocoapodsExtension)

            registerPodDownloadTask(project, cocoapodsExtension)
            registerPodGenTask(project, kotlinExtension, cocoapodsExtension)
            registerPodInstallTask(project, cocoapodsExtension)
            registerPodSetupBuildTasks(project, kotlinExtension, cocoapodsExtension)
            registerPodBuildTasks(project, kotlinExtension, cocoapodsExtension)
            registerPodImportTask(project, kotlinExtension)
            registerPodPublishTasks(project, cocoapodsExtension)

            if (HostManager.hostIsMac && !isAvailableToProduceSynthetic) {
                logger.quiet(
                    """
                        Dependency on pods requires cocoapods-generate plugin to be installed.
                        If you plan to add dependencies on third party pods, don't forget to install it by executing 'gem install cocoapods-generate' in terminal.
                    """.trimIndent()
                )
            }
            createInterops(project, kotlinExtension, cocoapodsExtension)
            configureTestBinaries(project, cocoapodsExtension)
        }
    }

    companion object {
        const val COCOAPODS_EXTENSION_NAME = "cocoapods"
        const val TASK_GROUP = "CocoaPods"
        const val POD_FRAMEWORK_PREFIX = "pod"
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
        const val PLATFORM_PROPERTY = "kotlin.native.cocoapods.platform"
        const val TARGET_PROPERTY = "kotlin.native.cocoapods.target"
        const val ARCHS_PROPERTY = "kotlin.native.cocoapods.archs"
        const val CONFIGURATION_PROPERTY = "kotlin.native.cocoapods.configuration"

        const val CFLAGS_PROPERTY = "kotlin.native.cocoapods.cflags"
        const val HEADER_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.headers"
        const val FRAMEWORK_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.frameworks"

        const val GENERATE_WRAPPER_PROPERTY = "kotlin.native.cocoapods.generate.wrapper"

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
