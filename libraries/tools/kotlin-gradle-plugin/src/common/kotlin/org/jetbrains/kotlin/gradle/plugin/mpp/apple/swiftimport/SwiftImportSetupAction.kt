@file:Suppress("SENSELESS_COMPARISON")
@file:OptIn(Idea222Api::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.project
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.prepareKotlinIdeaImportTask
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.DUMP_FILE_ARGS_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Platform
import org.jetbrains.kotlin.gradle.plugin.testTaskName
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.addConfigurationMetrics
import org.jetbrains.kotlin.gradle.utils.getAttributeSafely
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.io.ObjectInputStream
import kotlin.io.readLines
import kotlin.io.resolve


internal val SwiftImportSetupAction = KotlinProjectSetupAction {
    if (project.kotlinPropertiesProvider.disableSwiftPMImport) return@KotlinProjectSetupAction

    val kotlinExtension = project.multiplatformExtension
    val swiftPMImportExtension = locateOrRegisterSwiftPMDependenciesExtension()
    swiftPMImportExtension.swiftPMDependencies.all {
        project.addConfigurationMetrics {
            it.put(BooleanMetrics.KMP_SWIFT_PM_IMPORT_HAS_DIRECT_DEPENDENCIES, true)
        }
        project.addConfigurationMetrics {
            it.put(NumericalMetrics.KMP_SWIFT_PM_IMPORT_NUMBER_OF_DIRECT_DEPENDENCIES, 1)
        }
    }

    val isMacOSHost = HostManager.hostIsMac

    inheritSwiftPMDependenciesFromAppleCompilationDependencies()

    val syntheticImportProjectProductType = provider {
        val hasDynamicFrameworks = kotlinExtension.targets.filterIsInstance<KotlinNativeTarget>().any { target ->
            target.binaries.filterIsInstance<Framework>().any {
                !it.isStatic
            }
        }

        /**
         * FIXME: KT-83873 This linkage configuration is not correct in general
         */
        if (hasDynamicFrameworks) {
            SyntheticProductType.DYNAMIC
        } else {
            SyntheticProductType.INFERRED
        }
    }

    val transitiveSwiftPMDependenciesProvider = transitiveSwiftPMDependenciesProvider()
    val transitiveLocalSwiftPMDependenciesProvider = transitiveSwiftPMDependenciesProvider.map {
        it.metadataByDependencyIdentifier.values.flatMap { swiftPMDependencies ->
            swiftPMDependencies.dependencies.mapNotNull { dependency ->
                when (dependency) {
                    is SwiftPMDependency.Local -> dependency
                    is SwiftPMDependency.Remote -> null
                }
            }
        }
    }

    val syntheticImportProjectGenerationTaskForEmbedAndSignLinkage = locateOrRegisterRegenerateLinkageImportProjectTask()
    syntheticImportProjectGenerationTaskForEmbedAndSignLinkage.configure {
        it.configureWithExtension(swiftPMImportExtension)
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesProvider)
        it.syntheticProductType.set(syntheticImportProjectProductType)
    }

    val projectPathProvider = project.providers.environmentVariable(PROJECT_PATH_ENV)
    val syntheticImportProjectGenerationTaskForLinkageForCli = registerXcodeIntegrationLinkagePackageGeneration(
        swiftPMImportExtension = swiftPMImportExtension,
        projectPathProvider = projectPathProvider,
        syntheticImportProjectProductType = syntheticImportProjectProductType,
        transitiveSwiftPMDependenciesProvider = transitiveSwiftPMDependenciesProvider,
    )
    registerXcodeIntegrationTasks(
        projectPathProvider = projectPathProvider,
        syntheticImportProjectGenerationTaskForLinkageForCli = syntheticImportProjectGenerationTaskForLinkageForCli,
    )

    val computeLocalPackageDependencyInputFiles = project.locateOrRegisterTask<ComputeLocalPackageDependencyInputFiles>(
        ComputeLocalPackageDependencyInputFiles.TASK_NAME,
    ) {
        it.onlyIf("SwiftPM import is only supported on macOS hosts") { isMacOSHost }
        it.localPackages.addAll(
            transitiveLocalSwiftPMDependenciesProvider.map { deps ->
                deps.map { dep -> dep.absolutePath }
            }
        )
    }

    val syntheticImportProjectGenerationTaskForCinteropsAndLdDump = project.locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
        GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName,
    ) {
        it.configureWithExtension(swiftPMImportExtension)
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesProvider)
        /**
         * The reason we always use dynamic here is to force LD dump to happen at the same time as CC dump.
         *
         * FIXME: KT-84798 This might not be not what we actually want. Having dynamic linkage here might erroneously fail def file creation
         * if the linkage type is incompatible with consumed targets. Probably we want to do LD dump in a separate step and only if necessary
         */
        it.syntheticProductType.set(SyntheticProductType.DYNAMIC)
    }

    val syncPersistedPackageResolvedToSyntheticSwiftPMPackage = project.locateOrRegisterTask<SyncPackageResolvedTask>(
        SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME
    )

    val hasDirectOrTransitiveSwiftPMDependencies = hasDirectOrTransitiveSwiftPMDependencies()
    val fetchSyntheticImportProjectPackages = project.locateOrRegisterTask<FetchSyntheticImportProjectPackages>(
        FetchSyntheticImportProjectPackages.TASK_NAME,
    ) {
        it.onlyIf("SwiftPM import is only supported on macOS hosts") { isMacOSHost }
        it.onlyIf { hasDirectOrTransitiveSwiftPMDependencies.get() }
        it.dependsOn(hasDirectOrTransitiveSwiftPMDependencies)
        it.dependsOn(syncPersistedPackageResolvedToSyntheticSwiftPMPackage)
        it.dependsOn(syntheticImportProjectGenerationTaskForCinteropsAndLdDump)
        it.localPackageManifests.from(
            transitiveLocalSwiftPMDependenciesProvider.map { localPackageDependencyProvider ->
                localPackageDependencyProvider.map { localPackageDependency ->
                    localPackageDependency.absolutePath.resolve("Package.swift")
                }
            }
        )
        it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinteropsAndLdDump.map { it.syntheticImportProjectRoot.get() })
    }

    syncPersistedPackageResolvedToSyntheticSwiftPMPackage.configure { syncTaskProvider ->
        // dest files is fixed to synthetic package
        syncTaskProvider.destinationFile.set(
            fetchSyntheticImportProjectPackages.map {
                it.syntheticLockFile.get()
            }
        )
    }

    val syncSyntheticPackageResolvedToPersisted = project.locateOrRegisterTask<SyncPackageResolvedTask>(
        SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME
    )

    val syntheticImportTasks = listOf(
        syntheticImportProjectGenerationTaskForCinteropsAndLdDump,
        syntheticImportProjectGenerationTaskForEmbedAndSignLinkage,
        syntheticImportProjectGenerationTaskForLinkageForCli,
    )

    project.afterEvaluate {
        val persistedPackageResolved = providePersistedPackageResolved()

        syncPersistedPackageResolvedToSyntheticSwiftPMPackage.configure { taskProvider ->
            taskProvider.sourceFile.set(persistedPackageResolved)
        }

        when (val packageIdentifier = identifierSynchronizationOrNull()) {
            is PackageResolvedSynchronization.Identifier -> {
                val packageResolvedSynchronizationIdentifier = packageIdentifier.identifier
                kotlinExtension.targets.matching { it.supportsSwiftPMImport() }.all {
                    val aggregationService = SwiftPMLockTaskAggregationBuildService.registerIfAbsent(this)

                    val projectPath = project.path

                    aggregationService.get().contribute(
                        identifier = packageResolvedSynchronizationIdentifier,
                        projectPathContribution = projectPath,
                    )

                    val sharedCheckoutDir = provideIdentifierCheckoutDir(packageResolvedSynchronizationIdentifier)

                    val actualGeneratedClaimer = locateOrRegisterUmbrellaPackageGenerateTask(
                        identifier = packageResolvedSynchronizationIdentifier,
                        aggregationService = aggregationService,
                        isMacOSHost = isMacOSHost,
                    )
                    val actualFetchClaimer = locateOrRegisterUmbrellaFetchTask(
                        identifier = packageResolvedSynchronizationIdentifier,
                        aggregationService = aggregationService,
                        checkOutDir = sharedCheckoutDir,
                        actualGeneratedClaimer = actualGeneratedClaimer,
                        isMacOSHost = isMacOSHost,
                    )

                    syncPersistedPackageResolvedToSyntheticSwiftPMPackage.configure {
                        it.dependsOn(actualFetchClaimer)
                        it.onlyIf("Shared Package.resolved exists") {
                            persistedPackageResolved.asFile.exists()
                        }
                    }
                }
            }
            // If none, after resolution in synthetic we sync back to persisted location.
            // With identifier, it would cause the umbrella package to be overridden
            else -> {
                syncSyntheticPackageResolvedToPersisted.configure { taskProvider ->
                    taskProvider.sourceFile.set(
                        fetchSyntheticImportProjectPackages.map { task ->
                            task.syntheticLockFile.get()
                        }
                    )
                    taskProvider.destinationFile.set(persistedPackageResolved)
                    taskProvider.onlyIf("Synthetic Package.resolved exists") {
                        taskProvider.sourceFile.get().asFile.exists()
                    }
                }

                fetchSyntheticImportProjectPackages.configure {
                    it.finalizedBy(syncSyntheticPackageResolvedToPersisted)
                }
            }
        }
    }

    syntheticImportTasks.forEach {
        it.configure {
            it.onlyIf {
                hasDirectOrTransitiveSwiftPMDependencies.get()
            }
        }
    }

    prepareKotlinIdeaImportTask.configure {
        it.dependsOn(project.swiftPMImportIdeModelProvider())
    }

    kotlinExtension.targets.matching { it.supportsSwiftPMImport() }.all { target ->
        target as KotlinNativeTarget

        syntheticImportTasks.forEach {
            it.configure {
                it.konanTargets.add(target.konanTarget)
            }
        }

        locateOrRegisterSwiftPMDependenciesMetadataTaskForLockFilesAndConsumableConfiguration(
            swiftPMImportExtension,
            transitiveSwiftPMDependenciesProvider,
            target.konanTarget,
        )

        val cinteropName = "swiftPMImport"
        val targetPlatform = target.konanTarget.applePlatform
        // use sdk for a more conventional name
        val targetSdk = target.konanTarget.appleTarget.sdk

        val defFilesAndLdDumpGenerationTask = registerConvertSyntheticSwiftPMImportProjectIntoDefFile(
            fetchSyntheticImportProjectPackages = fetchSyntheticImportProjectPackages,
            computeLocalPackageDependencyInputFiles = computeLocalPackageDependencyInputFiles,
            syntheticImportProjectGenerationTaskForCinteropsAndLdDump = syntheticImportProjectGenerationTaskForCinteropsAndLdDump,
            discoverModulesImplicitly = swiftPMImportExtension.discoverClangModulesImplicitly,
            hasDirectOrTransitiveSwiftPMDependencies = hasDirectOrTransitiveSwiftPMDependencies,
            isMacOSHost = isMacOSHost,
            targetSdk = targetSdk,
            targetPlatform = targetPlatform,
        )
        defFilesAndLdDumpGenerationTask.configure {
            it.architectures.add(target.konanTarget.appleArchitecture)
        }

        tasks.configureEach { task ->
            if (task.name == target.testTaskName) {
                task as KotlinNativeTest
                configureTestTaskDyldSearchPaths(
                    task,
                    target,
                    syntheticImportProjectGenerationTaskForCinteropsAndLdDump,
                    defFilesAndLdDumpGenerationTask,
                )
            }
        }

        target.binaries.all { binary ->
            binary.linkTaskProvider.configure { linkTask ->
                if (binary is Framework && binary.isStatic) return@configure
                val isFrameworkBinary = binary is Framework
                val ldArgDumpPath = defFilesAndLdDumpGenerationTask.map {
                    if (isFrameworkBinary) {
                        it.ldFileForFrameworkLinkagePath(target.konanTarget.appleArchitecture)
                    } else {
                        it.ldFilePath(target.konanTarget.appleArchitecture)
                    }
                }
                val ldArgDumpFingerprintPath = defFilesAndLdDumpGenerationTask.map {
                    it.ldFileFingerprintPath(target.konanTarget.appleArchitecture)
                }
                linkTask.linkerOptionsProducerFingerprint.from(ldArgDumpFingerprintPath)
                linkTask.dependsOn(defFilesAndLdDumpGenerationTask)
                linkTask.doFirst {
                    it as KotlinNativeLink
                    it.additionalLinkerOpts.addAll(
                        ldArgDumpPath.get().get().asFile.readLines().single()
                            .split(DUMP_FILE_ARGS_SEPARATOR)
                            .filter { it.isNotEmpty() }
                    )
                }
            }
        }

        swiftPMImportExtension.swiftPMDependencies.all spmDependency@{ swiftPMDependency ->
            // Auto-enable commonization on 1+ consumed SwiftPM dependencies for IDE and metadata compilation of shared source sets
            kotlinPropertiesProvider.enableCInteropCommonizationSetByExternalPlugin = true
            // Expose declared SwiftPM dependencies in the outgoing variant on 1+ consumed SwiftPM dependencies
            locateOrRegisterSwiftPMDependenciesMetadataTaskAndConsumableConfiguration(
                swiftPMImportExtension,
            )

            val mainCompilationCinterops = target.compilations.getByName("main").cinterops
            // Create the cinterop and wire the def file into
            if (cinteropName !in mainCompilationCinterops.names) {
                val defFile = defFilesAndLdDumpGenerationTask.map {
                    it.defFilePath(target.konanTarget.appleArchitecture).get()
                }
                val swiftPMImportCinterop = mainCompilationCinterops.create(cinteropName)
                tasks.configureEach {
                    if (it.name == swiftPMImportCinterop.interopProcessingTaskName) {
                        it.onlyIf { hasDirectOrTransitiveSwiftPMDependencies.get() }
                    }
                }
                swiftPMImportCinterop.definitionFile.set(defFile)
                swiftPMImportCinterop.isGeneratedCinterop = true
            }

            when (swiftPMDependency) {
                is SwiftPMDependency.Local -> {
                    if (checkLocalSwiftDependencyIsValid(swiftPMDependency)) {
                        computeLocalPackageDependencyInputFiles.configure {
                            it.localPackages.add(swiftPMDependency.absolutePath)
                        }
                        fetchSyntheticImportProjectPackages.configure {
                            it.localPackageManifests.from(
                                swiftPMDependency.absolutePath.resolve("Package.swift")
                            )
                        }
                    }
                }
                is SwiftPMDependency.Remote -> Unit
            }

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

private fun Project.getIdentifierLockFilesMetadataProvider(): Provider<List<SwiftPMImportMetadataForLockFiles>> {
    if (!HostManager.hostIsMac) return provider { emptyList() }
    return swiftPMDependenciesForLockFilesResolvableMetadataConfiguration()
        .incoming.artifacts.resolvedArtifacts
        .map { artifacts ->
            artifacts
                .filter { artifact ->
                    artifact.variant.attributes.getAttributeSafely(
                        Usage.USAGE_ATTRIBUTE
                    ) == SWIFTPM_DEPENDENCIES_METADATA_FOR_LOCK_FILES_USAGE
                }
                .map { artifact ->
                    artifact.file.inputStream().use { input ->
                        ObjectInputStream(input).use { stream ->
                            stream.readObject() as SwiftPMImportMetadataForLockFiles
                        }
                    }
                }
                .sortedBy { it.projectPath }
        }
}

private fun Project.getKonanTargetsForUmbrellaPackageProvider(): Provider<Set<KonanTarget>>? =
    getIdentifierLockFilesMetadataProvider().map {
        it.flatMap { metadata -> metadata.konanTargets.orEmpty() }
            .distinct()
            .sortedBy { it.name }
            .toSet()
    }


private fun Project.getAggregatedTransitiveDependenciesProvider(): Provider<TransitiveSwiftPMDependencies> =
    getIdentifierLockFilesMetadataProvider().map { lockFilesMetadata ->
        val merged = linkedMapOf<SwiftPMDependencyIdentifier, SwiftPMImportMetadata>()

        lockFilesMetadata.forEach { contribution ->
            if (
                contribution.directDependencies.isEmpty() &&
                contribution.transitiveDependencies.metadataByDependencyIdentifier.isEmpty()
            ) {
                return@forEach
            }

            val selfIdentifier = SwiftPMDependencyIdentifier(
                contribution.projectPath.replace(":", "_"),
                isModular = false,
            )

            merged[selfIdentifier] = SwiftPMImportMetadata(
                konanTargets = contribution.konanTargets.map { it.name }.toSet(),
                contribution.iosDeploymentVersion,
                contribution.macosDeploymentVersion,
                contribution.watchosDeploymentVersion,
                contribution.tvosDeploymentVersion,
                false,
                contribution.directDependencies,
            )

            contribution.transitiveDependencies.metadataByDependencyIdentifier
                .entries
                .sortedBy { it.key.identifier }
                .forEach { (dependencyIdentifier, metadata) ->
                    merged.putIfAbsent(dependencyIdentifier, metadata)
                }
        }

        val deterministic = linkedMapOf<SwiftPMDependencyIdentifier, SwiftPMImportMetadata>()
        merged.entries
            .sortedBy { it.key.identifier }
            .forEach { (dependencyIdentifier, metadata) ->
                deterministic[dependencyIdentifier] = metadata
            }

        TransitiveSwiftPMDependencies(deterministic)
    }

private fun Project.updateDependenciesWithAggregatedResults(
    aggregationService: Provider<SwiftPMLockTaskAggregationBuildService>,
    packageResolvedSynchronizationIdentifier: String,
) {
    if (!HostManager.hostIsMac) return
    project.swiftPMDependenciesForLockFilesScopeConfiguration().dependencies.addAllLater(
        project.provider {
            aggregationService.get().buildAggregatedResultDependencies(
                packageResolvedSynchronizationIdentifier
            ).map {
                project.dependencies.project(path = it)
            }
        }
    )
}

private fun Project.locateOrRegisterUmbrellaFetchTask(
    identifier: String,
    aggregationService: Provider<SwiftPMLockTaskAggregationBuildService>,
    checkOutDir : Provider<Directory>,
    actualGeneratedClaimer: String?,
    isMacOSHost: Boolean,
): String? {
    val candidateFetchTaskName =
        FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)
    val projectCandidateFetchTaskName = "${project.path}:$candidateFetchTaskName"

    val isFetchClaimed =
        aggregationService.get().claimFetchTask(identifier, projectCandidateFetchTaskName)

    val actualFetchClaimer =
        aggregationService.get().getClaimedFetchTask(identifier)

    if (!isFetchClaimed) return actualFetchClaimer

    val aggregatedTransitiveDependencies = getAggregatedTransitiveDependenciesProvider()


    locateOrRegisterTask<FetchSyntheticImportProjectPackages>(candidateFetchTaskName) {
        it.syntheticImportProjectRoot.set(provideIdentifierPackageRoot(identifier))
        it.dependsOn(actualGeneratedClaimer)
        it.onlyIf("SwiftPM import is only supported on macOS hosts") { isMacOSHost }
        it.onlyIf { aggregatedTransitiveDependencies.get().metadataByDependencyIdentifier.values.any { it.dependencies.isNotEmpty() } }
        it.swiftPMDependenciesCheckout.set(checkOutDir)
        it.gitIgnoreCheckoutDir.set(true)
    }

    return actualFetchClaimer
}


private fun Project.locateOrRegisterUmbrellaPackageGenerateTask(
    identifier: String,
    aggregationService: Provider<SwiftPMLockTaskAggregationBuildService>,
    isMacOSHost: Boolean,
): String? {
    val swiftPMRootPath = provideIdentifierPackageRoot(identifier)

    val candidateGenerateTaskName =
        GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(identifier)
    val projectCandidateGenerateTaskName = "${project.path}:$candidateGenerateTaskName"

    val isGeneratedClaimed =
        aggregationService.get().claimGenerateTask(identifier, projectCandidateGenerateTaskName)

    val actualGeneratedClaimer =
        aggregationService.get().getClaimedGenerateTask(identifier)

    if (!isGeneratedClaimed) return actualGeneratedClaimer

    updateDependenciesWithAggregatedResults(
        aggregationService,
        identifier
    )

    val aggregatedTransitiveDependenciesProvider =
        getAggregatedTransitiveDependenciesProvider()

    val konanTargetsProvider = getKonanTargetsForUmbrellaPackageProvider()

    locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(candidateGenerateTaskName) { task ->
        task.onlyIf("SwiftPM import is only supported on macOS hosts") { isMacOSHost }
        task.onlyIf { aggregatedTransitiveDependenciesProvider.get().metadataByDependencyIdentifier.values.any { it.dependencies.isNotEmpty() } }
        task.useOnlyTransitiveImportedDependencies()
        task.syntheticProductType.set(SyntheticProductType.INFERRED)
        task.syntheticImportProjectRoot.set(swiftPMRootPath)
        task.dependencyIdentifierToImportedSwiftPMDependencies.set(
            aggregatedTransitiveDependenciesProvider
        )
        task.konanTargets.set(konanTargetsProvider)
    }
    return actualGeneratedClaimer
}

private fun KotlinTarget.supportsSwiftPMImport() = this is KotlinNativeTarget && this.konanTarget.family.isAppleFamily

private fun KonanTarget.swiftPMPlatform(): SwiftPMDependency.Platform = when (this) {
    KonanTarget.IOS_ARM64,
    KonanTarget.IOS_SIMULATOR_ARM64,
    KonanTarget.IOS_X64,
        -> Platform.iOS
    KonanTarget.MACOS_ARM64,
    KonanTarget.MACOS_X64,
        -> Platform.macOS
    KonanTarget.TVOS_ARM64,
    KonanTarget.TVOS_SIMULATOR_ARM64,
    KonanTarget.TVOS_X64,
        -> Platform.tvOS
    KonanTarget.WATCHOS_ARM32,
    KonanTarget.WATCHOS_ARM64,
    KonanTarget.WATCHOS_DEVICE_ARM64,
    KonanTarget.WATCHOS_SIMULATOR_ARM64,
    KonanTarget.WATCHOS_X64,
        -> Platform.watchOS

    KonanTarget.ANDROID_ARM32,
    KonanTarget.ANDROID_ARM64,
    KonanTarget.ANDROID_X64,
    KonanTarget.ANDROID_X86,
    KonanTarget.LINUX_ARM32_HFP,
    KonanTarget.LINUX_ARM64,
    KonanTarget.LINUX_X64,
    KonanTarget.MINGW_X64,
        -> error("unsupported targets")
}

private fun Project.checkLocalSwiftDependencyIsValid(swiftPMDependency: SwiftPMDependency.Local): Boolean {
    val resolvedPath = swiftPMDependency.absolutePath
    val originalPath = project.projectDir.toPath().relativize(resolvedPath.toPath()).toString()

    // Validate at configuration time for direct dependencies using diagnostics
    if (!resolvedPath.exists()) {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.SwiftPMLocalPackageDirectoryNotFound(
                resolvedPath.absolutePath,
                originalPath
            )
        )
        return false
    }

    if (!resolvedPath.resolve("Package.swift").exists()) {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.SwiftPMLocalPackageMissingManifest(resolvedPath)
        )
        return false
    }

    if (swiftPMDependency.packageName.isBlank()) {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.SwiftPMLocalPackageInvalidName(originalPath)
        )
        return false
    }

    return true
}

