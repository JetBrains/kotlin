/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Action
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.Serializable

class KotlinCompilationNpmResolution(
    var internalDependencies: Collection<InternalDependency>,
    var internalCompositeDependencies: Collection<CompositeDependency>,
    var externalGradleDependencies: Collection<FileExternalGradleDependency>,
    var externalNpmDependencies: Collection<NpmDependencyDeclaration>,
    var fileCollectionDependencies: Collection<FileCollectionExternalGradleDependency>,
    val projectPath: String,
    val compilationDisambiguatedName: String,
    val npmProjectName: String,
    val npmProjectVersion: String,
    val tasksRequirements: TasksRequirements,
) : Serializable {

    val inputs: PackageJsonProducerInputs
        get() = PackageJsonProducerInputs(
            internalDependencies.map { it.projectName },
            externalGradleDependencies.map { it.file },
            externalNpmDependencies.map { it.uniqueRepresentation() },
            fileCollectionDependencies.flatMap { it.files }
        )

    private var closed = false
    internal var resolution: PreparedKotlinCompilationNpmResolution? = null

    @Synchronized
    fun prepareWithDependencies(
        npmResolutionManager: KotlinNpmResolutionManager,
        logger: Logger,
    ): PreparedKotlinCompilationNpmResolution {
        check(resolution == null) { "$this already resolved" }

        return createPreparedResolution(
            npmResolutionManager,
            logger
        ).also {
            resolution = it
        }
    }

    @Synchronized
    fun getResolutionOrPrepare(
        npmResolutionManager: KotlinNpmResolutionManager,
        logger: Logger,
    ): PreparedKotlinCompilationNpmResolution {

        return resolution ?: prepareWithDependencies(
            npmResolutionManager,
            logger
        )
    }

    @Synchronized
    fun close(
        npmResolutionManager: KotlinNpmResolutionManager,
        logger: Logger,
    ): PreparedKotlinCompilationNpmResolution {
        check(!closed) { "$this already closed" }
        closed = true
        return getResolutionOrPrepare(npmResolutionManager, logger)
    }

    fun createPreparedResolution(
        npmResolutionManager: KotlinNpmResolutionManager,
        logger: Logger,
    ): PreparedKotlinCompilationNpmResolution {
        val rootResolver = npmResolutionManager.parameters.resolution.get()

        val internalNpmDependencies = internalDependencies
            .map {
                val compilationNpmResolution: KotlinCompilationNpmResolution = rootResolver[it.projectPath][it.compilationName]
                compilationNpmResolution.getResolutionOrPrepare(
                    npmResolutionManager,
                    logger
                )
            }
            .flatMap { it.externalNpmDependencies }
        val importedExternalGradleDependencies = externalGradleDependencies.mapNotNull {
            npmResolutionManager.parameters.gradleNodeModulesProvider.get().get(it.dependencyName, it.dependencyVersion, it.file)
        } + fileCollectionDependencies.flatMap { dependency ->
            dependency.files
                // Gradle can hash with FileHasher only files and only existed files
                .filter { it.isFile }
                .map { file ->
                    npmResolutionManager.parameters.gradleNodeModulesProvider.get().get(
                        file.name,
                        dependency.dependencyVersion ?: "0.0.1",
                        file
                    )
                }
        }.filterNotNull()
        val transitiveNpmDependencies = (importedExternalGradleDependencies.flatMap {
            it.dependencies
        } + internalNpmDependencies).filter { it.scope != NpmDependency.Scope.DEV }

        val toolsNpmDependencies = tasksRequirements
            .getCompilationNpmRequirements(projectPath, compilationDisambiguatedName)

        val otherNpmDependencies = toolsNpmDependencies + transitiveNpmDependencies
        val allNpmDependencies = disambiguateDependencies(externalNpmDependencies, otherNpmDependencies, logger)

        return PreparedKotlinCompilationNpmResolution(
            npmResolutionManager.packagesDir.map { it.dir(npmProjectName) },
            importedExternalGradleDependencies,
            allNpmDependencies,
        )
    }

    fun createPackageJson(
        resolution: PreparedKotlinCompilationNpmResolution,
        npmProjectMain: Provider<String>,
        packageJsonHandlers: ListProperty<Action<PackageJson>>,
    ) {
        val packageJson = packageJson(
            npmProjectName,
            npmProjectVersion,
            npmProjectMain.get(),
            resolution.externalNpmDependencies,
            packageJsonHandlers.get()
        )

        packageJsonHandlers.get().forEach {
            it.execute(packageJson)
        }

        packageJson.saveTo(resolution.npmProjectDir.getFile().resolve(NpmProject.PACKAGE_JSON))
    }

    private fun disambiguateDependencies(
        direct: Collection<NpmDependencyDeclaration>,
        others: Collection<NpmDependencyDeclaration>,
        logger: Logger,
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
                                Transitive npm dependency version clash for compilation "${compilationDisambiguatedName}"
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
}