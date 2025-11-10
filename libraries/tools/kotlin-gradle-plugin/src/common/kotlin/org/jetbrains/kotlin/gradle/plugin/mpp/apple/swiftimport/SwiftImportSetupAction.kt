@file:Suppress("SENSELESS_COMPARISON")

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_METADATA
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ConvertSyntheticSwiftPMImportProjectIntoDefFile.Companion.DUMP_FILE_ARGS_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Platform
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.property
import org.jetbrains.kotlin.gradle.plugin.testTaskName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.fileProperty
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import java.security.MessageDigest
import java.util.UUID
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.io.readLines

internal fun Project.swiftPMDependenciesExtension(): SwiftImportExtension {
    val existingExtension = kotlinExtension.extensions.findByName(SwiftImportExtension.EXTENSION_NAME)
    if (existingExtension != null) {
        return existingExtension as SwiftImportExtension
    }
    kotlinExtension.extensions.create(
        SwiftImportExtension.EXTENSION_NAME,
        SwiftImportExtension::class.java
    )
    return kotlinExtension.getExtension<SwiftImportExtension>(
        SwiftImportExtension.EXTENSION_NAME
    )!!
}


internal val SwiftImportSetupAction = KotlinProjectSetupAction {
    val kotlinExtension = project.multiplatformExtension
    val swiftPMImportExtension = swiftPMDependenciesExtension()

    val productTypeProvider = provider {
        val hasDynamicFrameworks = kotlinExtension.targets.filterIsInstance<KotlinNativeTarget>().any { target ->
            target.binaries.filterIsInstance<Framework>().any {
                !it.isStatic
            }
        }
        if (hasDynamicFrameworks) {
            /**
             * FIXME: This is not correct: SwiftPM can promote products to be dynamic libraries when necessary and if we emit a package with
             * type: .none, then this will not happen. However, we also
             * - can't produce a dynamic library because it leads to symbol duplication with the K/N dynamic framework
             * - can't pass always static K/N framework to this SwiftPM linkage
             * - can't hack with linker settings because SwiftPM passes these settings to all downstream linkage sites???
             *
             * Things to try in the future:
             * - Redo the entire integration using an .pbxproj instead of the Package and hack something up in this linkage project file
             * - Reexport all potential libraries for dynamic K/N framework from the linkage shim (was there an issue with "private extern" in public API of some Google library?)
             */
//             SyntheticProductType.INFERRED
            SyntheticProductType.DYNAMIC
        } else {
            SyntheticProductType.INFERRED
        }
    }

    val swiftPMDependenciesMetadata = project.registerTask<SerializeSwiftPMDependenciesMetadata>(
        SerializeSwiftPMDependenciesMetadata.TASK_NAME,
    ) {
        it.configureWithExtension(swiftPMImportExtension)
    }
    project.configurations.createConsumable("swiftPMDependenciesMetadataElements") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        outgoing.artifact(swiftPMDependenciesMetadata)
    }

    val transitiveSwiftPMDependenciesMap = swiftPMDependenciesMetadataClasspath()
    val transitiveLocalSwiftPMDependencies = transitiveSwiftPMDependenciesMap.map {
        it.values.flatMap { swiftPMDependencies ->
            swiftPMDependencies.dependencies.mapNotNull { dependency ->
                when (dependency) {
                    is SwiftPMDependency.Local -> dependency
                    is SwiftPMDependency.Remote -> null
                }
            }
        }
    }

    val syntheticImportProjectGenerationTaskForEmbedAndSignLinkage = regenerateLinkageImportProjectTask(transitiveSwiftPMDependenciesMap)
    syntheticImportProjectGenerationTaskForEmbedAndSignLinkage.configure {
        it.configureWithExtension(swiftPMImportExtension)
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesMap)
        it.syntheticProductType.set(productTypeProvider)
    }

    val projectPathProvider = project.providers.environmentVariable(IntegrateLinkagePackageIntoXcodeProject.PROJECT_PATH_ENV)

    val syntheticImportProjectGenerationTaskForLinkageForCli = locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
        lowerCamelCaseName(
            GenerateSyntheticLinkageImportProject.TASK_NAME,
            "forLinkageForCli",
        ),
    ) {
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesMap)
        it.configureWithExtension(swiftPMImportExtension)
        it.syntheticImportProjectRoot.set(
            projectPathProvider.flatMap {
                project.layout.dir(
                    project.provider { File(it).parentFile.resolve(SYNTHETIC_IMPORT_TARGET_MAGIC_NAME) }
                )
            }
        )
        it.syntheticProductType.set(productTypeProvider)
    }

    project.locateOrRegisterTask<IntegrateLinkagePackageIntoXcodeProject>(IntegrateLinkagePackageIntoXcodeProject.TASK_NAME) {
        it.dependsOn(syntheticImportProjectGenerationTaskForLinkageForCli)
        it.xcodeprojPath.set(projectPathProvider)
    }

    val computeLocalPackageDependencyInputFiles = project.locateOrRegisterTask<ComputeLocalPackageDependencyInputFiles>(
        ComputeLocalPackageDependencyInputFiles.TASK_NAME,
    ) {
        it.localPackages.addAll(
            transitiveLocalSwiftPMDependencies.map {
                it.map { it.path }
            }
        )
    }

    val syntheticImportProjectGenerationTaskForCinteropsAndLdDump = project.locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
        lowerCamelCaseName(
            GenerateSyntheticLinkageImportProject.TASK_NAME,
            "forCinteropsAndLdDump",
        ),
    ) {
        it.configureWithExtension(swiftPMImportExtension)
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesMap)
        /**
         * FIXME: This is not what we actually want. Having dynamic linkage here might erroneously fail def file creation if the linkage
         * type is incompatible with consumed targets. Probably we want to do LD dump in a separate step and only if necessary
         */
        it.syntheticProductType.set(SyntheticProductType.DYNAMIC)
    }

    val hasDirectSwiftPMDependencies = project.provider { swiftPMImportExtension.spmDependencies.isNotEmpty() }
    val hasSwiftPMDependencies = transitiveSwiftPMDependenciesMap.map { transitiveDependencies ->
        hasDirectSwiftPMDependencies.get() || transitiveDependencies.values.any { it.dependencies.isNotEmpty() }
    }
    val fetchSyntheticImportProjectPackages = project.locateOrRegisterTask<FetchSyntheticImportProjectPackages>(
        FetchSyntheticImportProjectPackages.TASK_NAME,
    ) {
        it.onlyIf { hasSwiftPMDependencies.get() }
        it.dependsOn(syntheticImportProjectGenerationTaskForCinteropsAndLdDump)
        it.localPackageManifests.from(
            transitiveLocalSwiftPMDependencies.map {
                it.map {
                    it.path.resolve("Package.swift")
                }
            }
        )
        it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinteropsAndLdDump.map { it.syntheticImportProjectRoot.get() })
    }

    val syntheticImportTasks = listOf(
        syntheticImportProjectGenerationTaskForCinteropsAndLdDump,
        syntheticImportProjectGenerationTaskForLinkageForCli,
        syntheticImportProjectGenerationTaskForEmbedAndSignLinkage,
    )
    syntheticImportTasks.forEach {
        it.configure {
            it.onlyIf {
                hasSwiftPMDependencies.get()
            }
        }
    }

    kotlinExtension.targets.matching {
        val targetSupportsSwiftPMImport = it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily
        targetSupportsSwiftPMImport
    }.all { target ->
        target as KotlinNativeTarget

        syntheticImportTasks.forEach {
            it.configure {
                it.konanTargets.add(target.konanTarget)
            }
        }
        val cinteropName = "swiftPMImport"
        val targetPlatform = target.konanTarget.applePlatform
        // use sdk for a more conventional name
        val targetSdk = target.konanTarget.appleTarget.sdk
        val defFilesAndLdDumpGenerationTask = project.locateOrRegisterTask<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(
            lowerCamelCaseName(
                ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
                targetSdk,
            )
        ) {
            // FIXME: Remove this and fix input/outputs
            it.dependsOn(fetchSyntheticImportProjectPackages)
            it.dependsOn(computeLocalPackageDependencyInputFiles)
            it.resolvedPackagesState.from(
                fetchSyntheticImportProjectPackages.map { it.inputManifests },
                fetchSyntheticImportProjectPackages.map { it.lockFile },
            )
            it.xcodebuildPlatform.set(targetPlatform)
            it.xcodebuildSdk.set(targetSdk)
            it.swiftPMDependenciesCheckout.set(fetchSyntheticImportProjectPackages.map { it.swiftPMDependenciesCheckout.get() })
            it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinteropsAndLdDump.map { it.syntheticImportProjectRoot.get() })
            it.discoverModulesImplicitly.set(swiftPMImportExtension.discoverModulesImplicitly)
            it.filesToTrackFromLocalPackages.set(computeLocalPackageDependencyInputFiles.flatMap { it.filesToTrackFromLocalPackages })
            it.hasSwiftPMDependencies.set(hasSwiftPMDependencies)
        }

        tasks.configureEach { task ->
            if (task.name == target.testTaskName) {
                task as KotlinNativeTest
                val frameworkSearchPathsDump = provider {
                    defFilesAndLdDumpGenerationTask.get().frameworkSearchpathFilePath(target.konanTarget.appleArchitecture)
                }.get()
                val librariesSearchPathsDump = provider {
                    defFilesAndLdDumpGenerationTask.get().librarySearchpathFilePath(target.konanTarget.appleArchitecture)
                }.get()

                task.processOptions.environment.put(
                     if (task is KotlinNativeSimulatorTest) "SIMCTL_CHILD_DYLD_FALLBACK_FRAMEWORK_PATH" else "DYLD_FALLBACK_FRAMEWORK_PATH",
                    syntheticImportProjectGenerationTaskForCinteropsAndLdDump.flatMap {
                        // Fight eager CC provider: fetch dump path eagerly, but read the file only when the task has executed
                        it.outputs.files.elements
                    }.map {
                        frameworkSearchPathsDump.get().asFile.readLines().single()
                            .split(DUMP_FILE_ARGS_SEPARATOR)
                            .joinToString(":")
                    }
                )
                task.processOptions.environment.put(
                    if (task is KotlinNativeSimulatorTest) "SIMCTL_CHILD_DYLD_FALLBACK_LIBRARY_PATH" else "DYLD_FALLBACK_LIBRARY_PATH",
                    syntheticImportProjectGenerationTaskForCinteropsAndLdDump.flatMap {
                        // Fight eager CC provider: fetch dump path eagerly, but read the file only when the task has executed
                        it.outputs.files.elements
                    }.map {
                        librariesSearchPathsDump.get().asFile.readLines().single()
                            .split(DUMP_FILE_ARGS_SEPARATOR)
                            .joinToString(":")
                    }
                )
            }
        }

        target.binaries.all { binary ->
            binary.linkTaskProvider.configure { linkTask ->
                // FIXME: Just do this once instead of in the spmDependencies.all callback
                val ldArgDumpPath = provider {
                    defFilesAndLdDumpGenerationTask.get().ldFilePath(target.konanTarget.appleArchitecture)
                }.get()
                linkTask.dependsOn(defFilesAndLdDumpGenerationTask)
                linkTask.additionalLinkerOptsProperty.set(
                    defFilesAndLdDumpGenerationTask.flatMap {
                        // Fight eager CC provider: fetch dump path eagerly, but read the file only when the task has executed
                        it.outputs.files.elements
                    }.map {
                        val ldDumpFile = ldArgDumpPath.get().asFile
                        ldDumpFile.readLines().single().split(DUMP_FILE_ARGS_SEPARATOR)
                    }
                )
            }
        }

        defFilesAndLdDumpGenerationTask.configure {
            it.architectures.add(target.konanTarget.appleArchitecture)
        }

        swiftPMImportExtension.spmDependencies.all { swiftPMDependency ->
            kotlinPropertiesProvider.enableCInteropCommonizationSetByExternalPlugin = true
            when (swiftPMDependency) {
                is SwiftPMDependency.Local -> {
                    computeLocalPackageDependencyInputFiles.configure {
                        it.localPackages.add(swiftPMDependency.path)
                    }
                    fetchSyntheticImportProjectPackages.configure {
                        it.localPackageManifests.from(
                            swiftPMDependency.path.resolve("Package.swift")
                        )
                    }
                }
                is SwiftPMDependency.Remote -> Unit
            }

            val mainCompilationCinterops = target.compilations.getByName("main").cinterops
            if (cinteropName !in mainCompilationCinterops.names) {
                val defFile = defFilesAndLdDumpGenerationTask.map {
                    it.defFilePath(target.konanTarget.appleArchitecture).get()
                }
                val swiftPMImportCinterop = mainCompilationCinterops.create(cinteropName)
                tasks.configureEach {
                    if (it.name == swiftPMImportCinterop.interopProcessingTaskName) {
                        it.onlyIf {
                            hasSwiftPMDependencies.get()
                        }
                    }
                }
                swiftPMImportCinterop.definitionFile.set(defFile)
            }

            syntheticImportTasks.forEach { it.configure { it.directlyImportedSpmModules.add(swiftPMDependency) } }
            swiftPMDependenciesMetadata.configure { it.importedSpmModules.add(swiftPMDependency) }

            defFilesAndLdDumpGenerationTask.configure {
                val swiftPMPlatform = target.konanTarget.swiftPMPlatform()
                it.clangModules.addAll(
                    swiftPMDependency.cinteropClangModules.filter { dependency ->
                        dependency.platformConstraints?.let { constraints ->
                            swiftPMPlatform in constraints
                        } ?: true
                    }.map {
                        it.name
                    }
                )
            }
        }
    }
}

