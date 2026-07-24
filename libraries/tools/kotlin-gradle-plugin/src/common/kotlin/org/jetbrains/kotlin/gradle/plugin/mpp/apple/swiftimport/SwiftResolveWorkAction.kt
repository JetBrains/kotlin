/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.utils.contentEqualsIgnoringLineEndings
import java.io.File
import javax.inject.Inject

internal interface SwiftResolveAwaitWorkParameters : WorkParameters {
    val syntheticPackageHash: Property<SwiftResolveBucketMapKey>
    val coordinationService: Property<SwiftImportFingerprintedCoordinationService>
    val sourcePackageResolvedFile: Property<File>
    val destinationPackageResolved: Property<File>
    val sourceWorkspaceStateFile: Property<File>
    val destinationWorkspaceStateFile: Property<File>
    val testExecutionService: Property<SwiftImportTestExecutionService>
    val ideaSyncEnabled: Property<Boolean>
    val errorFile: RegularFileProperty
}

internal abstract class SwiftResolveAwaitWorkAction @Inject constructor(val fs: FileSystemOperations) : WorkAction<SwiftResolveAwaitWorkParameters> {
    private val logger = Logging.getLogger(SwiftResolveAwaitWorkAction::class.java)

    override fun execute() {
        logger.info("Awaiting SwiftPM package resolution ${parameters.syntheticPackageHash.get()}")
        val errorFile = parameters.errorFile.get().asFile
        errorFile.delete()
        try {
            if (parameters.testExecutionService.isPresent) {
                parameters.testExecutionService.get().beforeSwiftResolveAwaitWorkerStarted()
            }
            parameters.coordinationService.get().awaitSwiftResolved(parameters.syntheticPackageHash.get())
            finalizeFetchTask(
                fs,
                parameters.sourcePackageResolvedFile.get(),
                parameters.destinationPackageResolved.get(),
                parameters.sourceWorkspaceStateFile.get(),
                parameters.destinationWorkspaceStateFile.get()
            )
        } catch (failure: Throwable) {
            if (parameters.ideaSyncEnabled.get()) {
                val errorText = "Warning: Failed to resolve SwiftPM packages : ${failure.message ?: ""}"
                logger.warn(errorText, failure)
                errorFile.writeText(errorText)
            } else {
                throw failure
            }
        }
    }
}

internal interface SwiftResolveWorkParameters : WorkParameters {
    val syntheticImportProjectRoot: RegularFileProperty
    val swiftPMDependenciesCheckout: RegularFileProperty
    val additionalSwiftPackageResolveArgs: ListProperty<String>
    val gitIgnoreCheckoutDir: Property<Boolean>
    val coordinationService: Property<SwiftImportFingerprintedCoordinationService>
    val syntheticPackageHash: Property<SwiftResolveBucketMapKey>
    val coordinationEnabled: Property<Boolean>
    val syntheticLockFile: RegularFileProperty
    val persistedPackageResolved: RegularFileProperty
    val workspaceStateJson: RegularFileProperty
    val ideaSyncEnabled: Property<Boolean>
    val errorFile: RegularFileProperty
    val testExecutionService: Property<SwiftImportTestExecutionService>
}


