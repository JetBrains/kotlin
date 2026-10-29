/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
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
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import org.jetbrains.kotlin.gradle.targets.js.npm.packageJson
import java.io.File
import java.util.zip.ZipFile

/**
 * Writes the `package.json` of one JS or WasmJS compilation into [outputFile].
 */
@CacheableTask
internal abstract class KotlinNpmDependencyReportTask : DefaultTask() {

    @get:Nested
    abstract val compilation: CompilationNpmDependencies

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
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
        ).saveTo(outputFile.get().asFile)
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

/** npm dependencies of one JS or WasmJS compilation. */
internal abstract class CompilationNpmDependencies {

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
    // TODO KT-87942 to be removed
    @get:Input
    abstract val directDependencies: SetProperty<DefaultKotlinNpmDependency>

    /** Required by this compilation's tasks, test framework, webpack, etc. */
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

private fun disambiguateDependencies(
    direct: Collection<NpmDependencyDeclaration>,
    others: Collection<NpmDependencyDeclaration>,
    logger: Logger,
    compilationDisambiguatedName: String,
): Collection<NpmDependencyDeclaration> {
    val unique = others.groupBy(NpmDependencyDeclaration::name)
        .filterKeys { k -> direct.none { it.name == k } }
        .mapNotNull { (_, dependencies) ->
            dependencies.maxByOrNull { dep ->
                SemVer.from(dep.version, true)
            }?.also { selected ->
                if (dependencies.size > 1) {
                    logger.warn(
                        """
                        Transitive npm dependency version clash for compilation "$compilationDisambiguatedName"
                            Candidates:
                        ${dependencies.joinToString("\n") { "\t\t" + it.name + "@" + it.version }}
                            Selected:
                                ${selected.name}@${selected.version}
                        """.trimIndent()
                    )
                }
            }
        }
    return direct + unique
}