private fun KonanTarget.swiftPMPlatform(): SwiftPMDependency.Platform = when (this) {
    KonanTarget.IOS_ARM64,
    KonanTarget.IOS_SIMULATOR_ARM64,
    KonanTarget.IOS_X64 -> Platform.iOS
    KonanTarget.MACOS_ARM64,
    KonanTarget.MACOS_X64 -> Platform.macOS
    KonanTarget.TVOS_ARM64,
    KonanTarget.TVOS_SIMULATOR_ARM64,
    KonanTarget.TVOS_X64 -> Platform.tvOS
    KonanTarget.WATCHOS_ARM32,
    KonanTarget.WATCHOS_ARM64,
    KonanTarget.WATCHOS_DEVICE_ARM64,
    KonanTarget.WATCHOS_SIMULATOR_ARM64,
    KonanTarget.WATCHOS_X64 -> Platform.watchOS

    KonanTarget.ANDROID_ARM32,
    KonanTarget.ANDROID_ARM64,
    KonanTarget.ANDROID_X64,
    KonanTarget.ANDROID_X86,
    KonanTarget.LINUX_ARM32_HFP,
    KonanTarget.LINUX_ARM64,
    KonanTarget.LINUX_X64,
    KonanTarget.MINGW_X64 -> error("unsupported targets")
}

