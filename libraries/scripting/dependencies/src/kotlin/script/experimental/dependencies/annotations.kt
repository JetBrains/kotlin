/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.filterByAnnotationType

/**
 * A common annotation that could be used in a script to denote a dependency
 * The annotation could be processed by configuration refinement code and it's arguments passed to an ExternalDependenciesResolver
 * for resolving dependencies
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOn(vararg val artifactsCoordinates: String)

/**
 * A common annotation that could be used in a script to denote a repository for an ExternalDependenciesResolver
 * The annotation could be processed by configuration refinement code and it's arguments passed to an ExternalDependenciesResolver
 * for configuring repositories
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Repository(vararg val repositoriesCoordinates: String)

/**
 * An extension function that configures repositories and resolves artifacts denoted by the [Repository] and [DependsOn] annotations
 */
suspend fun ExternalDependenciesResolver.resolveFromScriptSourceAnnotations(
    annotations: Iterable<ScriptSourceAnnotation<*>>
): ResultWithDiagnostics<List<File>> {
    val reports = mutableListOf<ScriptDiagnostic>()
    annotations.forEach { (annotation, locationWithId) ->
        when (annotation) {
            is Repository -> {
                for (coordinates in annotation.repositoriesCoordinates) {
                    val added = addRepository(coordinates, locationWithId)
                        .also { reports.addAll(it.reports) }
                        .valueOr { return it }

                    if (!added)
                        return reports + makeFailureResult(
                            "Unrecognized repository coordinates: $coordinates",
                            locationWithId = locationWithId
                        )
                }
            }
            is DependsOn -> {}
            else -> return reports + makeFailureResult("Unknown annotation ${annotation.javaClass}", locationWithId = locationWithId)
        }
    }

    return reports + annotations.filterByAnnotationType<DependsOn>()
        .flatMapSuccess { (annotation, locationWithId) ->
            annotation.artifactsCoordinates.asIterable().flatMapSuccess { artifactCoordinates ->
                resolve(artifactCoordinates, locationWithId)
            }
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