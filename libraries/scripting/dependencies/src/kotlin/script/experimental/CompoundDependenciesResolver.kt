/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic

class CompoundDependenciesResolver(private val resolvers: List<GenericDependenciesResolver>) : GenericDependenciesResolver() {

    constructor(vararg resolvers: GenericDependenciesResolver) : this(resolvers.toList())

    override fun acceptsArtifact(artifactCoordinates: String): Boolean {
        return resolvers.any { it.acceptsArtifact(artifactCoordinates) }
    }

    override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean {
        return resolvers.any { it.acceptsRepository(repositoryCoordinates) }
    }

    override fun addRepository(repositoryCoordinates: RepositoryCoordinates) {
        if (resolvers.count { it.tryAddRepository(repositoryCoordinates) } == 0)
            throw Exception("Failed to detect repository type: ${repositoryCoordinates}")
    }

    override suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<Iterable<File>> {

        val reports = mutableListOf<ScriptDiagnostic>()

        for (resolver in resolvers) {
            if (resolver.acceptsArtifact(artifactCoordinates)) {
                when (val resolveResult = resolver.resolve(artifactCoordinates)) {
                    is ResultWithDiagnostics.Failure -> reports.addAll(resolveResult.reports)
                    else -> return resolveResult
                }
            }
        }
        if (reports.count() == 0) return makeResolveFailureResult("No suitable dependency resolver found for artifact '$artifactCoordinates'")
        return ResultWithDiagnostics.Failure(reports)
    }

}