internal abstract class SwiftResolveWorkAction @Inject constructor(
    private val execOps: ExecOperations,
    private val fs: FileSystemOperations,
) : WorkAction<SwiftResolveWorkParameters> {

    private val logger = Logging.getLogger(SwiftResolveWorkAction::class.java)

    override fun execute() {
        logger.info("Starting SwiftPM package resolution ${parameters.syntheticPackageHash.orNull?.let { "(bucket ${it})" }}")
        val errorFile = parameters.errorFile.get().asFile
        if (parameters.coordinationEnabled.get()) {
            if (parameters.testExecutionService.isPresent) {
                parameters.testExecutionService.get().beforeSwiftResolveOwnerWorkerStarted()
            }
            parameters.coordinationService.get().markSwiftResolveStarted(parameters.syntheticPackageHash.get())
        }
        errorFile.delete()
        try {
            // Seed the synthetic project from the persisted lock, then resolve, then optionally write back.
            syncFromPersisted()
            doExecute()
            syncToPersisted()
            if (parameters.coordinationEnabled.get()) {
                finalizeFetchTask(
                    fs,
                    parameters.syntheticImportProjectRoot.get().asFile.resolve("Package.resolved"),
                    parameters.syntheticLockFile.get().asFile,
                    parameters.swiftPMDependenciesCheckout.get().asFile.resolve("workspace-state.json"),
                    parameters.workspaceStateJson.get().asFile
                )
                parameters.coordinationService.get()
                    .markSwiftResolveCompleted(parameters.syntheticPackageHash.get())
            }
        } catch (failure: Throwable) {
            if (parameters.coordinationEnabled.get()) {
                parameters.coordinationService.get()
                    .markSwiftResolveFailed(parameters.syntheticPackageHash.get(), failure)
            }

            if (parameters.ideaSyncEnabled.get()) {
                val errorText = "Warning: Failed to resolve SwiftPM packages : ${failure.message ?: ""}"
                logger.warn(errorText, failure)
                errorFile.writeText(errorText)
            } else {
                throw failure
            }
        }
    }

    /**
     * Sync Package.resolved before and after running `swift package resolve`.
     */
    private fun syncFromPersisted() {
        if (isUmbrellaFetch()) return

        val source = parameters.persistedPackageResolved.get().asFile
        val destination = when (isInFingerprintedSharedPackage()) {
            true -> {
                /**
                 * if in fingerprinted shared package, we need to sync umbrella Package.resolved to fingerprinted Package.resolved
                 */
                syntheticPackageResolvedFile()
            }
            false -> parameters.syntheticLockFile.get().asFile
        }

        syncSwiftLockFile(fs, source, destination)
    }

    private fun syncToPersisted() {
        if (isUmbrellaFetch() || isInFingerprintedSharedPackage()) return
        syncSwiftLockFile(fs, syntheticPackageResolvedFile(), parameters.persistedPackageResolved.get().asFile)
    }

    private fun isUmbrellaFetch(): Boolean = !parameters.persistedPackageResolved.isPresent

    private fun isInFingerprintedSharedPackage(): Boolean = parameters.coordinationEnabled.get()

    private fun syntheticPackageResolvedFile(): File =
        parameters.syntheticImportProjectRoot.get().asFile.resolve("Package.resolved")

    private fun doExecute() {
        execOps.exec { exec ->

            exec.workingDir(parameters.syntheticImportProjectRoot.get().asFile)

            val args = mutableListOf(
                "/usr/bin/swift",
                "package",
                "--scratch-path", parameters.swiftPMDependenciesCheckout.get().asFile,
                "resolve",
            )

            if (parameters.additionalSwiftPackageResolveArgs.isPresent) {
                args.addAll(parameters.additionalSwiftPackageResolveArgs.get())
            }

            val environmentToFilter = listOf("SDKROOT")
            environmentToFilter.forEach { key ->
                if (exec.environment.containsKey(key)) {
                    exec.environment.remove(key)
                }
            }

            exec.commandLine(args)
        }

        if (parameters.gitIgnoreCheckoutDir.get()) {
            writeCheckoutDirToGitIgnore()
        }
    }

    private fun writeCheckoutDirToGitIgnore() {
        val checkoutDir = parameters.swiftPMDependenciesCheckout.get().asFile
        val root = checkoutDir.parentFile
        val exclude = root.resolve(".gitignore")

        if (!exclude.exists()) {
            exclude.parentFile.mkdirs()
            exclude.createNewFile()
        }

        val entry = "${checkoutDir.name}/"

        exclude.writeText(entry)
    }
}

private fun hasSameContent(dest: File, src: File): Boolean = dest.exists() && src.exists() && contentEqualsIgnoringLineEndings(src, dest)


internal fun finalizeFetchTask(
    fs: FileSystemOperations,
    sourcePackageResolvedFile: File,
    destinationPackageResolved: File,
    sourceWorkspaceStateFile: File,
    destinationWorkspaceStateFile: File,
) {
    syncSwiftLockFile(
        fs,
        sourcePackageResolvedFile,
        destinationPackageResolved,
    )
    syncSwiftLockFile(
        fs,
        sourceWorkspaceStateFile,
        destinationWorkspaceStateFile
    )
}

internal fun syncSwiftLockFile(
    fs: FileSystemOperations,
    sourcePackageResolvedFile: File,
    destinationPackageResolved: File,
) {
    if (!sourcePackageResolvedFile.exists()) {
        if (destinationPackageResolved.exists()) destinationPackageResolved.delete()
        return
    }

    if (hasSameContent(sourcePackageResolvedFile, destinationPackageResolved)) return

    if (!destinationPackageResolved.parentFile.exists()) destinationPackageResolved.parentFile.mkdirs()

    fs.copy { spec ->
        spec.from(sourcePackageResolvedFile)
        spec.into(destinationPackageResolved.parentFile)
        spec.rename { destinationPackageResolved.name }
    }
}
