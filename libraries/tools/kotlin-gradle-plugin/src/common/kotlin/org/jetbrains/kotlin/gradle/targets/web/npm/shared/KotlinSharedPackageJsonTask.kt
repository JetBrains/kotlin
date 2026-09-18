/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyScopeDeprecated
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.packageJson

/**
 * Writes one `package.json` per compilation of a JS or WasmJS target into [outputDirectory].
 */
@CacheableTask
internal abstract class KotlinSharedPackageJsonTask : DefaultTask() {

    @get:Nested
    abstract val compilations: ListProperty<CompilationPackageJsonInputs>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDirectory = outputDirectory.get().asFile

        compilations.get().forEach { compilation ->
            val moduleName = compilation.moduleName.get()
            val npmDependencies = compilation.directDependencies.get() + compilation.toolDependencies.get()

            packageJson(
                name = moduleName,
                version = compilation.packageVersion.get(),
                main = compilation.main.get(),
                types = compilation.types.orNull,
                npmDependencies = npmDependencies.map { it.toDeclaration() },
                packageJsonHandlers = emptyList(),
            ).saveTo(outputDirectory.resolve(moduleName).resolve(NpmProject.PACKAGE_JSON))
        }
    }
}

internal abstract class CompilationPackageJsonInputs {

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val packageVersion: Property<String>

    @get:Input
    abstract val main: Property<String>

    @get:Input
    @get:Optional
    abstract val types: Property<String>

    /** Declared by the user with `npm`, `npmDev`, `npmOptional` and `npmPeer`. */
    @get:Input
    abstract val directDependencies: SetProperty<DefaultKotlinNpmDependency>

    /** Required by this compilation's tasks, test framework, webpack. etc. */
    @get:Input
    abstract val toolDependencies: SetProperty<DefaultKotlinNpmDependency>
}

private fun KotlinNpmDependency.toDeclaration(): NpmDependencyDeclaration =
    NpmDependencyDeclaration(
        scope = when (scope) {
            KotlinNpmDependency.Scope.NORMAL -> NpmDependencyScopeDeprecated.NORMAL
            KotlinNpmDependency.Scope.DEV -> NpmDependencyScopeDeprecated.DEV
            KotlinNpmDependency.Scope.OPTIONAL -> NpmDependencyScopeDeprecated.OPTIONAL
            KotlinNpmDependency.Scope.PEER -> NpmDependencyScopeDeprecated.PEER
        },
        name = name,
        version = version,
    )
