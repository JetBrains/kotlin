/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
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
        artifactCoordinates: String,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?
    ): ResultWithDiagnostics<List<File>> {
        val reports = mutableListOf<ScriptDiagnostic>()

        for (resolver in resolvers) {
            if (resolver.acceptsArtifact(artifactCoordinates)) {
                when (val resolveResult = resolver.resolve(artifactCoordinates, options, sourceCodeLocation)) {
                    is ResultWithDiagnostics.Failure -> reports.addAll(resolveResult.reports)
                    else -> return resolveResult
                }
            }
        }
        return if (reports.count() == 0) {
            makeResolveFailureResult("No suitable dependency resolver found for artifact '$artifactCoordinates'", sourceCodeLocation)
        } else {
            ResultWithDiagnostics.Failure(reports)
        }
    }

}