/**
 * FIXME: This is incorrect, the linkage package should:
 * - collect dependencies from all the entire classpath
 * - should emit the linkage structure at specific sites, e.g. for embedAndSign, for internal linkage, etc
 */
// FIXME: Rearrange this task so that it only runs after linkage package detection
internal fun Project.regenerateLinkageImportProjectTask(swiftPMDependencies: Provider<Map<String, SwiftPMImport>>): TaskProvider<GenerateSyntheticLinkageImportProject> {
    val hasDirectlyDeclaredSwiftPMDependencies = provider { swiftPMDependenciesExtension().spmDependencies.isNotEmpty() }
    return locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
        lowerCamelCaseName(
            GenerateSyntheticLinkageImportProject.TASK_NAME,
            "forEmbedAndSignLinkage",
        ),
    ).also {
        it.configure {
            it.failOnNonIdempotentChanges.set(true)
            it.onlyIf {
                hasDirectlyDeclaredSwiftPMDependencies.get() || swiftPMDependencies.get().values.any { it.dependencies.isNotEmpty() }
            }
        }
    }
}

@DisableCachingByDefault(because = "...")
internal abstract class GenerateSyntheticLinkageImportProject : DefaultTask() {

    @get:Input
    abstract val directlyImportedSpmModules: SetProperty<SwiftPMDependency>

    @get:Input
    abstract val dependencyIdentifierToImportedSwiftPMDependencies: MapProperty<String, SwiftPMImport>

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImport")
    )

    @get:OutputFiles
    protected val projectRootTrackedFiles get() = syntheticImportProjectRoot.asFileTree.matching {
        // FIXME: SwiftPM always generates Package.resolved adjacent to the root Package.swift or in the xcodeproj...
        it.exclude("Package.resolved")
    }

    @get:Input
    abstract val konanTargets: SetProperty<KonanTarget>

    @get:Optional
    @get:Input
    abstract val iosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val macosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val watchosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val tvosDeploymentVersion: Property<String>

    @get:Input
    abstract val syntheticProductType: Property<SyntheticProductType>

    @get:Input
    val failOnNonIdempotentChanges: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    enum class SyntheticProductType : Serializable {
        DYNAMIC,
        INFERRED,
    }

    @get:Inject
    protected abstract val execOps: ExecOperations

    fun configureWithExtension(swiftPMImportExtension: SwiftImportExtension) {
        iosDeploymentVersion.set(swiftPMImportExtension.iosDeploymentVersion)
        macosDeploymentVersion.set(swiftPMImportExtension.macosDeploymentVersion)
        watchosDeploymentVersion.set(swiftPMImportExtension.watchosDeploymentVersion)
        tvosDeploymentVersion.set(swiftPMImportExtension.tvosDeploymentVersion)
    }

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        val packageRoot = syntheticImportProjectRoot.get().asFile
        val initialDigest = if (failOnNonIdempotentChanges.get()) {
            val sha = MessageDigest.getInstance("SHA-256")
            projectRootTrackedFiles.files.sorted().forEach {
                sha.update(it.readBytes())
            }
            sha.digest()
        } else null

        val linkerHack = when (syntheticProductType.get()) {
            SyntheticProductType.DYNAMIC -> packageRoot.resolve("linkerHack").also {
                it.writeText(linkerScriptHack())
                it.setExecutable(true)
            }
            SyntheticProductType.INFERRED -> null
        }
        generatePackageManifest(
            identifier = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
            packageRoot = packageRoot,
            syntheticProductType = syntheticProductType.get(),
            directlyImportedSwiftPMDependencies = directlyImportedSpmModules.get(),
            localSyntheticPackages = dependencyIdentifierToImportedSwiftPMDependencies.get().keys,
            linkerHackPath = linkerHack,
        )
        dependencyIdentifierToImportedSwiftPMDependencies.get().forEach { (dependencyIdentifier, swiftPMDependencies) ->
            generatePackageManifest(
                identifier = dependencyIdentifier,
                packageRoot = packageRoot.resolve("${SUBPACKAGES}/${dependencyIdentifier}"),
                /**
                 * FIXME: We probably always want inferred here, but figure out what is wrong with SwiftPM's linkage when 2 .dynamic products are involved
                 *
                 * Also all the project/modular dependencies will litter embedAndSign integration with useless dylibs
                 */
                syntheticProductType = SyntheticProductType.INFERRED,
                directlyImportedSwiftPMDependencies = swiftPMDependencies.dependencies,
                localSyntheticPackages = setOf(),
            )
        }

        if (initialDigest != null) {
            val sha = MessageDigest.getInstance("SHA-256")
            projectRootTrackedFiles.files.sorted().forEach {
                sha.update(it.readBytes())
            }
            val finalDigest = sha.digest()
            if (!initialDigest.contentEquals(finalDigest)) {
                println("error: Synthetic project regenerated")
                println("error: Please go to File -> Package -> Resolve Package Versions")
                error("Synthetic project state updated")
            }
        }
    }

    private fun generatePackageManifest(
        identifier: String,
        packageRoot: File,
        syntheticProductType: SyntheticProductType,
        directlyImportedSwiftPMDependencies: Set<SwiftPMDependency>,
        // FIXME: Implicitly, this is the directory, package and product name
        localSyntheticPackages: Set<String>,
        linkerHackPath: File? = null,
    ) {
        val repoDependencies = (directlyImportedSwiftPMDependencies.map { importedPackage ->
            buildString {
                appendLine(".package(")
                when (importedPackage) {
                    is SwiftPMDependency.Remote -> {
                        when (val repository = importedPackage.repository) {
                            is SwiftPMDependency.Remote.Repository.Id -> {
                                appendLine("  id: \"${repository.value}\",")
                            }
                            is SwiftPMDependency.Remote.Repository.Url -> {
                                appendLine("  url: \"${repository.value}\",")
                            }
                        }
                        when (val version = importedPackage.version) {
                            is SwiftPMDependency.Remote.Version.Exact -> appendLine("  exact: \"${version.value}\",")
                            is SwiftPMDependency.Remote.Version.From -> appendLine("  from: \"${version.value}\",")
                            // FIXME: Range specification needs more thought
                            is SwiftPMDependency.Remote.Version.Range -> appendLine("  \"${version.from}\"...\"${version.through}\",")
                            is SwiftPMDependency.Remote.Version.Branch -> appendLine("  branch: \"${version.value}\",")
                            is SwiftPMDependency.Remote.Version.Revision -> appendLine("  revision: \"${version.value}\",")
                        }
                    }
                    is SwiftPMDependency.Local -> {
                        appendLine("  path: \"${importedPackage.path.path}\",")
                    }
                }
                if (importedPackage.traits.isNotEmpty()) {
                    val traitsString = importedPackage.traits.joinToString(", ") { "\"${it}\"" }
                    appendLine("  traits: [${traitsString}],")
                }
                appendLine("),")
            }
        } + localSyntheticPackages.map {
            ".package(path: \"${SUBPACKAGES}/${it}\"),"
        })
        val targetDependencies = (directlyImportedSwiftPMDependencies.flatMap { dep -> dep.products.map { it to dep.packageName } }.map {
            buildString {
                appendLine(".product(")
                appendLine("  name: \"${it.first.name}\",")
                appendLine("  package: \"${it.second}\",")
                val platformConstraints = it.first.platformConstraints
                if (platformConstraints != null) {
                    val platformsString = platformConstraints.joinToString(", ") { ".${it.swiftEnumName}" }
                    appendLine("  condition: .when(platforms: [${platformsString}]),")
                }
                appendLine("),")
            }
        } + localSyntheticPackages.map {
            ".product(name: \"${it}\", package: \"${it}\"),"
        })

        val platforms = konanTargets.get().map { it.family }.toSet().map {
            when (it) {
                Family.OSX -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        macosDeploymentVersion,
                        MACOS_DEPLOYMENT_VERSION_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().values.mapNotNull { it.macosDeploymentVersion },
                    )
                    ".macOS(\"${deploymentTarget}\"),"
                }
                Family.IOS -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        iosDeploymentVersion,
                        IOS_DEPLOYMENT_VERSION_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().values.mapNotNull { it.iosDeploymentVersion },
                    )
                    ".iOS(\"${deploymentTarget}\"),"
                }
                Family.TVOS -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        tvosDeploymentVersion,
                        TVOS_DEPLOYMENT_VERSION_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().values.mapNotNull { it.tvosDeploymentVersion },
                    )
                    ".tvOS(\"${deploymentTarget}\"),"
                }
                Family.WATCHOS -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        watchosDeploymentVersion,
                        WATCHOS_DEPLOYMENT_VERSION_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().values.mapNotNull { it.watchosDeploymentVersion },
                    )
                    ".watchOS(\"${deploymentTarget}\"),"
                }
                Family.LINUX,
                Family.MINGW,
                Family.ANDROID
                    -> error("???")
            }
        }

        val productType = when (syntheticProductType) {
            SyntheticProductType.DYNAMIC -> ".dynamic"
            SyntheticProductType.INFERRED -> ".none"
        }

        val manifest = packageRoot.resolve(MANIFEST_NAME)
        manifest.also {
            it.parentFile.mkdirs()
        }.writeText(
            buildString {
                appendLine("// swift-tools-version: 5.9")
                appendLine("import PackageDescription")
                appendLine("let package = Package(")
                appendLine("  name: \"$identifier\",")
                appendLine("  platforms: [")
                platforms.forEach { appendLine("    $it")}
                appendLine("  ],")
                appendLine(
                    """
                        products: [
                            .library(
                                name: "$identifier",
                                type: ${productType},
                                targets: ["$identifier"]
                            ),
                        ],
                    """.replaceIndent("  ")
                )
                appendLine("  dependencies: [")
                repoDependencies.forEach { appendLine(it.replaceIndent("    ")) }
                appendLine("  ],")
                appendLine("  targets: [")
                appendLine("    .target(")
                appendLine("      name: \"$identifier\",")
                appendLine("      dependencies: [")
                targetDependencies.forEach { appendLine(it.replaceIndent("        ")) }
                appendLine("      ]")
                if (linkerHackPath != null) {
                    appendLine("      , linkerSettings: [.unsafeFlags([\"-fuse-ld=${linkerHackPath.path}\"])]")
                }
                appendLine("    ),")
                appendLine("  ]")
                appendLine(")")
            }
        )

        val objcSource = "Sources/${identifier}/${identifier}.m"
        val objcHeader = "Sources/${identifier}/include/${identifier}.h"
        // Generate ObjC sources specifically because the next CC-overriding step relies on passing a clang shim to dump compiler arguments
        packageRoot.resolve(objcSource).also {
            it.parentFile.mkdirs()
        }.writeText("")
        packageRoot.resolve(objcHeader).also {
            it.parentFile.mkdirs()
        }.writeText("")
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun explicitOrMaximumDeploymentTarget(
        deploymentVersionProperty: Provider<String>,
        deploymentVersionDefault: String,
        transitivelyImportedDeploymentVersions: List<String>,
    ): String {
        val explicitlySpecifiedDeploymentVersion = deploymentVersionProperty.orNull
        if (explicitlySpecifiedDeploymentVersion != null) {
            return explicitlySpecifiedDeploymentVersion
        }
        val maximumDeploymentTarget = transitivelyImportedDeploymentVersions.fold(
            SemVer.from(deploymentVersionDefault, loose = true),
        ) { max, current ->
            val other = SemVer.from(current, loose = true)
            if (max >= other) {
                max
            } else {
                other
            }
        }
        return "${maximumDeploymentTarget.major}.${maximumDeploymentTarget.minor}"
    }

    companion object {
        const val TASK_NAME = "generateSyntheticLinkageSwiftPMImportProject"
        const val SYNTHETIC_IMPORT_TARGET_MAGIC_NAME = "_internal_linkage_SwiftPMImport"
        const val SUBPACKAGES = "subpackages"
        const val MANIFEST_NAME = "Package.swift"

        // FIXME: Maybe tests against CI RECOMMENDED_ version to keep up to date?
        const val IOS_DEPLOYMENT_VERSION_DEFAULT = "15.0"
        const val MACOS_DEPLOYMENT_VERSION_DEFAULT = "10.15"
        const val WATCHOS_DEPLOYMENT_VERSION_DEFAULT = "15.0"
        const val TVOS_DEPLOYMENT_VERSION_DEFAULT = "9.0"
    }
}

