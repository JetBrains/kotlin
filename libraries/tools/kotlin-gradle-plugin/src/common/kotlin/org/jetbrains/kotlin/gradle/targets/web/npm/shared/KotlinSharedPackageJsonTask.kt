/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import com.google.gson.Gson
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyScopeDeprecated
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.targets.js.npm.fakePackageJsonValue
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import org.jetbrains.kotlin.gradle.targets.js.npm.packageJson
import java.io.File
import java.util.zip.ZipFile

/**
 * Writes one `package.json` per compilation of a JS or WasmJS target into [outputDirectory]. Cacheable because every
 * input is a value or a file: no project model, no build service.
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
            val direct = compilation.directDependencies.get()
            val others = compilation.toolDependencies.get() + compilation.transitiveArtifacts.files.flatMap { it.npmDependencies() }

            packageJson(
                name = moduleName,
                version = compilation.packageVersion.get(),
                main = compilation.main.get(),
                types = compilation.types.orNull,
                npmDependencies = disambiguateDependencies(moduleName, direct, others, logger).map { it.toDeclaration() },
                packageJsonHandlers = compilation.packageJsonHandlers.get(),
            ).saveTo(outputDirectory.resolve(moduleName).resolve(NpmProject.PACKAGE_JSON))
        }
    }

    /** Either another project's `package.json`, or a Maven library's klib with its `package.json` inside. */
    private fun File.npmDependencies(): Collection<KotlinNpmDependency> {
        val packageJson = when {
            name == NpmProject.PACKAGE_JSON -> fromSrcPackageJson(this)
            isCompatibleArchive -> ZipFile(this).use { zip ->
                zip.getEntry(NpmProject.PACKAGE_JSON)
                    ?.let { zip.getInputStream(it).reader().use { reader -> Gson().fromJson(reader, PackageJson::class.java) } }
            }
            else -> null
        } ?: return emptyList()

        return packageJson.npmDependencies()
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

    /** Dependency projects' package.json and Maven klibs; only the content matters, hence [PathSensitivity.NONE]. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val transitiveArtifacts: ConfigurableFileCollection

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

/**
 * Declared dependencies win as declared; among the rest the highest version per name wins, like
 * `KotlinCompilationNpmResolution.disambiguateDependencies`. Two direct declarations of one name are left to
 * `packageJson`, which intersects their ranges or fails.
 */
private fun disambiguateDependencies(
    moduleName: String,
    direct: Collection<KotlinNpmDependency>,
    others: Collection<KotlinNpmDependency>,
    logger: Logger,
): Collection<KotlinNpmDependency> {
    val unique = others.groupBy(KotlinNpmDependency::name)
        .filterKeys { name -> direct.none { it.name == name } }
        .mapNotNull { (_, dependencies) ->
            dependencies.maxByOrNull { SemVer.from(it.version, true) }?.also { selected ->
                if (dependencies.size > 1) {
                    logger.warn(
                        """
                        Transitive npm dependency version clash for "$moduleName"
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

/** What a consumer inherits; `devDependencies` are the producer's own tooling and the legacy resolver drops them too. */
private fun PackageJson.npmDependencies(): List<KotlinNpmDependency> {
    fun scope(scope: KotlinNpmDependency.Scope, dependencies: Map<String, String>) =
        dependencies.map { (name, version) -> DefaultKotlinNpmDependency(name, version, scope) }

    return scope(KotlinNpmDependency.Scope.NORMAL, dependencies) +
            scope(KotlinNpmDependency.Scope.PEER, peerDependencies) +
            scope(KotlinNpmDependency.Scope.OPTIONAL, optionalDependencies)
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
