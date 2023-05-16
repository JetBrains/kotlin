/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.cocoapods

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.daemon.common.trimQuotes
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.KotlinArtifactsPodspecExtension
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.kotlinArtifactsPodspecExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.kotlinArtifactsExtension
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.asValidTaskName
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

internal val Project.cocoapodsBuildDirs: CocoapodsBuildDirs
    get() = CocoapodsBuildDirs(this)

internal class CocoapodsBuildDirs(val project: Project) {
    val root: File
        get() = project.buildDir.resolve("cocoapods")

    val framework: File
        get() = root.resolve("framework")

    val dummyFramework: File
        get() = root.resolve("dummy.framework")

    val defs: File
        get() = root.resolve("defs")

    val buildSettings: File
        get() = root.resolve("buildSettings")

    val synthetic: File
        get() = root.resolve("synthetic")

    fun synthetic(family: Family) = synthetic.resolve(family.platformLiteral)

    val publish: File = root.resolve("publish")

    fun fatFramework(buildType: NativeBuildType) =
        root.resolve("fat-frameworks/${buildType.getName()}")
}

internal fun String.asValidFrameworkName() = replace('-', '_')

internal val Family.platformLiteral: String
    get() = when (this) {
        Family.OSX -> "macos"
        Family.IOS -> "ios"
        Family.TVOS -> "tvos"
        Family.WATCHOS -> "watchos"
        else -> throw IllegalArgumentException("Bad family ${this.name}")
    }

private val Family.toPodGenTaskName: String
    get() = lowerCamelCaseName(KotlinCocoapodsPlugin.POD_GEN_TASK_NAME, platformLiteral)

private val Family.toPodInstallSyntheticTaskName: String
    get() = lowerCamelCaseName(KotlinCocoapodsPlugin.POD_INSTALL_TASK_NAME, "synthetic", platformLiteral)

private fun String.toSetupBuildTaskName(pod: CocoapodsDependency): String = lowerCamelCaseName(
    KotlinCocoapodsPlugin.POD_SETUP_BUILD_TASK_NAME,
    pod.schemeName.asValidTaskName(),
    this
)

private fun String.toBuildDependenciesTaskName(pod: CocoapodsDependency): String = lowerCamelCaseName(
    KotlinCocoapodsPlugin.POD_BUILD_TASK_NAME,
    pod.schemeName.asValidTaskName(),
    this
)