@DisableCachingByDefault(because = "...")
internal abstract class ComputeLocalPackageDependencyInputFiles : DefaultTask() {

    @get:Input
    val localPackages: SetProperty<File> = project.objects.setProperty(File::class.java)

    /**
     * Recompute if the manifests change
     */
    @get:InputFiles
    protected val manifests get() = localPackages.map { it.map { it.resolve("Package.swift") } }

    @get:OutputFile
    val filesToTrackFromLocalPackages: RegularFileProperty = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("kotlin/swiftImportFilesToTrackFromLocalPackages")
    )

    @get:Inject
    protected abstract val execOps: ExecOperations

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        // FIXME: Transitive local packages...
        val localPackageFiles = localPackages.get().flatMap { packageRoot ->
            listOf(
                packageRoot.resolve("Package.swift")
            ) + findLocalPackageSources(packageRoot)
        }.map {
            it.path
        }
        filesToTrackFromLocalPackages.getFile().writeText(
            localPackageFiles.joinToString("\n")
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun findLocalPackageSources(path: File): List<File> {
        val jsonBuffer = ByteArrayOutputStream()
        execOps.exec {
            it.workingDir(path)
            it.standardOutput = jsonBuffer
            it.commandLine("swift", "package", "describe", "--type", "json")
        }
        val packageJson = Gson().fromJson(
            jsonBuffer.toString(), Map::class.java
        ) as Map<String, Any>
        val targets = packageJson["targets"] as List<Map<String, Any>>
        val relativeSourceRootPaths = targets
            .filter {
                val moduleType = it["module_type"]
                moduleType == "SwiftTarget" || moduleType == "ClangTarget"
            }
            .map {
                it["path"] as String
            }
        return relativeSourceRootPaths.map {
            path.resolve(it)
        }
    }

    companion object {
        const val TASK_NAME = "computeLocalPackageDependencyInputFiles"
    }
}

@DisableCachingByDefault(because = "...")
internal abstract class FetchSyntheticImportProjectPackages : DefaultTask() {