internal fun Project.identifierSynchronizationOrNull(): PackageResolvedSynchronization.Identifier? {
    val sync = locateOrRegisterSwiftPMDependenciesExtension()
        .packageResolvedSynchronization
    return sync as? PackageResolvedSynchronization.Identifier
}

/**
 * For test tasks that depend on dynamic frameworks we pass the DYLD_ env search path
 */
private fun configureTestTaskDyldSearchPaths(
    task: KotlinNativeTest,
    target: KotlinNativeTarget,
    syntheticImportProjectGenerationTaskForCinteropsAndLdDump: TaskProvider<GenerateSyntheticLinkageImportProject>,
    defFilesAndLdDumpGenerationTask: TaskProvider<ConvertSyntheticSwiftPMImportProjectIntoDefFile>,
) {
    val frameworkSearchPathsDump = defFilesAndLdDumpGenerationTask.get().frameworkSearchpathFilePath(target.konanTarget.appleArchitecture)
    val librariesSearchPathsDump = defFilesAndLdDumpGenerationTask.get().librarySearchpathFilePath(target.konanTarget.appleArchitecture)

    val frameworksDyldEnv =
        if (task is KotlinNativeSimulatorTest) "SIMCTL_CHILD_DYLD_FALLBACK_FRAMEWORK_PATH" else "DYLD_FALLBACK_FRAMEWORK_PATH"

    fun extractFrameworkSearchPaths() = frameworkSearchPathsDump.get().asFile.readLines().single()
        .split(DUMP_FILE_ARGS_SEPARATOR).filter { it.isNotEmpty() }
        .joinToString(":")

    val librariesDyldEnv =
        if (task is KotlinNativeSimulatorTest) "SIMCTL_CHILD_DYLD_FALLBACK_LIBRARY_PATH" else "DYLD_FALLBACK_LIBRARY_PATH"

    fun extractLibrariesSearchPaths() = librariesSearchPathsDump.get().asFile.readLines().single()
        .split(DUMP_FILE_ARGS_SEPARATOR).filter { it.isNotEmpty() }
        .joinToString(":")

    // lazyMapWithCC fails with ClassNotFound exception in Gradle 7.6.3
    if (GradleVersion.current().baseVersion < GradleVersion.version("8.0")) {
        task.doFirst {
            it as KotlinNativeTest
            it.processOptions.environment.put(
                frameworksDyldEnv,
                extractFrameworkSearchPaths()
            )
            it.processOptions.environment.put(
                librariesDyldEnv,
                extractLibrariesSearchPaths()
            )
        }
    } else {
        task.processOptions.environment.put(
            frameworksDyldEnv,
            syntheticImportProjectGenerationTaskForCinteropsAndLdDump.lazyMapWithCC {
                extractFrameworkSearchPaths()
            }
        )
        task.processOptions.environment.put(
            librariesDyldEnv,
            syntheticImportProjectGenerationTaskForCinteropsAndLdDump.lazyMapWithCC {
                extractLibrariesSearchPaths()
            }
        )
    }
}

