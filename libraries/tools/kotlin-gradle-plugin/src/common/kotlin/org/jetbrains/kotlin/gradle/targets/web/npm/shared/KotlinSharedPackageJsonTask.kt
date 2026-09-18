/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyScopeDeprecated
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fakePackageJsonValue
import org.jetbrains.kotlin.gradle.targets.js.npm.packageJson

/**
 * Writes one `package.json` per compilation of a JS or WasmJS target into [outputDirectory].
 * Cacheable because every input is a plain value: no project model, no build service.
 */
@CacheableTask
internal abstract class KotlinSharedPackageJsonTask : DefaultTask() {

    /** One entry per compilation; `@Nested` fingerprints each entry's own annotated properties. */
    @get:Nested
    abstract val compilationsDependencies: ListProperty<CompilationNpmDependencies>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDirectory = outputDirectory.get().asFile

        compilationsDependencies.get().forEach { compilation ->
            val moduleName = compilation.moduleName.get()
            val npmDependencies = compilation.directDependencies.get() + compilation.toolDependencies.get()

            packageJson(
                name = moduleName,
                version = compilation.packageVersion.get(),
                main = compilation.main.get(),
                types = compilation.types.orNull,
                npmDependencies = npmDependencies.map { it.toDeclaration() },
                packageJsonHandlers = compilation.packageJsonHandlers.get(),
            ).saveTo(outputDirectory.resolve(moduleName).resolve(NpmProject.PACKAGE_JSON))
        }
    }
}

/** The npm dependency inputs of one compilation, filled in `setupSharedNpmSubproject.kt`. */
internal abstract class CompilationNpmDependencies {

    /** `lib-b` for the main compilation, `lib-b-test` for the test one; also the output folder name. */
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

    /** Required by this compilation's tasks: test framework, webpack. Always DEV scoped. */
    @get:Input
    abstract val toolDependencies: SetProperty<DefaultKotlinNpmDependency>


    /** Lambdas cannot be fingerprinted; [packageJsonInputHandlers] carries the up-to-date check instead. */
    @get:Internal
    abstract val packageJsonHandlers: ListProperty<Action<PackageJson>>

    /** Runs the handlers against a dummy [PackageJson] so a change inside one re-runs the task. */
    @Suppress("unused")
    @get:Input
    val packageJsonInputHandlers: Provider<PackageJson>
        get() = packageJsonHandlers.map { handlers ->
            PackageJson(fakePackageJsonValue, fakePackageJsonValue).apply {
                handlers.forEach { it.execute(this) }
            }
        }
}

/** `packageJson` still takes the deprecated declaration type; this is the only conversion to it. */
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