    /**
     * Refetch when Package manifests of local SwiftPM dependencies change
     */
    @get:InputFiles
    abstract val localPackageManifests: ConfigurableFileCollection

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty()

    /**
     * These are own manifest and manifests from project/modular dependencies. Refetch when any of these Package manifests changed.
     */
    // For some reason FileTree still invalidates on random directories without this annotation even though directories are not tracked...
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    val inputManifests
        get() = syntheticImportProjectRoot
            .asFileTree
            .matching {
                it.include("**/Package.swift")
            }

    // FIXME: Actually think about: "what do we want as a UTD check for the the packages checkout? The lock file?"
    // FIXME: We probably want this cache to be global
    @get:Internal
    val swiftPMDependenciesCheckout: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftPMCheckout")
    )

    /**
     * Invalidate fetch when Package.swift or Package.resolved files changed.
     */
    @get:OutputFile
    val lockFile = syntheticImportProjectRoot.file("Package.resolved")

    @get:Internal
    protected val swiftPMDependenciesCheckoutLogs: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftPMCheckoutDD")
    )

    @get:Inject
    protected abstract val execOps: ExecOperations

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        checkoutSwiftPMDependencies()
    }

    private fun checkoutSwiftPMDependencies() {
        execOps.exec {
            it.workingDir(syntheticImportProjectRoot.get().asFile)
            it.commandLine(
                "xcodebuild", "-resolvePackageDependencies",
                "-scheme", SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER, swiftPMDependenciesCheckout.get().asFile.path,
                "-derivedDataPath", swiftPMDependenciesCheckoutLogs.get().asFile.path,
            )
        }
    }

    companion object {
        const val TASK_NAME = "fetchSyntheticImportProjectPackages"
    }
}

@DisableCachingByDefault(because = "...")
internal abstract class ConvertSyntheticSwiftPMImportProjectIntoDefFile : DefaultTask() {

    @get:Input
    abstract val xcodebuildPlatform: Property<String>
    @get:Input
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    abstract val clangModules: SetProperty<String>

    @get:Input
    abstract val discoverModulesImplicitly: Property<Boolean>

    @get:Input
    abstract val hasSwiftPMDependencies: Property<Boolean>

    @get:InputFile
    abstract val filesToTrackFromLocalPackages: RegularFileProperty
    @get:InputFiles
    protected val localPackageSources get() = filesToTrackFromLocalPackages.map { it.asFile.readLines().filter { it.isNotEmpty() }.map { File(it) } }

    @get:InputFiles
    abstract val resolvedPackagesState: ConfigurableFileCollection

    @get:OutputDirectory
    protected val defFiles = xcodebuildSdk.flatMap { sdk ->
        project.layout.buildDirectory.dir("kotlin/swiftImportDefs/${sdk}")
    }

    @get:OutputDirectory
    protected val ldDump = xcodebuildSdk.flatMap { sdk ->
        project.layout.buildDirectory.dir("kotlin/swiftImportLdDump/${sdk}")
    }

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Internal
    val syntheticImportDd = project.layout.buildDirectory.dir("kotlin/swiftImportDd")

    @get:Inject
    protected abstract val execOps: ExecOperations

    @get:Inject
    protected abstract val objects: ObjectFactory

    private val layout = project.layout

    private val cinteropNamespace = listOf(
        "swiftPMImport",
        project.group.toString(),
        if (project.path == ":") project.name else project.path.drop(1)
    ).filter {
        !it.isEmpty()
    }.joinToString(".") {
        it.replace(Regex("[^a-zA-Z0-9_.]"), ".")
    }

