/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import java.io.File

@DisableCachingByDefault(because = "Validation task with no meaningful outputs")
internal abstract class ValidateLocalSwiftPMDependencies : DefaultTask(), UsesKotlinToolingDiagnostics {

    /**
     * All local SwiftPM dependencies to validate — both direct (declared in this project's
     * `swiftPMDependencies { }` block) and transitive (from consumed module metadata).
     */
    @get:Input
    abstract val localDependencies: SetProperty<SwiftPMDependency.Local>

    /**
     * The Gradle project directory, used to compute relative paths for diagnostic messages.
     */
    @get:Input
    abstract val projectDir: Property<File>

    @TaskAction
    internal fun validate() {
        val errors = mutableListOf<String>()
        for (dependency in localDependencies.get()) {
            val resolvedPath = dependency.absolutePath
            val originalPath = projectDir.get().toPath().relativize(resolvedPath.toPath()).toString()

            if (!resolvedPath.exists()) {
                reportDiagnostic(
                    KotlinToolingDiagnostics.SwiftPMLocalPackageDirectoryNotFound(
                        resolvedPath.absolutePath,
                        originalPath
                    )
                )
                errors += KotlinToolingDiagnostics.SwiftPMLocalPackageDirectoryNotFound.id
                continue
            }

            if (!resolvedPath.resolve("Package.swift").exists()) {
                reportDiagnostic(
                    KotlinToolingDiagnostics.SwiftPMLocalPackageMissingManifest(resolvedPath)
                )
                errors += KotlinToolingDiagnostics.SwiftPMLocalPackageMissingManifest.id
                continue
            }

            if (dependency.packageName.isBlank()) {
                reportDiagnostic(
                    KotlinToolingDiagnostics.SwiftPMLocalPackageInvalidName(originalPath)
                )
                errors += KotlinToolingDiagnostics.SwiftPMLocalPackageInvalidName.id
            }
        }

        if (errors.isNotEmpty()) {
            error(
                "Validation of local SwiftPM dependencies failed (${errors.joinToString()}). " +
                        "See diagnostics above for details."
            )
        }
    }

    companion object {
        const val TASK_NAME = "validateLocalSwiftPMDependencies"
    }
}
