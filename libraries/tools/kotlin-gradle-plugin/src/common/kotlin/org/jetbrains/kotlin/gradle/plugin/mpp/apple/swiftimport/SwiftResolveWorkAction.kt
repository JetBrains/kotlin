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
import java.io.File
import javax.inject.Inject

internal interface SwiftResolveAwaitWorkParameters : WorkParameters {
    val syntheticPackageHash: Property<SwiftResolveBucketMapKey>
    val coordinationService: Property<SwiftImportFingerprintedCoordinationService>
    val sourcePackageResolvedFile: Property<File>
    val destinationPackageResolved: Property<File>
    val sourceWorkspaceStateFile: Property<File>
    val destinationWorkspaceStateFile: Property<File>
}

internal abstract class SwiftResolveAwaitWorkAction @Inject constructor(val fs: FileSystemOperations) : WorkAction<SwiftResolveAwaitWorkParameters> {
    override fun execute() {
        parameters.coordinationService.get().awaitSwiftResolved(parameters.syntheticPackageHash.get())
        finalizeFetchTask(
            fs,
            parameters.sourcePackageResolvedFile.get(),
            parameters.destinationPackageResolved.get(),
            parameters.sourceWorkspaceStateFile.get(),
            parameters.destinationWorkspaceStateFile.get()
        )
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
    val workspaceStateJson: RegularFileProperty
    val ideaSyncEnabled: Property<Boolean>
    val errorFile: RegularFileProperty
}


internal abstract class SwiftResolveWorkAction @Inject constructor(
    private val execOps: ExecOperations,
    private val fs: FileSystemOperations,
) : WorkAction<SwiftResolveWorkParameters> {

    private val logger = Logging.getLogger(SwiftResolveWorkAction::class.java)

    override fun execute() {
        val errorFile = parameters.errorFile.get().asFile
        errorFile.delete()
        try {
            // Copy lock file from persisted
            // - In identifier this is .swiftpm-locks
            // - In None this is buildscript/Package.resolved

            doExecute()

            // Return lock file to the persisted location
            // - In None this is buildscript/Package.resolved
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
                return
            }
            throw failure
        }
    }

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

internal fun finalizeFetchTask(
    fs: FileSystemOperations,
    sourcePackageResolvedFile: File,
    destinationPackageResolved: File,
    sourceWorkspaceStateFile: File,
    destinationWorkspaceStateFile: File,
) {
    copySwiftLockFile(
        fs,
        sourcePackageResolvedFile,
        destinationPackageResolved,
    )
    copySwiftLockFile(
        fs,
        sourceWorkspaceStateFile,
        destinationWorkspaceStateFile
    )
}