internal fun Project.locateOrRegisterRegenerateLinkageImportProjectTask(): TaskProvider<GenerateSyntheticLinkageImportProject> {
    val hasDirectOrTransitiveSwiftPMDependencies = hasDirectOrTransitiveSwiftPMDependencies()
    return locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
        lowerCamelCaseName(
            GenerateSyntheticLinkageImportProject.TASK_NAME,
            "forEmbedAndSignLinkage",
        ),
    ).also {
        it.configure {
            /**
             * When we build in Xcode, we want to fail the build if the Package.swift regeneration resulted in non-idempotent changes
             * because these changes won't be picked up by the build system until the package is "reloaded".
             *
             * FIXME: KMT-2005 In KMP IDE plugin we can avoid failing on non-idempotent changes, but right now KMP IDE plugin doesn't handle
             * linkage package well if it regenerates during the build.
             */
            it.failOnNonIdempotentChanges.set(true)
            it.buildingFromXcode.set(project.providers.systemProperty("idea.active").map { _ -> false }.orElse(true))
            it.dependsOn(hasDirectOrTransitiveSwiftPMDependencies)
            it.onlyIf {
                hasDirectOrTransitiveSwiftPMDependencies.get()
            }
        }
    }
}

private fun Project.registerConvertSyntheticSwiftPMImportProjectIntoDefFile(
    computeLocalPackageDependencyInputFiles: TaskProvider<ComputeLocalPackageDependencyInputFiles>,
    fetchSyntheticImportProjectPackages: TaskProvider<FetchSyntheticImportProjectPackages>,
    syntheticImportProjectGenerationTaskForCinteropsAndLdDump: TaskProvider<GenerateSyntheticLinkageImportProject>,
    discoverModulesImplicitly: Provider<Boolean>,
    hasDirectOrTransitiveSwiftPMDependencies: Provider<Boolean>,
    isMacOSHost: Boolean,
    targetSdk: String,
    targetPlatform: String,
): TaskProvider<ConvertSyntheticSwiftPMImportProjectIntoDefFile> {
    return project.locateOrRegisterTask<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(
        lowerCamelCaseName(
            ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
            targetSdk,
        )
    ) {
        it.onlyIf("SwiftPM import doesn't support non macOS hosts") { isMacOSHost }
        it.dependsOn(fetchSyntheticImportProjectPackages)
        it.dependsOn(computeLocalPackageDependencyInputFiles)
        it.resolvedPackagesState.from(
            fetchSyntheticImportProjectPackages.map { it.inputManifests },
            fetchSyntheticImportProjectPackages.map { it.syntheticLockFile },
        )
        it.xcodebuildPlatform.set(targetPlatform)
        it.xcodebuildSdk.set(targetSdk)
        it.swiftPMDependenciesCheckout.set(fetchSyntheticImportProjectPackages.map { it.swiftPMDependenciesCheckout.get() })
        it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinteropsAndLdDump.map { it.syntheticImportProjectRoot.get() })
        it.discoverModulesImplicitly.set(discoverModulesImplicitly)
        it.filesToTrackFromLocalPackages.set(computeLocalPackageDependencyInputFiles.flatMap { it.filesToTrackFromLocalPackages })
        it.hasSwiftPMDependencies.set(hasDirectOrTransitiveSwiftPMDependencies)
    }
}

