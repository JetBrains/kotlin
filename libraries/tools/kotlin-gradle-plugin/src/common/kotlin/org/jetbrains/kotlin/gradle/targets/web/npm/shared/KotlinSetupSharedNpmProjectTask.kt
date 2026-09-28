/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import java.io.File

/**
 * Assembles the shared-npm-project for one platform from the collected package.json files:
 * one workspace `package.json` per compilation plus the root `package.json` listing all workspaces.
 */
@CacheableTask
internal abstract class KotlinSetupSharedNpmProjectTask : DefaultTask(), UsesKotlinToolingDiagnostics {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val packageJsonFiles: ConfigurableFileCollection

    @get:Input
    abstract val rootPackageName: Property<String>

    @get:Input
    abstract val rootPackageVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun assemble() {
        val outputDirectory = outputDirectory.get().asFile
        val packagesDirectory = outputDirectory.resolve("packages")
        packagesDirectory.deleteRecursively()

        val workspaceNames = packageJsonFiles.files
            .mapNotNull { file ->
                file.packageName()?.also { name ->
                    file.copyTo(packagesDirectory.resolve(name).resolve("package.json"))
                }
            }
            .sorted()

        PackageJson(rootPackageName.get(), rootPackageVersion.get()).apply {
            private = true
            workspaces = workspaceNames.map { "packages/$it" }
        }.saveTo(outputDirectory.resolve("package.json"))
    }

    private fun File.packageName(): String? {
        val name = runCatching { fromSrcPackageJson(this)?.name }.getOrNull()
        if (name.isNullOrBlank()) {
            reportDiagnostic(KotlinToolingDiagnostics.SharedNpmProjectInvalidPackageJson(this))
            return null
        }
        return name
    }
}