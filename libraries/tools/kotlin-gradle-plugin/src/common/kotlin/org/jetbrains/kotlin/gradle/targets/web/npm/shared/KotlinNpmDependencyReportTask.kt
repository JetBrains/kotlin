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
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.encodedVersionSpec
import java.util.*

/**
 * Writes the [NpmDependencyReport] of one JS or WasmJS compilation into [outputFile].
 */
@CacheableTask
abstract class KotlinNpmDependencyReportTask internal constructor() : DefaultTask() {

    @get:Input
    abstract val target: Property<String>

    @get:Nested
    internal abstract val compilation: CompilationNpmDependencies

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    protected fun generate() {
        val dependencies = resolveNpmDependencyEntries()

        val report = createNpmDependencyReport(dependencies, target.get(), compilation.moduleName.get())

        report.writeTo(outputFile.get().asFile.toPath())
    }

    private fun resolveNpmDependencyEntries(): Set<NpmDependencyEntry> {
        fun KotlinNpmDependency.toEntry() = NpmDependencyEntry(name, dependencyType, encodedVersionSpec().get())
        return buildSet {
            addAll(compilation.directUserDependencies.get().map { it.toEntry() })
            addAll(compilation.npmToolingDependencies.get().map { it.toEntry() })
            addAll(compilation.dependencyReports.files.flatMap { readNpmDependencyReport(it).npmDependencies() })
        }
    }

    /** `devDependencies` are not inherited. */
    private fun NpmDependencyReport.npmDependencies(): List<NpmDependencyEntry> {
        fun scope(dependencyType: KotlinNpmDependency.DependencyType, dependencies: Map<String, String>) =
            dependencies.map { (name, version) ->
                NpmDependencyEntry(name, dependencyType, version)
            }

        return buildList {
            addAll(scope(KotlinNpmDependency.DependencyType.NORMAL, dependencies))
            addAll(scope(KotlinNpmDependency.DependencyType.PEER, peerDependencies))
            addAll(scope(KotlinNpmDependency.DependencyType.OPTIONAL, optionalDependencies))
        }
    }
}

/** A [KotlinNpmDependency], with a fixed [versionSpec]. */
internal data class NpmDependencyEntry(
    val name: String,
    val dependencyType: KotlinNpmDependency.DependencyType,
    val versionSpec: String,
)

/** npm dependencies of one JS or WasmJS compilation. */
internal abstract class CompilationNpmDependencies {

    @get:Input
    abstract val moduleName: Property<String>

    /** Declared by the user with `npm`, `npmDev`, `npmOptional` and `npmPeer`. */
    @get:Nested
    abstract val directUserDependencies: SetProperty<KotlinNpmDependency>

    /** Required by this compilation's tasks, test framework, webpack, etc. */
    @get:Nested
    abstract val npmToolingDependencies: SetProperty<KotlinNpmDependency>

    /** Reports of runtime dependencies, built from klibs where a dependency has none. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val dependencyReports: ConfigurableFileCollection
}

/** Versions of one package are joined into one npm range, so npm validates them: KT-87942. */
internal fun createNpmDependencyReport(
    dependencies: Collection<NpmDependencyEntry>,
    target: String,
    compilation: String,
): NpmDependencyReport {
    val normal: SortedMap<String, SortedSet<String>> = sortedMapOf()
    val dev: SortedMap<String, SortedSet<String>> = sortedMapOf()
    val peer: SortedMap<String, SortedSet<String>> = sortedMapOf()
    val optional: SortedMap<String, SortedSet<String>> = sortedMapOf()

    dependencies.forEach { dependency ->
        val section = when (dependency.dependencyType) {
            KotlinNpmDependency.DependencyType.NORMAL -> normal
            KotlinNpmDependency.DependencyType.DEV -> dev
            KotlinNpmDependency.DependencyType.PEER -> peer
            KotlinNpmDependency.DependencyType.OPTIONAL -> optional
        }
        section.getOrPut(dependency.name) { sortedSetOf() } += dependency.versionSpec
    }

    fun SortedMap<String, SortedSet<String>>.joinVersions(): Map<String, String> =
        mapValues { (_, versions) -> mergeNpmVersions(versions) }

    return NpmDependencyReport(
        target = target,
        compilation = compilation,
        dependencies = normal.joinVersions(),
        devDependencies = dev.joinVersions(),
        peerDependencies = peer.joinVersions(),
        optionalDependencies = optional.joinVersions(),
    )
}

// TODO(KT-87942): improve version merging logic, eg. handle `||` and hyphen ranges, which change meaning when joined with a space.
private fun mergeNpmVersions(versions: Set<String>): String {
    return versions.joinToString(" ")
}