internal const val PROJECT_PATH_ENV = "XCODEPROJ_PATH"
internal fun searchForGradlew(path: File?): File? {
    if (path == null) return null
    path.listFiles().firstOrNull { it.name == "gradlew" }?.let { return it }
    return searchForGradlew(path.parentFile)
}

internal fun Project.hasDirectOrTransitiveSwiftPMDependencies(): Provider<Boolean> {
    val swiftPMImportExtension = locateOrRegisterSwiftPMDependenciesExtension()
    val hasDirectSwiftPMDependencies = provider { swiftPMImportExtension.swiftPMDependencies.isNotEmpty() }
    return transitiveSwiftPMDependenciesProvider().map { transitiveDependencies ->
        hasDirectSwiftPMDependencies.get() || transitiveDependencies.metadataByDependencyIdentifier.values.any { it.dependencies.isNotEmpty() }
    }
}

private fun Project.registerXcodeIntegrationLinkagePackageGeneration(
    swiftPMImportExtension: SwiftPMImportExtension,
    projectPathProvider: Provider<String>,
    syntheticImportProjectProductType: Provider<SyntheticProductType>,
    transitiveSwiftPMDependenciesProvider: Provider<TransitiveSwiftPMDependencies>,
): TaskProvider<GenerateSyntheticLinkageImportProject> {
    return registerTask<GenerateSyntheticLinkageImportProject>(
        lowerCamelCaseName(
            GenerateSyntheticLinkageImportProject.TASK_NAME,
            "forLinkageForCli",
        ),
    ) {
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesProvider)
        it.configureWithExtension(swiftPMImportExtension)
        it.syntheticImportProjectRoot.set(
            projectPathProvider.flatMap {
                project.layout.dir(
                    project.provider { File(it).parentFile.resolve(SYNTHETIC_IMPORT_TARGET_MAGIC_NAME) }
                )
            }
        )
        it.syntheticProductType.set(syntheticImportProjectProductType)
    }
}