    @TaskAction
    fun generateDefFiles() {
        if (!hasSwiftPMDependencies.get()) {
            architectures.get().forEach { architecture ->
                /**
                 * Stub out all these to ensure correctness on incremental runs.
                 *
                 * FIXME: Find a proper way to avoid doing this
                 */
                defFilePath(architecture).getFile().writeText(
                    """
                        language = Objective-C
                        package = $cinteropNamespace
                    """.trimIndent()
                )
                ldFilePath(architecture).getFile().writeText("\n")
                frameworkSearchpathFilePath(architecture).getFile().writeText("\n")
                librarySearchpathFilePath(architecture).getFile().writeText("\n")
            }
            return
        }

        val dumpIntermediates = xcodebuildSdk.flatMap { sdk ->
            layout.buildDirectory.dir("kotlin/swiftImportClangDump/${sdk}")
        }.get().asFile.also {
            if (it.exists()) {
                it.deleteRecursively()
            }
            it.mkdirs()
        }

        val clangArgsDumpScript = dumpIntermediates.resolve("clangDump.sh")
        clangArgsDumpScript.writeText(clangArgsDumpScript())
        clangArgsDumpScript.setExecutable(true)
        val clangArgsDump = dumpIntermediates.resolve("clang_args_dump")
        clangArgsDump.mkdirs()

        val ldArgsDumpScript = dumpIntermediates.resolve("ldDump.sh")
        ldArgsDumpScript.writeText(ldArgsDumpScript())
        ldArgsDumpScript.setExecutable(true)
        val ldArgsDump = dumpIntermediates.resolve("ld_args_dump")
        ldArgsDump.mkdirs()

        val targetArchitectures = architectures.get().map {
            it.xcodebuildArch
        }

        val projectRoot = syntheticImportProjectRoot.get().asFile
        // FIXME: For some reason reusing dd in parallel xcodebuild calls explodes something in Xcode
        val dd = syntheticImportDd.get().asFile.resolve("dd_${xcodebuildSdk.get()}")

        if (discoverModulesImplicitly.get()) {
            val intermediates = dd.resolve("Build/Intermediates.noindex")
            // Nuke all intermediates to discover c++ modules...
            if (intermediates.exists()) {
                intermediates.listFiles()
                    .filter { it.name.endsWith(".build") }
                    .forEach {
                        it.deleteRecursively()
                    }
            }
            val products = dd.resolve("Build/Products")
            // Also nuke products to avoid discovering stale modulemaps in the products directory
            if (products.exists()) {
                products.deleteRecursively()
            }
            // FIXME: Why not just "clean"?
        } else {
            val forceClangToReexecute = dd.resolve("Build/Intermediates.noindex/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME.build")
            if (forceClangToReexecute.exists()) {
                forceClangToReexecute.deleteRecursively()
            }
        }

        execOps.exec { exec ->
            exec.workingDir(projectRoot)
            exec.commandLine(
                "xcodebuild", "build",
                "-scheme", SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                "-destination", "generic/platform=${xcodebuildPlatform.get()}",
                "-derivedDataPath", dd.path,
                // "-disableAutomaticPackageResolution", FIXME: Probably?
                XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER, swiftPMDependenciesCheckout.get().asFile.path,
                "CC=${clangArgsDumpScript.path}",
                "LD=${ldArgsDumpScript.path}",
                "ARCHS=${targetArchitectures.joinToString(" ")}",
                // FIXME: Check how truly necessary this is
                "CODE_SIGN_IDENTITY=",
                // FIXME: Measure how much this impacts perf
                "COMPILER_INDEX_STORE_ENABLE=NO",
                "SWIFT_INDEX_STORE_ENABLE=NO",
                // FIXME: This will force the .dylib to be created instead of the framework. We actually want to account for this?
                // "-IDEPackageSupportCreateDylibsForDynamicProducts=YES"
            )
            exec.environment(KOTLIN_CLANG_ARGS_DUMP_FILE_ENV, clangArgsDump)
            exec.environment(KOTLIN_LD_ARGS_DUMP_FILE_ENV, ldArgsDump)

            val environmentToFilter = listOf(
                "EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES",
            ) + AppleSdk.xcodeEnvironmentDebugDylibVars
            environmentToFilter.forEach {
                if (exec.environment.containsKey(it)) {
                    exec.environment.remove(it)
                }
            }
            exec.environment.keys.filter {
                // ScanDependencies explode with duplicate modules because it reads this env for some reason
                it.startsWith("OTHER_")
                        // Also some asset catalogs utility explodes
                        || it.startsWith("ASSETCATALOG_")
            }.forEach {
                exec.environment.remove(it)
            }
        }

        architectures.get().forEach { architecture ->
            val clangArchitecture = architecture.clangArch
            val architectureSpecificProductClangCalls = mutableListOf<File>()
            val cxxModules = mutableSetOf<String>()

            clangArgsDump.listFiles().filter {
                it.isFile
            }.forEach {
                val clangArgs = it.readLines().single()
                val isArchitectureSpecificProductClangCall = "-fmodule-name=${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}" in clangArgs
                        && "-target${DUMP_FILE_ARGS_SEPARATOR}${clangArchitecture}-apple" in clangArgs
                if (isArchitectureSpecificProductClangCall) {
                    architectureSpecificProductClangCalls.add(it)
                }
                val isCxxModule = discoverModulesImplicitly.get() && "-x${DUMP_FILE_ARGS_SEPARATOR}c++" in clangArgs
                if (isCxxModule) {
                    val moduleNameArg = "-fmodule-name="
                    val moduleName = clangArgs.split(DUMP_FILE_ARGS_SEPARATOR).firstOrNull {
                        it.startsWith(moduleNameArg)
                    }
                    // Some -x;c++ calls are version discoveries or something like that
                    if (moduleName != null) {
                        cxxModules.add(moduleName.substring(moduleNameArg.length))
                    }
                }
            }
            val architectureSpecificProductClangCall = architectureSpecificProductClangCalls.single()
            val cinteropClangArgs = mutableListOf<String>()

            val compileTimeFrameworkSearchPaths = mutableSetOf<String>()
            val includeSearchPaths = mutableSetOf<String>()
            val explicitModuleMaps = mutableSetOf<String>()

            architectureSpecificProductClangCall.readLines().single().split(DUMP_FILE_ARGS_SEPARATOR).forEach { arg ->
                if (arg.startsWith("-F")) {
                    cinteropClangArgs.add(arg)
                    compileTimeFrameworkSearchPaths.add(arg.substring(2))
                }
                if (arg.startsWith("-I")) {
                    cinteropClangArgs.add(arg)
                    includeSearchPaths.add(arg.substring(2))
                }
                if (arg.startsWith("-fmodule-map-file=")) {
                    cinteropClangArgs.add(arg)
                    explicitModuleMaps.add(arg.substring("-fmodule-map-file=".length))
                }
            }

            val moduleName = Regex("\\bmodule ([A-Za-z0-9_.]+) ")
            fun inferModuleName(modulemap: File): String? = moduleName.find(modulemap.readText())?.let {
                it.groups[1]?.value
            }

            val clangModules = if (discoverModulesImplicitly.get()) {
                /**
                 * FIXME: This will discovery logic will break on incremental runs as it will discover stale modules (same issue with Xcode)
                 */
                val implicitlyDiscoveredModules = mutableSetOf<String>()
                compileTimeFrameworkSearchPaths.map { File(it) }.filter { it.exists() }.forEach {
                    implicitlyDiscoveredModules.addAll(
                        it.listFiles().filter {
                            it.extension == "framework"
                        }.filter { framework ->
                            val hasModules = framework.listFiles().any { it.name == "Modules" }
                            // FIXME: Google...
                            val hasHeaders = framework.listFiles().any { it.name == "Headers" }
                            hasModules && hasHeaders
                        }.map { framework ->
                            framework.nameWithoutExtension
                        }
                    )
                }
                includeSearchPaths.map { File(it) }.filter { it.exists() }.forEach { searchPath ->
                    searchPath.listFiles().forEach { searchPathFile ->
                        if (searchPathFile.name == "module.modulemap") {
                            val module = inferModuleName(searchPathFile)
                            if (module != null) {
                                implicitlyDiscoveredModules.add(module)
                            }
                        }
                        // Also discover modules in the form
                        // -I/search/path
                        // /search/path/ModuleName/module.modulemap
                        // E.g. GoogleMaps
                        if (searchPathFile.isDirectory) {
                            searchPathFile.listFiles().filter {
                                it.name == "module.modulemap"
                            }.forEach { subsearchPathFile ->
                                val module = inferModuleName(subsearchPathFile)
                                // The module must be eqiual to the directory name, same as with frameworks
                                if (module != null && module == searchPathFile.name) {
                                    implicitlyDiscoveredModules.add(module)
                                }
                            }
                        }
                    }
                }
                implicitlyDiscoveredModules.addAll(
                    explicitModuleMaps.mapNotNull {
                        inferModuleName(File(it))
                    }
                )
                implicitlyDiscoveredModules - cxxModules
            } else clangModules.get().map { it }

            val defFileSearchPaths = cinteropClangArgs.joinToString(" ") { "\"${it}\"" }
            val modules = clangModules.joinToString(" ") { "\"${it}\"" }

            val workaroundKT81695 = "-DSWIFT_TYPEDEFS"
            val defFilePath = defFilePath(architecture)
            defFilePath.getFile().writeText(
                buildString {
                    appendLine("language = Objective-C")
                    appendLine("compilerOpts = $workaroundKT81695 -fmodules $defFileSearchPaths")
                    appendLine("package = $cinteropNamespace")
                    if (modules.isNotEmpty()) {
                        appendLine("modules = $modules")
                    }
                    val invalidateDownstreamCinterops = UUID.randomUUID().toString()
                    appendLine("""
                        ---
                        // $invalidateDownstreamCinterops
                    """.trimIndent())
                }
            )

            val architectureSpecificProductLdCalls = ldArgsDump.listFiles().filter {
                it.isFile
            }.filter {
                // This will actually be a clang call
                val ldArgs = it.readLines().single()
                ("@rpath/lib${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.dylib" in ldArgs || "@rpath/${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.framework" in ldArgs)
                        && "-target${DUMP_FILE_ARGS_SEPARATOR}${clangArchitecture}-apple" in ldArgs
            }
            val architectureSpecificProductLdCall = architectureSpecificProductLdCalls.single()
            val ldArgs = mutableListOf<String>()
            val resplitLdCall = architectureSpecificProductLdCall.readLines().single().split(DUMP_FILE_ARGS_SEPARATOR)
            val linkTimeFrameworkSearchPaths = mutableSetOf<String>()
            val librarySearchPaths = mutableSetOf<String>()

            resplitLdCall.forEachIndexed { index, arg ->
                if (arg == "-filelist" || arg == "-framework" || (arg.startsWith("-") && arg.endsWith("_framework"))) {
                    ldArgs.addAll(listOf(arg, resplitLdCall[index + 1]))
                }
                // FIXME: match all the other flavors of library linkage
                if (arg.startsWith("-l")) {
                    ldArgs.add(arg)
                }
                // FIXME: This is not accurate but whatever
                if (arg.startsWith("-F/")) {
                    ldArgs.add(arg)
                    linkTimeFrameworkSearchPaths.add(arg.substring(2))
                }
                if (arg.startsWith("-L/")) {
                    ldArgs.add(arg)
                    librarySearchPaths.add(arg.substring(2))
                }
                // FIXME: This is the branch that is necessary to link against other targets. Do this properly
                if (arg.startsWith("/")) {
                    if (arg.endsWith(".a")) {
                        ldArgs.add(arg)
                    }
                    if (arg.endsWith(".dylib")) {
                        ldArgs.add(arg)
                        librarySearchPaths.add((File(arg).parentFile.path))
                    }
                    if (".framework/" in arg) {
                        ldArgs.add(arg)
                        linkTimeFrameworkSearchPaths.add(
                            // FIXME: this is different for macOS
                            File(arg).parentFile.parentFile.path
                        )
                    }
                }
            }

            ldFilePath(architecture).getFile()
                .writeText(ldArgs.joinToString(DUMP_FILE_ARGS_SEPARATOR))
            frameworkSearchpathFilePath(architecture).getFile()
                .writeText(linkTimeFrameworkSearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR))
            librarySearchpathFilePath(architecture).getFile()
                .writeText(librarySearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR))
        }
    }

    fun defFilePath(architecture: AppleArchitecture) = defFiles.map { it.file("${architecture.xcodebuildArch}.def") }
    fun ldFilePath(architecture: AppleArchitecture) = ldDump.map { it.file("${architecture.xcodebuildArch}.ld") }
    fun frameworkSearchpathFilePath(architecture: AppleArchitecture) = ldDump.map { it.file("${architecture.xcodebuildArch}_framework_search_paths") }
    fun librarySearchpathFilePath(architecture: AppleArchitecture) = ldDump.map { it.file("${architecture.xcodebuildArch}_library_search_paths") }

    private fun clangArgsDumpScript() = argsDumpScript("clang", KOTLIN_CLANG_ARGS_DUMP_FILE_ENV)
    private fun ldArgsDumpScript() = argsDumpScript("clang", KOTLIN_LD_ARGS_DUMP_FILE_ENV)

    private fun argsDumpScript(
        targetCli: String,
        dumpPathEnv: String,
    ) = """
        #!/bin/bash

        DUMP_FILE="${'$'}{${dumpPathEnv}}/${'$'}(/usr/bin/uuidgen)"
        for arg in "$@"
        do
           echo -n "${'$'}arg" >> "${'$'}{DUMP_FILE}"
           echo -n "$DUMP_FILE_ARGS_SEPARATOR" >> "${'$'}{DUMP_FILE}"
        done

        ${targetCli} "$@"
    """.trimIndent()

    companion object {
        const val KOTLIN_CLANG_ARGS_DUMP_FILE_ENV = "KOTLIN_CLANG_ARGS_DUMP_FILE"
        const val KOTLIN_LD_ARGS_DUMP_FILE_ENV = "KOTLIN_LD_ARGS_DUMP_FILE"
        const val DUMP_FILE_ARGS_SEPARATOR = ";"
        const val TASK_NAME = "convertSyntheticImportProjectIntoDefFile"
    }

}

