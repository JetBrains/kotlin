/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import java.io.File
import java.util.zip.ZipFile

/**
 * Writes the [NpmDependencyReport] of one JS or WasmJS compilation into [outputFile].
 */
@CacheableTask
internal abstract class KotlinNpmDependencyReportTask : DefaultTask() {

    @get:Input
    abstract val target: Property<String>

    @get:Nested
    abstract val compilation: CompilationNpmDependencies

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val dependencies = compilation.directDependencies.get() +
                compilation.toolDependencies.get() +
                compilation.transitiveArtifacts.files.flatMap { it.collectNpmDependencies() }

        NpmDependencyReport(target.get(), mapOf(compilation.moduleName.get() to dependencies.toCompilationReport()))
            .writeTo(outputFile.get().asFile)
    }

    /** A dependency's report, its `package.json`, or a klib with `package.json` inside. */
    private fun File.collectNpmDependencies(): Collection<KotlinNpmDependency> {
        if (name == NPM_DEPENDENCY_REPORT_FILE_NAME) {
            return readNpmDependencyReport(this).compilations.values.flatMap { it.npmDependencies() }
        }
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

/** npm dependencies of one JS or WasmJS compilation. */
internal abstract class CompilationNpmDependencies {

    @get:Input
    abstract val moduleName: Property<String>

    /** Declared by the user with `npm`, `npmDev`, `npmOptional` and `npmPeer`. */
    // TODO KT-87942 to be removed
    @get:Input
    abstract val directDependencies: SetProperty<DefaultKotlinNpmDependency>

    /** Required by this compilation's tasks, test framework, webpack, etc. */
    @get:Input
    abstract val toolDependencies: SetProperty<DefaultKotlinNpmDependency>

    /** Dependency projects' reports and Maven klibs. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val transitiveArtifacts: ConfigurableFileCollection
}

/** `devDependencies` are not inherited. */
private fun PackageJsonDependencies.npmDependencies(): List<KotlinNpmDependency> =
    inheritedNpmDependencies(dependencies, peerDependencies, optionalDependencies)

private fun CompilationNpmDependencyReport.npmDependencies(): List<KotlinNpmDependency> =
    inheritedNpmDependencies(dependencies, peerDependencies, optionalDependencies)

private fun inheritedNpmDependencies(
    dependencies: Map<String, String>,
    peerDependencies: Map<String, String>,
    optionalDependencies: Map<String, String>,
): List<KotlinNpmDependency> {
    fun scope(scope: KotlinNpmDependency.Scope, dependencies: Map<String, String>) =
        dependencies.map { (name, version) -> DefaultKotlinNpmDependency(name, version, scope) }

    return scope(KotlinNpmDependency.Scope.NORMAL, dependencies) +
            scope(KotlinNpmDependency.Scope.PEER, peerDependencies) +
            scope(KotlinNpmDependency.Scope.OPTIONAL, optionalDependencies)
}

/** Versions of one package are joined into one npm range, so npm validates them: KT-87942. */
private fun Collection<KotlinNpmDependency>.toCompilationReport(): CompilationNpmDependencyReport {
    fun scope(scope: KotlinNpmDependency.Scope) = filter { it.scope == scope }
        .groupBy({ it.name }, { it.version })
        .mapValues { (_, versions) -> versions.distinct().sorted().joinToString(" ") }
        .toSortedMap()

    return CompilationNpmDependencyReport(
        dependencies = scope(KotlinNpmDependency.Scope.NORMAL),
        devDependencies = scope(KotlinNpmDependency.Scope.DEV),
        peerDependencies = scope(KotlinNpmDependency.Scope.PEER),
        optionalDependencies = scope(KotlinNpmDependency.Scope.OPTIONAL),
    )
}