private fun Project.registerXcodeIntegrationTasks(
    syntheticImportProjectGenerationTaskForLinkageForCli: TaskProvider<GenerateSyntheticLinkageImportProject>,
    projectPathProvider: Provider<String>,
) {
    val embedAndSignIntegration =
        project.registerTask<IntegrateEmbedAndSignIntoXcodeProject>(IntegrateEmbedAndSignIntoXcodeProject.TASK_NAME) {
            it.dependsOn(syntheticImportProjectGenerationTaskForLinkageForCli)
            it.currentDir.set(gradle.startParameter.currentDir)
            it.xcodeprojPath.set(projectPathProvider)
        }
    project.registerTask<IntegrateLinkagePackageIntoXcodeProject>(IntegrateLinkagePackageIntoXcodeProject.TASK_NAME) {
        it.dependsOn(syntheticImportProjectGenerationTaskForLinkageForCli)
        it.currentDir.set(gradle.startParameter.currentDir)
        it.xcodeprojPath.set(projectPathProvider)
        it.mustRunAfter(embedAndSignIntegration)
    }
}

private fun Project.providePersistedPackageResolvedSync(): PackageResolvedSynchronization {
    val swiftPMImportExtension = locateOrRegisterSwiftPMDependenciesExtension()
    return swiftPMImportExtension.packageResolvedSynchronization
}

