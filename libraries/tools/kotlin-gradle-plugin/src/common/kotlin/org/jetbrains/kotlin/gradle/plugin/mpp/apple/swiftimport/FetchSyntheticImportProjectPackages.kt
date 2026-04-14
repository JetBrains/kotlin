/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
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
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.Serializable


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

    /**
     * Invalidate fetch when Package.swift or Package.resolved files changed.
     */
    @get:OutputFile
    val syntheticLockFile = syntheticImportProjectRoot.file("Package.resolved")

    @get:Internal
    abstract val additionalSwiftPackageResolveArgs: ListProperty<String>

    @get:Inject
    protected abstract val execOps: ExecOperations

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        checkoutSwiftPMDependencies()
    }

    private fun swiftpmResolve() {
        execOps.exec { exec ->
            val packagePath = syntheticImportProjectRoot.get().asFile
            val scratchPath = swiftPMDependenciesCheckout.get().asFile

            exec.workingDir(packagePath)

            val args = mutableListOf(
                "/usr/bin/swift",
                "package",
                "--package-path", packagePath.path,
                "--scratch-path", scratchPath.path,
                "resolve",
            )

            if (additionalSwiftPackageResolveArgs.isPresent) {
                args.addAll(additionalSwiftPackageResolveArgs.get())
            }

            val environmentToFilter = listOf("SDKROOT")
            environmentToFilter.forEach { key ->
                if (exec.environment.containsKey(key)) {
                    exec.environment.remove(key)
                }
            }

            exec.commandLine(args)
        }
    }

    private fun checkoutSwiftPMDependencies() {
        swiftpmResolve()
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
