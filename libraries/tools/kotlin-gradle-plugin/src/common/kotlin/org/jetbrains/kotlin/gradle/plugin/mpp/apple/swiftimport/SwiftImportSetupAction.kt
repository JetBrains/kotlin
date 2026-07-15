@file:Suppress("SENSELESS_COMPARISON")
@file:OptIn(Idea222Api::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.project
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.prepareKotlinIdeaImportTask
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.DUMP_FILE_ARGS_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
import org.jetbrains.kotlin.gradle.plugin.testTaskName
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.addConfigurationMetrics
import org.jetbrains.kotlin.gradle.utils.getAttributeSafely
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.io.ObjectInputStream

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
    val ideaSyncEnabled = project.isInIdeaSync

    inheritSwiftPMDependenciesFromAppleCompilationDependencies()

    val syntheticImportProjectProductType = syntheticImportProjectProductTypeFromFrameworkTypes()

    val transitiveSwiftPMMetadataProvider = transitiveSwiftPMMetadataProvider()
    val transitiveLocalSwiftPMMetadataProvider = transitiveSwiftPMMetadataProvider.map {
        it.metadataByDependencyIdentifier.values.flatMap { swiftPMDependencies ->
            swiftPMDependencies.dependencies.mapNotNull { dependency ->
                when (dependency) {
                    is SwiftPMDependency.Local -> dependency
                    is SwiftPMDependency.Remote -> null
                }
            }
        }
    }
    val hasLocalSwiftPMDependencies = transitiveLocalSwiftPMMetadataProvider.map { it.isNotEmpty() }

    val syntheticImportProjectGenerationTaskForEmbedAndSignLinkage = locateOrRegisterRegenerateLinkageImportProjectTask()
    syntheticImportProjectGenerationTaskForEmbedAndSignLinkage.configure {
        it.configureWithExtension(swiftPMImportExtension)
        it.transitiveSwiftPMMetadata.set(transitiveSwiftPMMetadataProvider)
        it.syntheticProductType.set(syntheticImportProjectProductType)
    }

    val projectPathProvider = project.providers.environmentVariable(PROJECT_PATH_ENV)
    val syntheticImportProjectGenerationTaskForLinkageForCli = registerXcodeIntegrationLinkagePackageGeneration(
        swiftPMImportExtension = swiftPMImportExtension,
        projectPathProvider = projectPathProvider,
        syntheticImportProjectProductType = syntheticImportProjectProductType,
        transitiveSwiftPMMetadataProvider = transitiveSwiftPMMetadataProvider,
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
            transitiveLocalSwiftPMMetadataProvider.map { deps ->
                deps.map { dep -> dep.absolutePath }
            }
        )
    }


    val directSwiftPMMetadataProvider = directSwiftPMMetadataProvider(swiftPMImportExtension)

    val fingerprintSyntheticPackageTask = project.locateOrRegisterTask<FingerprintSyntheticPackage>(
        FingerprintSyntheticPackage.TASK_NAME
    ) {
        it.transitiveSwiftPMMetadata.set(
            transitiveSwiftPMMetadataProvider
        )
        it.directSwiftPMMetadata.set(
            directSwiftPMMetadataProvider
        )
    }


    val validateLocalSwiftPMDependencies = project.locateOrRegisterTask<ValidateLocalSwiftPMDependencies>(
        ValidateLocalSwiftPMDependencies.TASK_NAME,
    ) {
        it.onlyIf("SwiftPM import is only supported on macOS hosts") { isMacOSHost }
        it.setupKotlinToolingDiagnosticsParameters(project)
        it.projectDir.set(project.projectDir)
        it.localDependencies.addAll(transitiveLocalSwiftPMMetadataProvider)
    }

    computeLocalPackageDependencyInputFiles.configure {
        it.dependsOn(validateLocalSwiftPMDependencies)
    }

    val syntheticImportProjectGenerationTaskForCinteropsAndLdDump = project.locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
        GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName,
    ) { generateTask ->
        generateTask.configureWithExtension(swiftPMImportExtension)
        generateTask.transitiveSwiftPMMetadata.set(transitiveSwiftPMMetadataProvider)
        /**
         * The reason we always use dynamic here is to force LD dump to happen at the same time as CC dump.
         *
         * FIXME: KT-84798 This might not be not what we actually want. Having dynamic linkage here might erroneously fail def file creation
         * if the linkage type is incompatible with consumed targets. Probably we want to do LD dump in a separate step and only if necessary
         */
        generateTask.syntheticProductType.set(GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType.DYNAMIC)
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
        it.ideaSyncEnabled.set(ideaSyncEnabled)
        it.testExecutionHooks.set(swiftPMImportExtension.testExecutionHooks)
        it.testExecutionService.set(swiftPMImportExtension.testExecutionService)
        it.dependsOn(hasDirectOrTransitiveSwiftPMDependencies)
        it.dependsOn(syncPersistedPackageResolvedToSyntheticSwiftPMPackage)
        it.dependsOn(syntheticImportProjectGenerationTaskForCinteropsAndLdDump)
        it.dependsOn(hasLocalSwiftPMDependencies.map { hasLocal ->
            if (hasLocal) listOf(validateLocalSwiftPMDependencies) else emptyList()
        })
        it.localPackageManifests.from(
            transitiveLocalSwiftPMMetadataProvider.map { localPackageDependencyProvider ->
                localPackageDependencyProvider.map { localPackageDependency ->
                    localPackageDependency.absolutePath.resolve("Package.swift")
                }
            }
        )
        it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinteropsAndLdDump.map { it.syntheticImportProjectRoot.get() })
    }

    val syncSyntheticPackageResolvedToPersisted = project.locateOrRegisterTask<SyncPackageResolvedTask>(
        SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME
    )

    val fingerprintCoordinationService = SwiftImportFingerprintedCoordinationService.registerIfAbsent(
        this,
        provideXcodeDumpsDir(),
        provideCheckoutDir(),
        provideSyntheticPackageDir(),
    )

    project.launch {
        KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await()

        val persistedPackageResolved = providePersistedPackageResolved()

        syncPersistedPackageResolvedToSyntheticSwiftPMPackage.configure { taskProvider ->
            taskProvider.sourceFile.set(persistedPackageResolved)
        }

        syncSyntheticPackageResolvedToPersisted.configure { taskProvider ->
            taskProvider.destinationFile.set(persistedPackageResolved)
        }

        fingerprintSyntheticPackageTask.configure {
            it.packageResolvedSynchronizationFingerprint.set(
                swiftPMImportExtension.packageResolvedSynchronization
            )
        }

        when (val packageIdentifier = identifierSynchronizationOrNull()) {
            is PackageResolvedSynchronization.Identifier -> {
                val packageResolvedSynchronizationIdentifier = packageIdentifier.identifier
                enableFingerprintCoordination(
                    fingerprintCoordinationService = fingerprintCoordinationService,
                    generateSyntheticPackageTask = syntheticImportProjectGenerationTaskForCinteropsAndLdDump,
                    fingerprintSyntheticPackageTask = fingerprintSyntheticPackageTask,
                    transitiveSwiftPMMetadataProvider = transitiveSwiftPMMetadataProvider,
                    directSwiftPMMetadata = directSwiftPMMetadataProvider,
                    fetchSyntheticImportProjectPackages = fetchSyntheticImportProjectPackages,
                    syncPersistedPackageResolvedToSyntheticSwiftPMPackage = syncPersistedPackageResolvedToSyntheticSwiftPMPackage,
                    syncSyntheticPackageResolvedToPersisted = syncSyntheticPackageResolvedToPersisted,
                )

                if (multiplatformExtension.awaitTargets().any { it.supportsSwiftPMImport() }) {

                    val aggregationService = SwiftPMLockTaskAggregationBuildService.registerIfAbsent(project)

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
                syncPersistedPackageResolvedToSyntheticSwiftPMPackage.configure { taskProvider ->
                    taskProvider.destinationFile.set(
                        fetchSyntheticImportProjectPackages.map { task ->
                            task.syntheticLockFile.get()
                        }
                    )
                }
                fetchSyntheticImportProjectPackages.configure {
                    it.finalizedBy(syncSyntheticPackageResolvedToPersisted)
                }
            }
        }
    }

    val syntheticImportTasks = listOf(
        syntheticImportProjectGenerationTaskForCinteropsAndLdDump,
        syntheticImportProjectGenerationTaskForEmbedAndSignLinkage,
        syntheticImportProjectGenerationTaskForLinkageForCli,
    )

    syntheticImportTasks.forEach {
        it.configure {
            it.onlyIf("Has SwiftPM dependencies") {
                hasDirectOrTransitiveSwiftPMDependencies.get()
            }
        }
    }

    prepareKotlinIdeaImportTask.configure {
        it.dependsOn(project.swiftPMImportIdeModelProvider())
    }

    kotlinExtension.swiftPMImportTargets().all { target ->
        target as KotlinNativeTarget

        tasks.withType(GenerateSyntheticLinkageImportProject::class.java).configureEach {
            it.konanTargets.add(target.konanTarget)
        }

        locateOrRegisterSwiftPMDependenciesMetadataTaskForLockFilesAndConsumableConfiguration(
            swiftPMImportExtension,
            transitiveSwiftPMMetadataProvider,
            target.konanTarget,
        )

        val cinteropName = "swiftPMImport"
        val targetPlatform = target.konanTarget.applePlatform
        // use sdk for a more conventional name
        val targetSdk = target.konanTarget.appleTarget.sdk

        val defFilesAndLdDumpGenerationTask = registerConvertSyntheticSwiftPMImportProjectIntoDefFile(
            discoverModulesImplicitly = swiftPMImportExtension.discoverClangModulesImplicitly,
            isMacOSHost = isMacOSHost,
            targetSdk = targetSdk,
        )

        defFilesAndLdDumpGenerationTask.configure {
            it.architectures.add(target.konanTarget.appleArchitecture)
        }


        val fingerprintXcode = project.locateOrRegisterTask<FingerprintXcodeBuild>(
            lowerCamelCaseName(
                FingerprintXcodeBuild.TASK_NAME,
                targetSdk,
            )
        ) { fingerprintTask ->
            fingerprintTask.onlyIf("SwiftPM import doesn't support non macOS hosts") { isMacOSHost }
            fingerprintTask.syntheticPackageFingerprint.set(fingerprintSyntheticPackageTask.map { it.syntheticPackageFingerprint.get() })
            fingerprintTask.xcodebuildSdk.set(targetSdk)
        }

        fingerprintXcode.configure {
            it.architectures.add(target.konanTarget.appleArchitecture)

        }

        val xcodebuildDumpTaskName = lowerCamelCaseName(
            DumpXcodeBuildArgs.TASK_NAME,
            targetSdk,
        )
        val xcodebuildDumpTask = registerDumpXcodebuildArgsTask(
            taskName = xcodebuildDumpTaskName,
            computeLocalPackageDependencyInputFiles = computeLocalPackageDependencyInputFiles,
            fetchSyntheticImportProjectPackages = fetchSyntheticImportProjectPackages,
            hasDirectOrTransitiveSwiftPMDependencies = hasDirectOrTransitiveSwiftPMDependencies,
            syntheticImportProjectGenerationTaskForCinteropsAndLdDump = syntheticImportProjectGenerationTaskForCinteropsAndLdDump,
            targetSdk = targetSdk,
            targetPlatform = targetPlatform,
            isMacOSHost = isMacOSHost,
        )

        xcodebuildDumpTask.configure {
            it.architectures.add(target.konanTarget.appleArchitecture)
        }

        defFilesAndLdDumpGenerationTask.configure { task ->
            task.dependsOn(xcodebuildDumpTask)
            task.localPackages
                .filesToTrackFromLocalPackages
                .set(computeLocalPackageDependencyInputFiles.map { it.filesToTrackFromLocalPackages.get() })
        }

        project.afterEvaluate {
            when (identifierSynchronizationOrNull()) {
                is PackageResolvedSynchronization.Identifier -> {
                    xcodebuildDumpTask.configure {
                        it.fingerprintCoordinationService.set(fingerprintCoordinationService)
                        it.syntheticPackageFingerprint.set(
                            fingerprintSyntheticPackageTask.map { it.syntheticPackageFingerprint.get() }
                        )
                        it.xcodebuildFingerprint.set(fingerprintXcode.map { it.xcodebuildFingerprint.get() })
                    }

                    defFilesAndLdDumpGenerationTask.configure { defFileTask ->
                        defFileTask.xcodebuildFingerprint.set(
                            xcodebuildDumpTask.map { it.xcodebuildFingerprint.get() }
                        )
                        defFileTask.fingerprintsXcodeDumpsDir.set(provideXcodeDumpsDir())
                    }
                }
            }
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
                        it as CInteropProcess
                        it.onlyIf { hasDirectOrTransitiveSwiftPMDependencies.get() }
                        it.macroNamesCollectingMode.set(kotlinPropertiesProvider.swiftPMMacroCollectingMode)
                    }
                }
                swiftPMImportCinterop.definitionFile.set(defFile)
                swiftPMImportCinterop.isGeneratedCinterop = true
            }

            when (swiftPMDependency) {
                is SwiftPMDependency.Local -> {
                    validateLocalSwiftPMDependencies.configure {
                        it.localDependencies.add(swiftPMDependency)
                    }
                    computeLocalPackageDependencyInputFiles.configure {
                        it.localPackages.add(swiftPMDependency.absolutePath)
                    }
                    fetchSyntheticImportProjectPackages.configure {
                        it.localPackageManifests.from(
                            swiftPMDependency.absolutePath.resolve("Package.swift")
                        )
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

internal fun Project.syntheticImportProjectProductTypeFromFrameworkTypes() = provider {
    val hasDynamicFrameworks = multiplatformExtension.targets.filterIsInstance<KotlinNativeTarget>().any { target ->
        // FIXME: KT-85471 Deriving product type this way is not correct in case of frameworks with different linkage types
        target.binaries.filterIsInstance<Framework>().any {
            !it.isStatic
        }
    }

    /**
     * FIXME: KT-83873 This linkage configuration is not correct in general
     */
    if (hasDynamicFrameworks) {
        GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType.DYNAMIC
    } else {
        GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType.INFERRED
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


private fun Project.getAggregatedTransitiveDependenciesProvider(): Provider<TransitiveSwiftPMMetadata> =
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

        TransitiveSwiftPMMetadata(deterministic)
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

private fun Project.enableFingerprintCoordination(
    fingerprintCoordinationService: Provider<SwiftImportFingerprintedCoordinationService>,
    generateSyntheticPackageTask: TaskProvider<GenerateSyntheticLinkageImportProject>,
    fingerprintSyntheticPackageTask: TaskProvider<FingerprintSyntheticPackage>,
    transitiveSwiftPMMetadataProvider: Provider<TransitiveSwiftPMMetadata>,
    fetchSyntheticImportProjectPackages: TaskProvider<FetchSyntheticImportProjectPackages>,
    directSwiftPMMetadata: Provider<SwiftPMImportMetadata>,
    syncPersistedPackageResolvedToSyntheticSwiftPMPackage: TaskProvider<SyncPackageResolvedTask>,
    syncSyntheticPackageResolvedToPersisted: TaskProvider<SyncPackageResolvedTask>,
) {
    val fingerprintedSwiftPMDependencyGraph = transitiveSwiftPMMetadataProvider.zip(directSwiftPMMetadata) { transitiveMetadata, directMetadata ->
        fingerprintSwiftPMDependencyGraph(
            directMetadata,
            transitiveMetadata,
            normalizeVersions = false,
        )
    }

    syncPersistedPackageResolvedToSyntheticSwiftPMPackage.configure {
        // dest files is fixed to synthetic package
        it.syntheticPackagesRoot.set(
            provideSyntheticPackageDir()
        )
        it.packageFingerprint.set(
            fingerprintSyntheticPackageTask.map {
                it.syntheticPackageFingerprint.get()
            }
        )
        it.dependsOn(fingerprintSyntheticPackageTask)
    }

    syncSyntheticPackageResolvedToPersisted.configure {
        it.syntheticPackagesRoot.set(
            provideSyntheticPackageDir()
        )
        it.packageFingerprint.set(
            fingerprintSyntheticPackageTask.map {
                it.syntheticPackageFingerprint.get()
            }
        )
    }

    generateSyntheticPackageTask.configure {
        it.useOnlyTransitiveImportedDependencies()
        it.coordinationService.set(fingerprintCoordinationService)
        it.syntheticPackageFingerprint.set(
            fingerprintSyntheticPackageTask.map { it.syntheticPackageFingerprint.get() }
        )
        it.transitiveSwiftPMMetadata.set(fingerprintedSwiftPMDependencyGraph)
    }

    fetchSyntheticImportProjectPackages.configure {
        it.syntheticPackageFingerprint.set(
            fingerprintSyntheticPackageTask.map { it.syntheticPackageFingerprint.get() }
        )
        it.coordinationService.set(fingerprintCoordinationService)
    }
}

private fun Project.locateOrRegisterUmbrellaFetchTask(
    identifier: String,
    aggregationService: Provider<SwiftPMLockTaskAggregationBuildService>,
    checkOutDir: Provider<Directory>,
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
        it.ideaSyncEnabled.set(project.isInIdeaSync)
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
        task.syntheticProductType.set(GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType.INFERRED)
        task.syntheticImportProjectRoot.set(swiftPMRootPath)
        task.transitiveSwiftPMMetadata.set(
            aggregatedTransitiveDependenciesProvider
        )
        task.konanTargets.set(konanTargetsProvider)
    }
    return actualGeneratedClaimer
}

private fun Project.directSwiftPMMetadataProvider(
    swiftPMImportExtension: SwiftPMImportExtension,
): Provider<SwiftPMImportMetadata> = provider {
    SwiftPMImportMetadata(
        konanTargets = multiplatformExtension.swiftPMImportTargets()
            .map { (it as KotlinNativeTarget).konanTarget.name }
            .sorted()
            .toSet(),
        iosDeploymentVersion = swiftPMImportExtension.iosMinimumDeploymentTarget.orNull,
        macosDeploymentVersion = swiftPMImportExtension.macosMinimumDeploymentTarget.orNull,
        watchosDeploymentVersion = swiftPMImportExtension.watchosMinimumDeploymentTarget.orNull,
        tvosDeploymentVersion = swiftPMImportExtension.tvosMinimumDeploymentTarget.orNull,
        isModulesDiscoveryEnabled = swiftPMImportExtension.discoverClangModulesImplicitly.get(),
        dependencies = swiftPMImportExtension.swiftPMDependencies.toSet(),
    )
}

private fun KotlinTarget.supportsSwiftPMImport() = this is KotlinNativeTarget && this.konanTarget.family.isAppleFamily

private fun KotlinMultiplatformExtension.swiftPMImportTargets() =
    targets.matching { it.supportsSwiftPMImport() }

private fun KonanTarget.swiftPMPlatform(): SwiftPMDependency.Platform = when (this) {
    KonanTarget.IOS_ARM64,
    KonanTarget.IOS_SIMULATOR_ARM64,
    KonanTarget.IOS_X64,
        -> SwiftPMDependency.Platform.iOS
    KonanTarget.MACOS_ARM64,
    KonanTarget.MACOS_X64,
        -> SwiftPMDependency.Platform.macOS
    KonanTarget.TVOS_ARM64,
    KonanTarget.TVOS_SIMULATOR_ARM64,
    KonanTarget.TVOS_X64,
        -> SwiftPMDependency.Platform.tvOS
    KonanTarget.WATCHOS_ARM64,
    KonanTarget.WATCHOS_DEVICE_ARM64,
    KonanTarget.WATCHOS_SIMULATOR_ARM64,
    KonanTarget.WATCHOS_X64,
        -> SwiftPMDependency.Platform.watchOS

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
    discoverModulesImplicitly: Provider<Boolean>,
    isMacOSHost: Boolean,
    targetSdk: String,
): TaskProvider<ConvertSyntheticSwiftPMImportProjectIntoDefFile> {
    return project.locateOrRegisterTask<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(
        lowerCamelCaseName(
            ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
            targetSdk,
        )
    ) {
        it.onlyIf("SwiftPM import doesn't support non macOS hosts") { isMacOSHost }
        it.xcodebuildSdk.set(targetSdk)
        it.discoverModulesImplicitly.set(discoverModulesImplicitly)
        it.hasSwiftPMDependencies.set(hasDirectOrTransitiveSwiftPMDependencies())
        it.ideaSyncEnabled.set(project.isInIdeaSync)
    }
}

private fun Project.registerDumpXcodebuildArgsTask(
    taskName: String,
    computeLocalPackageDependencyInputFiles: TaskProvider<ComputeLocalPackageDependencyInputFiles>,
    fetchSyntheticImportProjectPackages: TaskProvider<FetchSyntheticImportProjectPackages>,
    syntheticImportProjectGenerationTaskForCinteropsAndLdDump: TaskProvider<GenerateSyntheticLinkageImportProject>,
    hasDirectOrTransitiveSwiftPMDependencies: Provider<Boolean>,
    targetSdk: String,
    targetPlatform: String,
    isMacOSHost: Boolean,
): TaskProvider<DumpXcodeBuildArgs> {
    return project.locateOrRegisterTask<DumpXcodeBuildArgs>(
        taskName
    ) { dumpTask ->
        dumpTask.onlyIf("SwiftPM import doesn't support non macOS hosts") { isMacOSHost }
        dumpTask.onlyIf("Project does not have any SwiftPM dependencies") { hasDirectOrTransitiveSwiftPMDependencies.get() }
        dumpTask.dependsOn(hasDirectOrTransitiveSwiftPMDependencies)
        dumpTask.testExecutionHooks.set(locateOrRegisterSwiftPMDependenciesExtension().testExecutionHooks)
        dumpTask.testExecutionService.set(locateOrRegisterSwiftPMDependenciesExtension().testExecutionService)
        dumpTask.resolvedPackagesState.from(
            fetchSyntheticImportProjectPackages.map { it.inputManifests },
            fetchSyntheticImportProjectPackages.map { it.syntheticLockFile },
        )
        dumpTask.xcodebuildPlatform.set(targetPlatform)
        dumpTask.xcodebuildSdk.set(targetSdk)
        dumpTask.swiftPMDependenciesCheckout.set(fetchSyntheticImportProjectPackages.map { it.swiftPMDependenciesCheckout.get() })
        dumpTask.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinteropsAndLdDump.map { it.syntheticImportProjectRoot.get() })
        dumpTask.localPackages.filesToTrackFromLocalPackages.set(computeLocalPackageDependencyInputFiles.map { it.filesToTrackFromLocalPackages.get() })
        dumpTask.ideaSyncEnabled.set(project.isInIdeaSync)
    }
}


internal const val PROJECT_PATH_ENV = "XCODEPROJ_PATH"
internal fun searchForGradlew(path: File?): File? {
    if (path == null) return null
    path.listFiles().firstOrNull { it.name == "gradlew" }?.let { return it }
    return searchForGradlew(path.parentFile)
}

internal fun Project.directSwiftPMDependencies(): Provider<Set<SwiftPMDependency>> {
    val swiftPMImportExtension = locateOrRegisterSwiftPMDependenciesExtension()
    return provider { swiftPMImportExtension.swiftPMDependencies }
}

internal fun Project.hasDirectOrTransitiveSwiftPMDependencies(): Provider<Boolean> {
    val hasDirectSwiftPMDependencies = directSwiftPMDependencies().map { it.isNotEmpty() }
    return transitiveSwiftPMMetadataProvider().map { transitiveDependencies ->
        hasDirectSwiftPMDependencies.get() || transitiveDependencies.metadataByDependencyIdentifier.values.any { it.dependencies.isNotEmpty() }
    }
}

internal fun Project.registerPackageGeneration(
    suffix: String,
    swiftPMImportExtension: SwiftPMImportExtension,
    syntheticImportProjectRoot: Provider<File>,
    syntheticImportProjectProductType: Provider<GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType>,
    transitiveSwiftPMMetadataProvider: Provider<TransitiveSwiftPMMetadata>,
): TaskProvider<GenerateSyntheticLinkageImportProject> {
    return registerTask<GenerateSyntheticLinkageImportProject>(
        lowerCamelCaseName(
            GenerateSyntheticLinkageImportProject.TASK_NAME,
            suffix,
        ),
    ) {
        it.transitiveSwiftPMMetadata.set(transitiveSwiftPMMetadataProvider)
        it.configureWithExtension(swiftPMImportExtension)
        it.syntheticImportProjectRoot.set(
            project.layout.dir(syntheticImportProjectRoot)
        )
        it.syntheticProductType.set(syntheticImportProjectProductType)
    }
}

internal const val SHARED_XCODE_DUMP_DIR = "build/kotlin/swiftPMXcodeDumps"
private fun Project.provideXcodeDumpsDir(): Provider<Directory> =
    layout.dir(
        provider {
            rootProject.projectDir.resolve(SHARED_XCODE_DUMP_DIR)
        }
    )

internal const val SHARED_SYNTHETIC_PACKAGE_DIR = "build/kotlin/swiftSyntheticPackages"
private fun Project.provideSyntheticPackageDir(): Provider<Directory> =
    layout.dir(
        provider {
            rootProject.projectDir.resolve(SHARED_SYNTHETIC_PACKAGE_DIR)
        }
    )

internal const val SHARED_CHECKOUT_DIR = "build/kotlin/swiftPMCheckouts"
private fun Project.provideCheckoutDir(): Provider<Directory> =
    layout.dir(
        provider {
            rootProject.projectDir.resolve(SHARED_CHECKOUT_DIR)
        }
    )

private fun Project.registerXcodeIntegrationLinkagePackageGeneration(
    swiftPMImportExtension: SwiftPMImportExtension,
    projectPathProvider: Provider<String>,
    syntheticImportProjectProductType: Provider<GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType>,
    transitiveSwiftPMMetadataProvider: Provider<TransitiveSwiftPMMetadata>,
): TaskProvider<GenerateSyntheticLinkageImportProject> = registerPackageGeneration(
    suffix = "forLinkageForCli",
    swiftPMImportExtension = swiftPMImportExtension,
    syntheticImportProjectRoot = projectPathProvider
        .flatMap { xcodeprojPath ->
            project.provider {
                File(xcodeprojPath).parentFile.resolve(SYNTHETIC_IMPORT_TARGET_MAGIC_NAME)
            }
        }
        .orElse(
            // Fallback so Gradle can configure the task graph without XCODEPROJ_PATH.
            // The integrate* tasks will surface an actionable error from their @TaskAction.
            project.layout.buildDirectory.dir("tmp/swiftImport-unconfigured/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME").getFile()
        ),
    syntheticImportProjectProductType = syntheticImportProjectProductType,
    transitiveSwiftPMMetadataProvider = transitiveSwiftPMMetadataProvider,
)

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

private fun Project.provideIdentifierCheckoutDir(identifier: String): Provider<Directory> {
    return providerIdentifierRoot(identifier).map { it.dir("swiftPMCheckout") }
}


internal fun Project.swiftPMImportIdeModelProvider(): Provider<SwiftPMImportIdeModel> =
    project.hasDirectOrTransitiveSwiftPMDependencies().map { hasDirectOrTransitiveSwiftPMDependencies ->
        SwiftPMImportIdeModel(
            hasDirectOrTransitiveSwiftPMDependencies,
            ("${project.path}:${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}").replace("::", ":"),
            SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
            project.directSwiftPMDependencies().map directSwiftPMDependencies@ { dependencies ->
                val declaredDependencies = dependencies.map {
                    when (it) {
                        is SwiftPMDependency.Local -> LocalSwiftPMDependencyForIde(it.absolutePath)
                        is SwiftPMDependency.Remote -> {
                            RemoteSwiftPMDependencyForIde(
                                when (val repository = it.repository) {
                                    is SwiftPMDependency.Remote.Repository.Id -> repository.value
                                    is SwiftPMDependency.Remote.Repository.Url -> repository.value
                                }
                            )
                        }
                    }
                }
                if (declaredDependencies.isEmpty()) return@directSwiftPMDependencies null

                val fetchTask = tasks.getByName(FetchSyntheticImportProjectPackages.TASK_NAME) as FetchSyntheticImportProjectPackages
                DeclaredSwiftPMDependencies(
                    dependencies = declaredDependencies,
                    checkoutPath = fetchTask.swiftPMDependenciesCheckout.getFile(),
                    swiftPackageResolveTaskPath = fetchTask.path,
                )
            }.orNull
        )
    }