private fun Project.rootDirFile(): File = rootProject.projectDir

private fun Project.providePersistedPackageResolved(): RegularFile {
    return when (val syncStrategy = providePersistedPackageResolvedSync()) {
        is PackageResolvedSynchronization.Identifier -> {
            layout.file(
                provider {
                    rootDirFile().resolve(".swiftpm-locks/${syncStrategy.identifier}/swiftImport/Package.resolved")
                }
            ).get()
        }
        else -> {
            layout.projectDirectory.file("Package.resolved")
        }
    }
}

private fun Project.providerIdentifierRoot(identifier: String): Provider<Directory> =
    layout.dir(
        provider {
            rootDirFile().resolve(".swiftpm-locks/$identifier")
        }
    )

private fun Project.provideIdentifierPackageRoot(identifier: String): Provider<Directory> =
    providerIdentifierRoot(identifier).map { it.dir("swiftImport") }

private fun Project.provideIdentifierCheckoutDir(identifier: String) : Provider<Directory> {
    return providerIdentifierRoot(identifier).map { it.dir("swiftPMCheckout") }
}


internal fun Project.swiftPMImportIdeModelProvider(): Provider<SwiftPMImportIdeModel> =
    project.hasDirectOrTransitiveSwiftPMDependencies().map { hasDirectOrTransitiveSwiftPMDependencies ->
        SwiftPMImportIdeModel(
            hasDirectOrTransitiveSwiftPMDependencies,
            ("${project.path}:${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}").replace("::", ":"),
            SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
        )
    }