private val KotlinNativeTarget.toValidSDK: String
    get() = when (konanTarget) {
        IOS_X64, IOS_SIMULATOR_ARM64 -> "iphonesimulator"
        IOS_ARM32, IOS_ARM64 -> "iphoneos"
        WATCHOS_X86, WATCHOS_X64, WATCHOS_SIMULATOR_ARM64 -> "watchsimulator"
        WATCHOS_ARM32, WATCHOS_ARM64, WATCHOS_DEVICE_ARM64 -> "watchos"
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

//Make frameworks headers discoverable with any syntax (quotes, brackets, @import, etc.)
//https://github.com/CocoaPods/CocoaPods/blob/d18f49392c5e9ed9a2cdcb2ee89391cf7690ee5d/lib/cocoapods/target/build_settings.rb#L1188
private val PodBuildSettingsProperties.frameworkHeadersSearchPaths: List<String>
    get() = mutableListOf<String>().apply {
        headerPaths?.let { addAll(it.splitQuotedArgs()) }
        publicHeadersFolderPath?.let { add("${configurationBuildDir.trimQuotes()}/${it.trimQuotes()}") }
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

    private fun createDefaultFrameworks(kotlinExtension: KotlinMultiplatformExtension) {
        kotlinExtension.supportedTargets().all { target ->
            target.binaries.framework(POD_FRAMEWORK_PREFIX) {
                baseName = project.name.asValidFrameworkName()
            }
        }
    }

    private fun Project.createCopyFrameworkTask(
        originalDirectory: Provider<File>,
        buildingTask: TaskProvider<*>
    ) = registerTask<FrameworkCopy>(SYNC_TASK_NAME) {
        it.group = TASK_GROUP
        it.description = "Copies a framework for given platform and build type into the CocoaPods build directory"

        it.dependsOn(buildingTask)
        it.files = filesProvider { originalDirectory.map { dir -> dir.listFiles().orEmpty() } }
        it.destDir = cocoapodsBuildDirs.framework
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
        fatTargets.forEach { (platform, targets) ->
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
                    val framework = it.binaries.getFramework(POD_FRAMEWORK_PREFIX, requestedBuildType)
                    task.baseName = framework.baseName //all frameworks should have same names
                    task.from(framework)
                }
            }
        }

        project.createCopyFrameworkTask(fatFrameworkTask.map { it.destinationDir }, fatFrameworkTask)
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
        project.createCopyFrameworkTask(frameworkLinkTask.flatMap { it.destinationDirectory.map { dir -> dir.asFile } }, frameworkLinkTask)
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
                """.trimIndent()
            )
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
            Could not identify build type for Kotlin framework '${cocoapodsExtension.podFrameworkName.get()}' built via cocoapods plugin with CONFIGURATION=$xcodeConfiguration.
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
            if (pod.linkOnly || moduleNames.contains(pod.moduleName)) {
                return@all
            }
            moduleNames.add(pod.moduleName)

            val defTask = project.registerTask<DefFileTask>(
                lowerCamelCaseName("generateDef", pod.moduleName).asValidTaskName()
            ) {
                it.pod.set(pod)
                it.useLibraries.set(cocoapodsExtension.useLibraries)
                it.description = "Generates a def file for CocoaPods dependencies with module ${pod.moduleName}"
                // This task is an implementation detail so we don't add it in any group
                // to avoid showing it in the `tasks` output.
            }

            kotlinExtension.supportedTargets().all { target ->
                val cinterops = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops
                cinterops.create(pod.moduleName) { interop ->

                    val interopTask = project.tasks.named<CInteropProcess>(interop.interopProcessingTaskName).get()

                    interopTask.dependsOn(defTask)

                    pod.interopBindingDependencies.forEach { dependencyName ->
                        addPodDependencyToInterop(project, cocoapodsExtension, pod, cinterops, interop, dependencyName)
                    }

                    with(interop) {
                        defFileProperty.set(defTask.map { it.outputFile })
                        _packageNameProp.set(project.provider { pod.packageName })
                        _extraOptsProp.addAll(project.provider { pod.extraOpts })
                    }

                    if (HostManager.hostIsMac) {
                        val podBuildTaskProvider = project.getPodBuildTaskProvider(target, pod)
                        interopTask.inputs.file(podBuildTaskProvider.map { it.buildSettingsFile })
                        interopTask.dependsOn(podBuildTaskProvider)
                    }
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

                        interop.compilerOpts.addAll(podBuildSettings.frameworkHeadersSearchPaths.map { "-I$it" })
                        interop.compilerOpts.addAll(podBuildSettings.frameworkSearchPaths.map { "-F$it" })

                    }
                }
            }
        }
    }

    private fun addPodDependencyToInterop(
        project: Project,
        cocoapodsExtension: CocoapodsExtension,
        pod: CocoapodsDependency,
        cinterops: NamedDomainObjectContainer<DefaultCInteropSettings>,
        interop: DefaultCInteropSettings,
        dependencyName: String,
    ) {
        if (pod.name == dependencyName) {
            error("Pod '${pod.name}' has an interop-binding dependency on itself")
        }

        val dependencyPod = cocoapodsExtension.pods.findByName(dependencyName)
            ?: error("Couldn't find declaration of pod '$dependencyName' (interop-binding dependency of pod '${pod.name}')")

        val dependencyTaskName = cinterops.getByName(dependencyPod.moduleName).interopProcessingTaskName
        val dependencyTask = project.tasks.named<CInteropProcess>(dependencyTaskName)

        interop.dependencyFiles += project.files(dependencyTask.map { it.outputFile }).builtBy(dependencyTask)

        dependencyPod.interopBindingDependencies.forEach { transitiveDependency ->
            addPodDependencyToInterop(project, cocoapodsExtension, pod, cinterops, interop, transitiveDependency)
        }
    }

    private fun registerDummyFrameworkTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
        project.registerTask<DummyFrameworkTask>(DUMMY_FRAMEWORK_TASK_NAME) { task ->
            task.frameworkName.set(cocoapodsExtension.podFrameworkName)
            task.useStaticFramework.set(cocoapodsExtension.podFrameworkIsStatic)
        }
    }

    private fun registerPodspecTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
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
            it.frameworkName = cocoapodsExtension.podFrameworkName
            it.ios = project.provider { cocoapodsExtension.ios }
            it.osx = project.provider { cocoapodsExtension.osx }
            it.tvos = project.provider { cocoapodsExtension.tvos }
            it.watchos = project.provider { cocoapodsExtension.watchos }
            val generateWrapper = project.findProperty(GENERATE_WRAPPER_PROPERTY)?.toString()?.toBoolean() ?: false
            if (generateWrapper) {
                it.dependsOn(":wrapper")
            }
        }
    }

    private fun registerPodspecTask(
        project: Project,
        artifact: KotlinNativeArtifact,
        podspecExtension: KotlinArtifactsPodspecExtension,
        cocoapodsExtension: CocoapodsExtension,
    ) {
        val artifactName = artifact.artifactName
        val assembleTask = project.tasks.named(artifact.taskName)
        val podspecTaskName = lowerCamelCaseName("generate", artifact.name, "podspec")

        val artifactType = when (artifact) {
            is KotlinNativeLibrary -> when {
                artifact.isStatic -> GenerateArtifactPodspecTask.ArtifactType.StaticLibrary
                else -> GenerateArtifactPodspecTask.ArtifactType.DynamicLibrary
            }
            is KotlinNativeFramework -> GenerateArtifactPodspecTask.ArtifactType.Framework
            is KotlinNativeFatFramework -> GenerateArtifactPodspecTask.ArtifactType.FatFramework
            is KotlinNativeXCFramework -> GenerateArtifactPodspecTask.ArtifactType.XCFramework
            else -> error("Podspec can only be generated for Library, Framework, FatFramework or XCFramework")
        }

        val podspecTask = project.tasks.register(podspecTaskName, GenerateArtifactPodspecTask::class.java) { task ->
            task.group = TASK_GROUP
            task.description = "Generates a podspec file for '$artifactName' artifact"
            task.specName.set(artifactName)
            task.specVersion.set(project.version.takeIf { it != Project.DEFAULT_VERSION }.toString())
            task.destinationDir.set(project.buildDir.resolve(artifact.outDir))
            task.attributes.set(podspecExtension.attributes)
            task.rawStatements.set(podspecExtension.rawStatements)
            task.dependencies.set(cocoapodsExtension.pods)
            task.artifactType.set(artifactType)
        }

        assembleTask.dependsOn(podspecTask)
    }

    private fun injectPodspecExtensionToArtifacts(
        project: Project,
        artifactsExtension: KotlinArtifactsExtension,
        cocoapodsExtension: CocoapodsExtension,
    ) {
        artifactsExtension.artifactConfigs.withType(KotlinNativeArtifactConfig::class.java) { artifactConfig ->
            val podspecExtension = project.objects.newInstance<KotlinArtifactsPodspecExtension>()
            artifactConfig.addExtension(ARTIFACTS_PODSPEC_EXTENSION_NAME, podspecExtension)
        }

        artifactsExtension.artifacts.withType(KotlinNativeArtifact::class.java) { artifact ->
            val podspecExtension = requireNotNull(artifact.kotlinArtifactsPodspecExtension)

            registerPodspecTask(project, artifact, podspecExtension, cocoapodsExtension)
        }
    }

    private fun registerPodInstallTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val podspecTaskProvider = project.tasks.named<PodspecTask>(POD_SPEC_TASK_NAME)
        val dummyFrameworkTaskProvider = project.tasks.named<DummyFrameworkTask>(DUMMY_FRAMEWORK_TASK_NAME)
        project.registerTask<PodInstallTask>(POD_INSTALL_TASK_NAME) { task ->
            task.group = TASK_GROUP
            task.description = "Invokes `pod install` call within Podfile location directory"
            task.podfile.set(project.provider { cocoapodsExtension.podfile })
            task.podspec.set(podspecTaskProvider.map { it.outputFile })
            task.frameworkName.set(cocoapodsExtension.podFrameworkName)
            task.specRepos.set(project.provider { cocoapodsExtension.specRepos })
            task.pods.set(cocoapodsExtension.pods)
            task.dummyFramework.set(dummyFrameworkTaskProvider.map { it.outputFramework.get() })
            task.dependsOn(podspecTaskProvider)
        }
    }

    private fun registerSyntheticPodTasks(
        project: Project, kotlinExtension: KotlinMultiplatformExtension, cocoapodsExtension: CocoapodsExtension
    ) {
        val families = mutableSetOf<Family>()

        val podspecTaskProvider = project.tasks.named<PodspecTask>(POD_SPEC_TASK_NAME)
        kotlinExtension.supportedTargets().all { target ->
            val family = target.konanTarget.family
            if (family in families) {
                return@all
            }
            families += family

            val platformSettings = when (family) {
                Family.IOS -> cocoapodsExtension.ios
                Family.OSX -> cocoapodsExtension.osx
                Family.TVOS -> cocoapodsExtension.tvos
                Family.WATCHOS -> cocoapodsExtension.watchos
                else -> error("Unknown cocoapods platform: $family")
            }

            val podGenTask = project.registerTask<PodGenTask>(family.toPodGenTaskName) { task ->
                task.description = "Сreates a synthetic Xcode project to retrieve CocoaPods dependencies"
                task.podspec.set(podspecTaskProvider.map { it.outputFile })
                task.podName.set(project.provider { cocoapodsExtension.name })
                task.useLibraries.set(project.provider { cocoapodsExtension.useLibraries })
                task.specRepos.set(project.provider { cocoapodsExtension.specRepos })
                task.family.set(family)
                task.platformSettings.set(platformSettings)
                task.pods.set(cocoapodsExtension.pods)
                task.xcodeVersion.set(Xcode?.currentVersion)
            }

            project.registerTask<PodInstallSyntheticTask>(family.toPodInstallSyntheticTaskName) { task ->
                task.description = "Invokes `pod install` for synthetic project"
                task.podfile.set(podGenTask.map { it.podfile.get() })
                task.family.set(family)
                task.podName.set(cocoapodsExtension.name)
                task.dependsOn(podGenTask)
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

                val podInstallTask = project.tasks.named<PodInstallSyntheticTask>(target.konanTarget.family.toPodInstallSyntheticTaskName)
                project.registerTask<PodSetupBuildTask>(sdk.toSetupBuildTaskName(pod)) { task ->
                    task.group = TASK_GROUP
                    task.description = "Collect environment variables from .xcworkspace file"
                    task.pod = project.provider { pod }
                    task.sdk = project.provider { sdk }
                    task.podsXcodeProjDir = podInstallTask.map { it.podsXcodeProjDirProvider.get() }
                    task.frameworkName = cocoapodsExtension.podFrameworkName
                    task.dependsOn(podInstallTask)
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
        val podImportTaskProvider = project.tasks.register(POD_IMPORT_TASK_NAME) {

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

        /* Older IDEs will explicitly call 'podImport' instead */
        @OptIn(Idea222Api::class)
        project.ideaImportDependsOn(podImportTaskProvider)
    }

    private fun configureLinkingOptions(project: Project, cocoapodsExtension: CocoapodsExtension) {
        project.multiplatformExtension.supportedTargets().all { target ->
            target.binaries.all { binary ->
                val testExecutable = binary is TestExecutable
                val podFramework = binary is Framework && binary.name.startsWith(POD_FRAMEWORK_PREFIX)
                if (testExecutable || podFramework) {
                    configureLinkingOptions(project, cocoapodsExtension, binary)
                }
            }
        }
    }

    private fun configureLinkingOptions(project: Project, cocoapodsExtension: CocoapodsExtension, nativeBinary: NativeBinary) {
        cocoapodsExtension.pods.all { pod ->
            nativeBinary.linkTaskProvider.configure { task ->
                val binary = task.binary
                if (HostManager.hostIsMac) {
                    val podBuildTaskProvider = project.getPodBuildTaskProvider(binary.target, pod)
                    task.inputs.file(podBuildTaskProvider.map { it.buildSettingsFile })
                    task.dependsOn(podBuildTaskProvider)
                }

                task.doFirst { _ ->
                    val isExecutable = binary is AbstractExecutable
                    val isDynamicFramework = (binary is Framework && !binary.isStatic)

                    val podBuildSettings = project.getPodBuildSettingsProperties(binary.target, pod)
                    val frameworkFileName = pod.moduleName + ".framework"
                    val frameworkSearchPaths = podBuildSettings.frameworkSearchPaths

                    if (isExecutable || isDynamicFramework) {
                        val frameworkFileExists = frameworkSearchPaths.any { dir -> File(dir, frameworkFileName).exists() }
                        if (frameworkFileExists) binary.linkerOpts.addArg("-framework", pod.moduleName)
                        binary.linkerOpts.addAll(frameworkSearchPaths.map { "-F$it" })
                    }

                    if (isExecutable) binary.linkerOpts.addArgs("-rpath", frameworkSearchPaths)
                }
            }
        }
    }

    private fun registerPodXCFrameworkTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension,
        buildType: NativeBuildType
    ): TaskProvider<XCFrameworkTask> =
        with(project) {
            registerTask(lowerCamelCaseName(POD_FRAMEWORK_PREFIX, "publish", buildType.getName(), "XCFramework")) { task ->
                multiplatformExtension.supportedTargets().all { target ->
                    target.binaries.matching { it.buildType == buildType && it.name.startsWith(POD_FRAMEWORK_PREFIX) }
                        .withType(Framework::class.java) { framework ->
                            task.from(framework)
                        }
                }
                task.outputDir = cocoapodsExtension.publishDir
                task.buildType = buildType
                task.baseName = cocoapodsExtension.podFrameworkName
                task.description = "Produces ${buildType.getName().capitalizeAsciiOnly()} XCFramework for all requested targets"
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
            val task =
                tasks.register(lowerCamelCaseName(POD_FRAMEWORK_PREFIX, "spec", buildType.getName()), PodspecTask::class.java) { task ->
                    task.description = "Generates podspec for ${buildType.getName().capitalizeAsciiOnly()} XCFramework publishing"
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
                    task.frameworkName = cocoapodsExtension.podFrameworkName
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
                target.binaries.matching { it.buildType == buildType && it.name.startsWith(POD_FRAMEWORK_PREFIX) }
                    .withType(Framework::class.java) { framework ->

                        val appleTarget = AppleTarget.values().firstOrNull { it.targets.contains(target.konanTarget) } ?: return@withType
                        val fatFrameworkTaskName =
                            lowerCamelCaseName(POD_FRAMEWORK_PREFIX, buildType.getName(), appleTarget.targetName, "FatFramework")
                        val fatFrameworkTask = locateOrRegisterTask<FatFrameworkTask>(fatFrameworkTaskName) { fatTask ->
                            fatTask.baseName = framework.baseName
                            fatTask.destinationDir =
                                XCFrameworkTask.fatFrameworkDir(this, fatTask.fatFrameworkName, buildType, appleTarget)
                            fatTask.onlyIf {
                                fatTask.frameworks.size > 1
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

    private fun checkLinkOnlyNotUsedWithStaticFramework(project: Project, cocoapodsExtension: CocoapodsExtension) {
        project.runProjectConfigurationHealthCheckWhenEvaluated {
            cocoapodsExtension.pods.all { pod ->
                if (pod.linkOnly && cocoapodsExtension.podFrameworkIsStatic.get()) {
                    logger.warn("""
                        Dependency on '${pod.name}' with option 'linkOnly=true' is unused for building static frameworks.
                        When using static linkage you will need to provide all dependencies for linking the framework into a final application.
                    """.trimIndent())
                }
            }
        }
    }

    override fun apply(project: Project): Unit = with(project) {

        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.multiplatformExtension
            val kotlinArtifactsExtension = project.kotlinArtifactsExtension
            val cocoapodsExtension = project.objects.newInstance(CocoapodsExtension::class.java, this)

            kotlinExtension.addExtension(COCOAPODS_EXTENSION_NAME, cocoapodsExtension)

            createDefaultFrameworks(kotlinExtension)
            registerDummyFrameworkTask(project, cocoapodsExtension)
            createSyncTask(project, kotlinExtension, cocoapodsExtension)
            injectPodspecExtensionToArtifacts(project, kotlinArtifactsExtension, cocoapodsExtension)
            registerPodspecTask(project, cocoapodsExtension)

            registerSyntheticPodTasks(project, kotlinExtension, cocoapodsExtension)
            registerPodInstallTask(project, cocoapodsExtension)
            registerPodSetupBuildTasks(project, kotlinExtension, cocoapodsExtension)
            registerPodBuildTasks(project, kotlinExtension, cocoapodsExtension)
            registerPodImportTask(project, kotlinExtension)
            registerPodPublishTasks(project, cocoapodsExtension)

            if (!HostManager.hostIsMac) {
                logger.warn(
                    """
                        Kotlin Cocoapods Plugin is fully supported on mac machines only. Gradle tasks that can not run on non-mac hosts will be skipped.
                    """.trimIndent()
                )
            }
            createInterops(project, kotlinExtension, cocoapodsExtension)
            configureLinkingOptions(project, cocoapodsExtension)
            checkLinkOnlyNotUsedWithStaticFramework(project, cocoapodsExtension)
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
        const val POD_GEN_TASK_NAME = "podGen"
        const val POD_SETUP_BUILD_TASK_NAME = "podSetupBuild"
        const val POD_BUILD_TASK_NAME = "podBuild"
        const val POD_IMPORT_TASK_NAME = "podImport"
        const val ARTIFACTS_PODSPEC_EXTENSION_NAME = "withPodspec"

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
    }
}

/**
 * Extends a KotlinArtifact with a corresponding Podspec
 *
 * Only needed in *.kts build files. In Groovy you can use the same syntax but without explicit extension import
 */
fun KotlinNativeArtifactConfig.withPodspec(configure: KotlinArtifactsPodspecExtension.() -> Unit) {
    val extension = cast<ExtensionAware>().kotlinArtifactsPodspecExtension

    checkNotNull(extension) { "CocoaPods plugin should be applied before using `${KotlinCocoapodsPlugin.ARTIFACTS_PODSPEC_EXTENSION_NAME}` extension" }

    extension.configure()
}