@DisableCachingByDefault(because = "...")
internal abstract class IntegrateLinkagePackageIntoXcodeProject : DefaultTask() {

    @get:Input
    abstract val xcodeprojPath: Property<String>

    @get:Inject
    protected abstract val execOps: ExecOperations

    @Suppress("UNCHECKED_CAST")
    @TaskAction
    fun integrate() {
        val projectPath = File(xcodeprojPath.get())
        val pbxprojPath = projectPath.resolve("project.pbxproj")
        val output = ByteArrayOutputStream()
        execOps.exec {
            it.standardOutput = output
            it.commandLine(
                "/usr/bin/plutil",
                "-convert", "json",
                pbxprojPath,
                "-o", "-"
            )
        }

        val projectJson = Gson().fromJson(
            output.toString(), Map::class.java
        ) as Map<String, Any>
        if (isLinkageProductReferencedInPBXObjects(projectJson)) {
            println("Product already referenced, nothing to do")
            return
        }

        val rootProjectId = projectJson.property<String>("rootObject")

        val objects = projectJson.property<Map<String, Any>>("objects").toMutableMap()
        val rootProject = objects[rootProjectId] ?: error("Couldn't find root project")

        val embedAndSignShellScriptPhaseReference = objects.entries.firstOrNull { (_, pbxObject) ->
            if (pbxObject is Map<*, *>) {
                pbxObject as Map<String, Any>
                val shellContent = pbxObject["shellScript"]
                if (shellContent is String) {
                    "gradle" in shellContent
                } else if (shellContent is List<*>) {
                    shellContent as List<String>
                    shellContent.any {
                        "gradle" in it
                    }
                } else false
            } else false
        }?.key ?: error("embedAndSign integration wasn't found")

        val embedAndSignTargets = objects.entries.filter { (_, pbxObject) ->
            if (pbxObject is Map<*, *>) {
                pbxObject as Map<String, Any>
                val phases = pbxObject["buildPhases"]
                if (phases is List<*>) {
                    phases as List<String>
                    embedAndSignShellScriptPhaseReference in phases
                } else false
            } else false
        }

        val productDependencyReference = generateRandomPBXObjectReference()
        objects[productDependencyReference] = mapOf(
            "isa" to "XCSwiftPackageProductDependency",
            "productName" to SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
        )

        val buildFileDependencyReference = generateRandomPBXObjectReference()
        objects[buildFileDependencyReference] = mapOf(
            "isa" to "PBXBuildFile",
            "productRef" to productDependencyReference,
        )

        val localPackageReference = generateRandomPBXObjectReference()
        objects[localPackageReference] = mapOf(
            "isa" to "XCLocalSwiftPackageReference",
            "relativePath" to SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
        )


        val updatedProject = (rootProject as Map<String, Any>).toMutableMap()
        val existingPackages = updatedProject["packageReferences"] as? List<String> ?: listOf()
        updatedProject["packageReferences"] = existingPackages + localPackageReference
        objects[rootProjectId] = updatedProject

        embedAndSignTargets.forEach { (uuid, target) ->
            val updatedTarget = (target as Map<String, Any>).toMutableMap()
            val existingPackageProductDependencies = updatedProject["packageProductDependencies"] as? List<String> ?: listOf()
            updatedTarget["packageProductDependencies"] = existingPackageProductDependencies + productDependencyReference
            objects[uuid] = updatedTarget
        }

        embedAndSignTargets.mapNotNull { (_, target) ->
            target as Map<String, Any>
            target.property<List<String>>("buildPhases")
        }.flatten().forEach { buildPhaseReference ->
            val buildPhase = objects.property<Map<String, Any>>(buildPhaseReference)
            if (buildPhase.property<String>("isa") == "PBXFrameworksBuildPhase") {
                val updatedBuildPhase = buildPhase.toMutableMap()
                val existingFiles = updatedBuildPhase["files"] as? List<String> ?: listOf()
                updatedBuildPhase["files"] = existingFiles + buildFileDependencyReference
                objects[buildPhaseReference] = updatedBuildPhase
            }
        }

        val updatedProjectJson = projectJson.toMutableMap()
        updatedProjectJson["objects"] = objects

        val resultingJson = Gson().toJson(updatedProjectJson)
        pbxprojPath.writeText(resultingJson)
    }

