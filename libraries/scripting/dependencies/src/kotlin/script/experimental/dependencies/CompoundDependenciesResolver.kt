/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.impl.makeResolveFailureResult

class CompoundDependenciesResolver(private val resolvers: List<ExternalDependenciesResolver>) : ExternalDependenciesResolver {

    constructor(vararg resolvers: ExternalDependenciesResolver) : this(resolvers.toList())

    override fun acceptsArtifact(artifactCoordinates: String): Boolean {
        return resolvers.any { it.acceptsArtifact(artifactCoordinates) }
    }

    override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean {
        return resolvers.any { it.acceptsRepository(repositoryCoordinates) }
    }

    override fun addRepository(
        repositoryCoordinates: RepositoryCoordinates,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?
    ): ResultWithDiagnostics<Boolean> {
        var success = false
        var repositoryAdded = false
        val reports = mutableListOf<ScriptDiagnostic>()

        for (resolver in resolvers) {
            if (resolver.acceptsRepository(repositoryCoordinates)) {
                when (val resolveResult = resolver.addRepository(repositoryCoordinates, options, sourceCodeLocation)) {
                    is ResultWithDiagnostics.Success -> {
                        success = true
                        repositoryAdded = repositoryAdded || resolveResult.value
                        reports.addAll(resolveResult.reports)
                    }
                    is ResultWithDiagnostics.Failure -> reports.addAll(resolveResult.reports)
                }
            }
        }

        return when {
            success -> repositoryAdded.asSuccess(reports)
            reports.isEmpty() -> makeResolveFailureResult(
                "No dependency resolver found that recognizes the repository coordinates '$repositoryCoordinates'",
                sourceCodeLocation
            )
            else -> ResultWithDiagnostics.Failure(reports)
        }
    }

    override suspend fun resolve(
        artifactsWithLocations: List<ArtifactWithLocation>,
        options: ExternalDependenciesResolver.Options
    ): ResultWithDiagnostics<List<File>> {
        val resultsCollector = IterableResultsCollector<File>()

        val artifactToResolverIndex = mutableMapOf<ArtifactWithLocation, Int>().apply {
            for (artifactWithLocation in artifactsWithLocations) {
                put(artifactWithLocation, -1)
            }
        }

        while (artifactToResolverIndex.isNotEmpty()) {
            val resolverGroups = mutableMapOf<Int, MutableList<ArtifactWithLocation>>()

            for ((artifactWithLocation, resolverIndex) in artifactToResolverIndex) {
                val (artifact, sourceCodeLocation) = artifactWithLocation

                var currentIndex = resolverIndex + 1
                while (currentIndex < resolvers.size) {
                    if (resolvers[currentIndex].acceptsArtifact(artifact)) break
                    ++currentIndex
                }
                if (currentIndex == resolvers.size) {
                    if (resolverIndex == -1) {
                        // We add this diagnostic only if there were no resolution attempts made
                        resultsCollector.addDiagnostic(
                            "No suitable dependency resolver found for artifact '$artifact'"
                                .asErrorDiagnostics(locationWithId = sourceCodeLocation)
                        )
                    }
                } else {
                    resolverGroups
                        .getOrPut(currentIndex) { mutableListOf() }
                        .add(artifactWithLocation)
                }
            }

            artifactToResolverIndex.clear()
            for ((resolverIndex, artifacts) in resolverGroups) {
                val resolver = resolvers[resolverIndex]
                val resolveResult = resolver.resolve(artifacts, options)
                resultsCollector.add(resolveResult)
                if (resolveResult.reports.isNotEmpty()) {
                    for (artifact in artifacts) {
                        artifactToResolverIndex[artifact] = resolverIndex
                    }
                }
            }
        }

        return resultsCollector.getResult()
    }

}
