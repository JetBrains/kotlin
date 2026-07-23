/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject
import org.gradle.api.tasks.Optional
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.utils.contentEqualsIgnoringLineEndings

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class FetchSyntheticImportProjectPackages : DefaultTask() {

    /**
     * Refetch when Package manifests of local SwiftPM dependencies change
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localPackageManifests: ConfigurableFileCollection

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty()

    /**
     * These are own manifest and manifests from project/modular dependencies. Refetch when any of these Package manifests changed.
     */
    // For some reason FileTree still invalidates on random directories without this annotation even though directories are not tracked...
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputManifests
        get() = syntheticImportProjectRoot
            .asFileTree
            .matching {
                // Don't traverse these for performance reasons
                it.exclude(".swiftpm")
                it.exclude(".build")

                it.include("**/Package.swift")
            }

    @get:Internal
    val swiftPMDependenciesCheckout: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftPMCheckout")
    )

    @get:OutputFile
    protected val workspaceStateJson = swiftPMDependenciesCheckout.map { checkoutDir ->
        checkoutDir.file("workspace-state.json")
    }

    @get:Input
    val gitIgnoreCheckoutDir : Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /**
     * When `true` (project is being imported by the IDE) a failure of `swift package resolve` is
     * downgraded to a warning instead of failing the whole IDE import. See KT-85468.
     */
    @get:Input
    val ideaSyncEnabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /**
     * Written only when resolve fails leniently during IDE sync, so the task is not considered
     * up-to-date and the next regular build retries. Mirrors CInteropProcess.errorFileProvider.
     */
    @get:OutputFile
    val ideImportError: Provider<RegularFile> = syntheticImportProjectRoot.file("swiftPMImportResolve_error.out")

    init {
        // KT-85468: a lenient failure during IDE sync writes an error file; while it exists the
        // task must not be up-to-date so the next build retries the resolve.
        testExecutionHooks.convention(SwiftImportExecutionHooks.NONE)
        outputs.upToDateWhen { !ideImportError.get().asFile.exists() }
    }

    /**
     * Invalidate fetch when Package.swift or Package.resolved files changed.
     */
    @get:OutputFile
    val syntheticLockFile = syntheticImportProjectRoot.file("Package.resolved")

    @get:Internal
    abstract val additionalSwiftPackageResolveArgs: ListProperty<String>

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val syntheticPackageFingerprint: RegularFileProperty

    @get:Internal
    abstract val coordinationService: Property<SwiftImportFingerprintedCoordinationService>

    @get:Internal
    abstract val testExecutionHooks: Property<SwiftImportExecutionHooks>

    @get:Internal
    abstract val testExecutionService: Property<SwiftImportTestExecutionService>

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @get:Internal
    abstract val persistedPackageResolved: RegularFileProperty

    @TaskAction
    fun fetchPackages() {
        val errorFile = ideImportError.get().asFile
        errorFile.delete()

        if (!syntheticPackageFingerprint.isPresent) {
            submitSwiftResolveWorkAction(
                ownerSyntheticImportProjectRoot = syntheticImportProjectRoot.get().asFile,
                ownerSwiftPMDependenciesCheckout = swiftPMDependenciesCheckout.get().asFile,
                syntheticPackageHash = null,
            )
            return
        }

        val ownerHash = syntheticPackageFingerprint.asFile.get().readText().trim().split("\n")[1]
        testExecutionHooks.get().beforeSwiftResolveClaim()
        val claim = coordinationService.get().claimOrJoinSwiftResolve(
            packageHash = ownerHash,
        )
        when (claim) {
            is CoordinationClaim.Existing -> {
                coordinationService.get().awaitSwiftResolveOwnerStarted(claim.bucket.key)
                workerExecutor.noIsolation().submit(SwiftResolveAwaitWorkAction::class.java) {
                    it.sourcePackageResolvedFile.set(claim.bucket.ownerPackageResolvedFile)
                    it.destinationPackageResolved.set(syntheticLockFile.get().asFile)
                    it.sourceWorkspaceStateFile.set(claim.bucket.ownerWorkspaceStateFile)
                    it.destinationWorkspaceStateFile.set(workspaceStateJson.get().asFile)
                    it.syntheticPackageHash.set(claim.bucket.key)
                    it.coordinationService.set(coordinationService)
                    it.testExecutionService.set(testExecutionService)
                    it.ideaSyncEnabled.set(ideaSyncEnabled)
                    it.errorFile.set(errorFile)
                }
            }

            is CoordinationClaim.Owner -> {
                testExecutionHooks.get().beforeSwiftResolveOwnerWorkerSubmission()
                runOwnerSwiftResolve(
                    claim.bucket.ownerSyntheticImportProjectRoot,
                    claim.bucket.ownerSwiftPMDependenciesCheckout,
                    claim.bucket.key,
                )
            }
        }

    }

    private fun runOwnerSwiftResolve(
        syntheticImportProjectRoot: File,
        swiftPMDependenciesCheckout: File,
        syntheticPackageHash: SwiftResolveBucketMapKey,
    ) {
        submitSwiftResolveWorkAction(
            ownerSyntheticImportProjectRoot = syntheticImportProjectRoot,
            ownerSwiftPMDependenciesCheckout = swiftPMDependenciesCheckout,
            syntheticPackageHash = syntheticPackageHash,
        )
    }

    fun submitSwiftResolveWorkAction(
        ownerSyntheticImportProjectRoot: File,
        ownerSwiftPMDependenciesCheckout: File,
        syntheticPackageHash: SwiftResolveBucketMapKey?,
    ) {
        val isCoordinationEnabled = syntheticPackageHash != null
        workerExecutor.noIsolation().submit(SwiftResolveWorkAction::class.java) { params ->
            params.syntheticImportProjectRoot.set(ownerSyntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(ownerSwiftPMDependenciesCheckout)
            params.additionalSwiftPackageResolveArgs.set(additionalSwiftPackageResolveArgs)
            params.gitIgnoreCheckoutDir.set(gitIgnoreCheckoutDir)
            params.coordinationEnabled.set(isCoordinationEnabled)
            params.ideaSyncEnabled.set(ideaSyncEnabled)
            params.errorFile.set(ideImportError)
            params.testExecutionService.set(testExecutionService)
            params.persistedPackageResolved.set(persistedPackageResolved)
            params.syntheticLockFile.set(syntheticLockFile)
            params.workspaceStateJson.set(workspaceStateJson)

            if (isCoordinationEnabled) {
                params.coordinationService.set(coordinationService)
                params.syntheticPackageHash.set(syntheticPackageHash!!)
            }
        }
    }


    companion object {
        const val TASK_NAME = "fetchSyntheticImportProjectPackages"
        fun fetchUmbrellaPackageTaskName(identifier: String) = lowerCamelCaseName(
            "fetchUmbrellaPackageIdentifierFor",
            identifier
        )

        const val XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER = "-clonedSourcePackagesDirPath"
    }
}
