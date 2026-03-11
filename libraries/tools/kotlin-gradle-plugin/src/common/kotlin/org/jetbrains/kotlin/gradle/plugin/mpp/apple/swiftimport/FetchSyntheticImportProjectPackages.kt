/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject
import org.gradle.api.provider.Property
import java.io.File


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
                it.include("**/Package.swift")
            }

    @get:Internal
    val swiftPMDependenciesCheckout: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftPMCheckout")
    )

    /**
     * Invalidate fetch when Package.swift or Package.resolved files changed.
     */
    @get:OutputFile
    val syntheticLockFile = syntheticImportProjectRoot.file("Package.resolved")

    /**
     * Optional SwiftPM repository cache override.
     * Passed to `xcodebuild` as:
     *   -packageCachePath <dir>
     * Used in tests to avoid collisions with the global cache
     * at `~/Library/Caches/org.swift.swiftpm/repositories`.
     */
    @get:Internal
    abstract val xcodePackageCacheDir: Property<File>


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
            /**
             * See KT-83863:
             * Avoid using `-onlyUsePackageVersionsFromResolvedFile`.
             * That flag forces SwiftPM to strictly use the versions from `Package.resolved`
             * and fail if the resolved file is considered out-of-date relative to the
             * current `Package.swift`. Because our synthetic `Package.swift` is regenerated,
             * SwiftPM may detect the lock file as stale and abort resolution instead of
             * reusing the locked versions.
             *
             * After changes in KT-83863:
             *`xcodebuild -resolvePackageDependencies` may reuse an existing `Package.resolved`
             * without materializing repositories in `-clonedSourcePackagesDirPath/checkouts`.
             * Therefore the checkout directory is not guaranteed to exist after the resolve
             * step and should not be relied on as a task postcondition.
             */
            val args = mutableListOf(
                "xcodebuild", "-resolvePackageDependencies",
                "-scheme", GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER, swiftPMDependenciesCheckout.get().asFile.path,
                "-derivedDataPath", swiftPMDependenciesCheckoutLogs.get().asFile.path,
            )

            if (xcodePackageCacheDir.isPresent) {
                args.add("-packageCachePath")
                args.add(xcodePackageCacheDir.get().path)
            }

            it.commandLine(args)
        }
    }

    companion object {
        const val TASK_NAME = "fetchSyntheticImportProjectPackages"
        const val XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER = "-clonedSourcePackagesDirPath"
    }
}