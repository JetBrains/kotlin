/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fixSemver
import java.io.File

/**
 * Assembles the shared-npm-project for one platform from the collected npm dependency reports:
 * one workspace `package.json` per compilation plus the root `package.json` listing all workspaces.
 */
@CacheableTask
abstract class KotlinSetupSharedNpmProjectTask internal constructor() : DefaultTask(), UsesKotlinToolingDiagnostics {

    @get:InputFiles
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val reportFiles: ConfigurableFileCollection

    @get:Input
    abstract val rootPackageName: Property<String>

    @get:Input
    abstract val rootPackageVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    protected fun assemble() {
        val outputDirectory = outputDirectory.get().asFile
        val packagesDirectory = outputDirectory.resolve("packages")
        packagesDirectory.deleteRecursively()

        val reports = reportFiles.files.associateWith { file -> file.npmDependencyReportOrNull() }
        val invalidFiles = reports.filterValues { it == null }.keys.sorted()
        if (invalidFiles.isNotEmpty()) {
            reportDiagnostic(KotlinToolingDiagnostics.SharedNpmProjectInvalidNpmDependencyReport(invalidFiles))
            return
        }

        val workspaceVersion = fixSemver(rootPackageVersion.get())
        val workspaceNames = reports.values.filterNotNull()
            .map { report ->
                val name = report.compilation
                report.toPackageJson(workspaceVersion).saveTo(packagesDirectory.resolve(name).resolve(NpmProject.PACKAGE_JSON))
                name
            }
            .sorted()

        PackageJson(rootPackageName.get(), workspaceVersion).apply {
            private = true
            workspaces = workspaceNames.map { "packages/$it" }
        }.saveTo(outputDirectory.resolve(NpmProject.PACKAGE_JSON))
    }

    private fun File.npmDependencyReportOrNull(): NpmDependencyReport? =
        runCatching { readNpmDependencyReport(this) }.getOrNull()?.takeIf { it.compilation.isNotBlank() }
}

private fun NpmDependencyReport.toPackageJson(version: String) = PackageJson(compilation, version).also {
    it.dependencies.putAll(dependencies)
    it.devDependencies.putAll(devDependencies)
    it.peerDependencies.putAll(peerDependencies)
    it.optionalDependencies.putAll(optionalDependencies)
}
