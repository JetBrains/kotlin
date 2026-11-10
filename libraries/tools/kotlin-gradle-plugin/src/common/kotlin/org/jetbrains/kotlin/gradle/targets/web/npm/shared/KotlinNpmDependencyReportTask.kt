/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency

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
        val dependencies = compilation.directUserDependencies.get() +
                compilation.npmToolingDependencies.get() +
                compilation.dependencyReports.files.flatMap { readNpmDependencyReport(it).npmDependencies() }

        dependencies.toNpmDependencyReport(target.get(), compilation.moduleName.get())
            .writeTo(outputFile.get().asFile)
    }
}

/** npm dependencies of one JS or WasmJS compilation. */
internal abstract class CompilationNpmDependencies {

    @get:Input
    abstract val moduleName: Property<String>

    /** Declared by the user with `npm`, `npmDev`, `npmOptional` and `npmPeer`. */
    @get:Input
    abstract val directUserDependencies: SetProperty<DefaultKotlinNpmDependency>

    /** Required by this compilation's tasks, test framework, webpack, etc. */
    @get:Input
    abstract val npmToolingDependencies: SetProperty<DefaultKotlinNpmDependency>

    /** Reports of runtime dependencies, built from klibs where a dependency has none. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val dependencyReports: ConfigurableFileCollection
}

/** `devDependencies` are not inherited. */
private fun NpmDependencyReport.npmDependencies(): List<KotlinNpmDependency> {
    fun scope(scope: KotlinNpmDependency.Scope, dependencies: Map<String, String>) =
        dependencies.map { (name, version) -> DefaultKotlinNpmDependency(name, version, scope) }

    return scope(KotlinNpmDependency.Scope.NORMAL, dependencies) +
            scope(KotlinNpmDependency.Scope.PEER, peerDependencies) +
            scope(KotlinNpmDependency.Scope.OPTIONAL, optionalDependencies)
}

/** Versions of one package are joined into one npm range, so npm validates them: KT-87942. */
private fun Collection<KotlinNpmDependency>.toNpmDependencyReport(target: String, compilation: String): NpmDependencyReport {
    // TODO(KT-87942): improve version merging logic, eg. handle `||` and hyphen ranges, which change meaning when joined with a space.
    fun scope(scope: KotlinNpmDependency.Scope) = filter { it.scope == scope }
        .groupBy({ it.name }, { it.version })
        .mapValues { (_, versions) -> versions.distinct().sorted().joinToString(" ") }
        .toSortedMap()

    return NpmDependencyReport(
        target = target,
        compilation = compilation,
        dependencies = scope(KotlinNpmDependency.Scope.NORMAL),
        devDependencies = scope(KotlinNpmDependency.Scope.DEV),
        peerDependencies = scope(KotlinNpmDependency.Scope.PEER),
        optionalDependencies = scope(KotlinNpmDependency.Scope.OPTIONAL),
    )
}
