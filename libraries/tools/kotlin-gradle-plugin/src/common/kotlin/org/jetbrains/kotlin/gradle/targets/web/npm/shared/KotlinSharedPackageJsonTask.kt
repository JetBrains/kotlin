/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyScopeDeprecated
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import org.jetbrains.kotlin.gradle.targets.js.npm.packageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.disambiguateDependencies
import java.io.File
import java.util.zip.ZipFile

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
            val direct = compilation.directDependencies.get()
            val others = compilation.toolDependencies.get() + compilation.transitiveArtifacts.files.flatMap { it.collectNpmDependencies() }

            packageJson(
                name = moduleName,
                version = compilation.packageVersion.get(),
                main = compilation.main.get(),
                types = compilation.types.orNull,
                npmDependencies = disambiguateDependencies(
                    direct.map { it.toDeclaration() },
                    others.map { it.toDeclaration() },
                    logger,
                    moduleName,
                ),
                packageJsonHandlers = emptyList(),
            ).saveTo(outputDirectory.resolve(moduleName).resolve(NpmProject.PACKAGE_JSON))
        }
    }

    /** Either `package.json`, or a klib with its `package.json` inside. */
    private fun File.collectNpmDependencies(): Collection<KotlinNpmDependency> {
        val packageJsonText = when {
            name == NpmProject.PACKAGE_JSON -> readText()
            isCompatibleArchive -> ZipFile(this).use { zip ->
                zip.getEntry(NpmProject.PACKAGE_JSON)
                    ?.let { zip.getInputStream(it).reader().use { reader -> reader.readText() } }
            }
            else -> null
        } ?: return emptyList()

        return KgpJson.default.decodeFromString<PackageJsonDependencies>(packageJsonText).npmDependencies()
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

    /** Dependency projects' package.json and Maven klibs. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val transitiveArtifacts: ConfigurableFileCollection
}

/** `devDependencies` are not inherited. */
private fun PackageJsonDependencies.npmDependencies(): List<KotlinNpmDependency> {
    fun scope(scope: KotlinNpmDependency.Scope, dependencies: Map<String, String>) =
        dependencies.map { (name, version) -> DefaultKotlinNpmDependency(name, version, scope) }

    return scope(KotlinNpmDependency.Scope.NORMAL, dependencies) +
            scope(KotlinNpmDependency.Scope.PEER, peerDependencies) +
            scope(KotlinNpmDependency.Scope.OPTIONAL, optionalDependencies)
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