    private fun generateRandomPBXObjectReference(): String {
        val messageDigest = MessageDigest.getInstance("MD5")
        return messageDigest.digest(
            UUID.randomUUID().toString().toByteArray()
        ).joinToString(separator = "") { byte -> "%02x".format(byte) }.uppercase().subSequence(0, 24).toString()
    }

    companion object {
        const val PROJECT_PATH_ENV = "XCODEPROJ_PATH"
        const val TASK_NAME = "integrateLinkagePackage"
    }
}

internal abstract class SerializeSwiftPMDependenciesMetadata : DefaultTask() {

    @get:Input
    abstract val importedSpmModules: SetProperty<SwiftPMDependency>

    @get:Optional
    @get:Input
    abstract val iosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val macosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val watchosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val tvosDeploymentVersion: Property<String>

    @get:OutputFile
    val serializationFile: Provider<RegularFile> = project.layout.buildDirectory.file("kotlin/importedSpmModules")

    fun configureWithExtension(swiftPMImportExtension: SwiftImportExtension) {
        iosDeploymentVersion.set(swiftPMImportExtension.iosDeploymentVersion)
        macosDeploymentVersion.set(swiftPMImportExtension.macosDeploymentVersion)
        watchosDeploymentVersion.set(swiftPMImportExtension.watchosDeploymentVersion)
        tvosDeploymentVersion.set(swiftPMImportExtension.tvosDeploymentVersion)
    }

    @TaskAction
    fun serialize() {
        val spmDependencies = importedSpmModules.get()
            // get rid of Google set
            .map { it }.toSet()
        serializationFile.get().asFile.outputStream().use { file ->
            ObjectOutputStream(file).use { objects ->
                objects.writeObject(
                    SwiftPMImport(
                        iosDeploymentVersion = iosDeploymentVersion.orNull,
                        macosDeploymentVersion = macosDeploymentVersion.orNull,
                        watchosDeploymentVersion = watchosDeploymentVersion.orNull,
                        tvosDeploymentVersion = tvosDeploymentVersion.orNull,
                        dependencies = spmDependencies,
                    )
                )
            }
        }
    }

    companion object {
        const val TASK_NAME = "serializeSwiftPMDependenciesMetadata"
    }

}

fun isLinkageProductReferencedInPBXObjects(projectJson: Map<String, Any>): Boolean {
    val objects = projectJson.property<Map<String, Any>>("objects")
    // FIXME: Check if the product is correctly integrated into the build phase
    val hasSyntheticImportProjectReference = objects.values.any { pbxObject ->
        @Suppress("UNCHECKED_CAST")
        pbxObject as Map<String, Any>
        val type = pbxObject.property<String>("isa")
        if (type == "XCSwiftPackageProductDependency") {
            val packageProductName = pbxObject.property<String>("productName")
            packageProductName == SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
        } else false
    }
    return hasSyntheticImportProjectReference
}

@Suppress("UNCHECKED_CAST")
private fun swiftPMDependencies(swiftPmDependenciesMetadataClasspath: ArtifactView): Provider<Map<String, SwiftPMImport>> {
    return swiftPmDependenciesMetadataClasspath
        .artifacts.resolvedArtifacts
        .map { artifacts ->
            artifacts.associate { resolvedArtifact ->
                val swiftPMPackageIdentifier = when (val componentId = resolvedArtifact.id.componentIdentifier) {
                    is ProjectComponentIdentifier -> componentId.projectPath.replace(Regex("[^a-zA-Z0-9]"), "_")
                    is ModuleComponentIdentifier -> "${componentId.group}_${componentId.module}_${componentId.version}".replace(Regex("[^a-zA-Z0-9]"), "_")
                    else -> error("Unexpected componentId: $componentId")
                }
                swiftPMPackageIdentifier to resolvedArtifact.file.inputStream().use {
                    ObjectInputStream(it).readObject() as SwiftPMImport
                }
            }
        }
}

private fun Project.swiftPMDependenciesMetadataConfiguration(): Configuration {
    val configurationName = "swiftPMDependenciesMetadataClasspath"
    val implementationDependencies = configurations.getByName(multiplatformExtension.sourceSets.commonMain.get().implementationConfigurationName)
    val apiDependencies = configurations.getByName(multiplatformExtension.sourceSets.commonMain.get().apiConfigurationName)
    return project.configurations.maybeCreateResolvable(configurationName) {
        // 1. Select metadataApiElements graph
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KOTLIN_METADATA))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        extendsFrom(implementationDependencies)
        extendsFrom(apiDependencies)
    }
}

internal fun Project.swiftPMDependenciesMetadataClasspath() = swiftPMDependencies(
    swiftPMDependenciesMetadataConfiguration().incoming.artifactView {
        it.withVariantReselection()
        it.lenient(true)
        it.attributes {
            // 2. Reselect SwiftPM metadata variant
            it.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_USAGE))
            it.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }
    }
)

private fun linkerScriptHack(): String = """
    #!/usr/bin/python3
    import os
    import sys
    from os.path import dirname
    
    if __name__ == '__main__':
        is_synthetic_linkage_call = False
        for arg in sys.argv:
            if arg.startswith('@rpath') and '${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}' in arg:
                is_synthetic_linkage_call = True
    
        if is_synthetic_linkage_call:
            print(sys.argv)
            filelist_index = None
            platform_version = None
            arch = None
            output = None
            syslibroot = None
            dependency_info = None
    
            for (index, arg) in enumerate(sys.argv[1:]):
                if arg == '-platform_version':
                    platform_version = sys.argv[index + 2:index + 5]
                if arg == '-filelist':
                    filelist_index = index + 2
    
                if arg == '-arch':
                    arch = sys.argv[index + 2]
                if arg == '-o':
                    output = sys.argv[index + 2]
                if arg == '-syslibroot':
                    syslibroot = sys.argv[index + 2]
                if arg == '-dependency_info':
                    dependency_info = sys.argv[index + 2]
            if filelist_index is None:
                raise 'No filelist'
    
            filelist_path = sys.argv[filelist_index]
            empty_object_file = None
            with open(filelist_path, 'r') as file:
                for line in file:
                    if '${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.o' in line:
                        empty_object_file = line
            if empty_object_file is None:
                raise f'Missing empty object file {filelist_path}'
    
            new_filelist = os.path.join(dirname(filelist_path), '_kotlinSwiftPMImport')
            with open(new_filelist, 'w') as file:
                file.write(empty_object_file)
    
            stub_ld_call = [
                "-dylib",
                "-dynamic",
                "-filelist", new_filelist,
                "-arch", arch,
                "-platform_version", *platform_version,
                "-syslibroot", syslibroot,
                "-dependency_info", dependency_info,
                "-lSystem",
                "-export_dynamic",
                "-o", output,
            ]
            print('ld ' + ' '.join([f'\'{arg}\'' for arg in stub_ld_call]))
            print(stub_ld_call)
            print(sys.argv)
            os.execlp('ld', 'ld', *stub_ld_call)
        else:
            os.execlp('ld', 'ld', *sys.argv[1:])

""".trimIndent()

const val XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER = "-clonedSourcePackagesDirPath"
const val SWIFTPM_DEPENDENCIES_METADATA_USAGE = "SWIFTPM_DEPENDENCIES_METADATA"