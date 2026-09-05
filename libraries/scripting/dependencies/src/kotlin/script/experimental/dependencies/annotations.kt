/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.impl.SimpleExternalDependenciesResolverOptionsParser
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions

/**
 * A common annotation that could be used in a script to denote a dependency
 * The annotation could be processed by configuration refinement code and it's arguments passed to an ExternalDependenciesResolver
 * for resolving dependencies
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOn(vararg val artifactsCoordinates: String, val options: Array<String> = [])

/**
 * A common annotation that could be used in a script to denote a repository for an ExternalDependenciesResolver
 * The annotation could be processed by configuration refinement code and it's arguments passed to an ExternalDependenciesResolver
 * for configuring repositories
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Repository(vararg val repositoriesCoordinates: String, val options: Array<String> = [])

/**
 * An extension function that configures repositories and resolves artifacts denoted by the [Repository] and [DependsOn] annotations
 */
suspend fun ExternalDependenciesResolver.resolveFromScriptSourceAnnotations(
    annotations: Iterable<ScriptSourceAnnotation<*>>
): ResultWithDiagnostics<List<File>> =
    externalArtifactsFromScriptSourceAnnotations(annotations).onSuccess {
        resolveExternalArtifacts(it.artifactsDependencies, it.artifactsRepositories)
    }

data class ExternalArtifactsDeclarations(
    val artifactsDependencies: List<UnresolvedExternalArtifacts>,
    val artifactsRepositories: List<ExternalArtifactsRepository>,
)

fun externalArtifactsFromScriptSourceAnnotations(
    annotations: Iterable<ScriptSourceAnnotation<*>>
): ResultWithDiagnostics<ExternalArtifactsDeclarations> {
    val repositories = mutableListOf<ExternalArtifactsRepository>()
    val dependencies = mutableListOf<UnresolvedExternalArtifacts>()

    annotations.forEach { (val annotation, val locationWithId = location) ->
        when (annotation) {
            is Repository -> {
                val options = SimpleExternalDependenciesResolverOptionsParser
                    .parseToMap(*annotation.options, locationWithId = locationWithId)
                    .valueOr { return it }

                annotation.repositoriesCoordinates.mapTo(repositories) {
                    ExternalArtifactsRepository(it, options, locationWithId)
                }
            }
            is DependsOn -> {
                val options = SimpleExternalDependenciesResolverOptionsParser
                    .parseToMap(*annotation.options, locationWithId = locationWithId)
                    .valueOr { return it }

                dependencies.add(
                    UnresolvedExternalArtifacts(annotation.artifactsCoordinates.toList(), options, sourceCodeLocation = locationWithId)
                )
            }
            else -> return makeFailureResult("Unknown annotation ${annotation.javaClass}", locationWithId = locationWithId)
        }
    }

    return ExternalArtifactsDeclarations(dependencies, repositories).asSuccess()
}

suspend fun ExternalDependenciesResolver.resolveExternalArtifacts(
    dependencies: Iterable<UnresolvedExternalArtifacts>,
    repositories: Iterable<ExternalArtifactsRepository>,
): ResultWithDiagnostics<List<File>> {
    val reports = mutableListOf<ScriptDiagnostic>()

    for (repository in repositories.distinct()) {
        val added = addRepository(
            RepositoryCoordinates(repository.coordinates),
            makeExternalDependenciesResolverOptions(repository.options),
            repository.sourceCodeLocation
        ).also { reports.addAll(it.reports) }.valueOr { return it }

        if (!added)
            return reports + makeFailureResult(
                "Unrecognized repository coordinates: ${repository.coordinates}",
                locationWithId = repository.sourceCodeLocation
            )
    }

    return reports + dependencies
        .filter { it.artifacts.isNotEmpty() }
        .groupBy { it.options }
        .entries
        .flatMapSuccess { (val options = key, val group = value) ->
            resolve(
                group.flatMap { dependency ->
                    dependency.artifacts.map { ArtifactWithLocation(it, dependency.sourceCodeLocation) }
                },
                makeExternalDependenciesResolverOptions(options)
            )
        }
}

/**
 * An extension function that configures repositories and resolves artifacts denoted by the [Repository] and [DependsOn] annotations
 */
suspend fun ExternalDependenciesResolver.resolveFromAnnotations(
    annotations: Iterable<Annotation>
): ResultWithDiagnostics<List<File>> {
    val scriptSourceAnnotations = annotations.map { ScriptSourceAnnotation(it, null) }
    return resolveFromScriptSourceAnnotations(scriptSourceAnnotations)
}
