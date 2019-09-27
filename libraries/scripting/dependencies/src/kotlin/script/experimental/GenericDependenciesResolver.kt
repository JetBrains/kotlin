/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import org.jetbrains.kotlin.script.util.resolvers.experimental.BasicArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.BasicRepositoryCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryCoordinates
import java.io.File

interface GenericDependenciesResolver {
    fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean
    fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean

    fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResolveArtifactResult
    fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates)
}

data class ResolveAttemptFailure(val location: String, val message: String)

sealed class ResolveArtifactResult {
    data class Success(val files: Iterable<File>): ResolveArtifactResult()

    data class Failure(val attempts: List<ResolveAttemptFailure>): ResolveArtifactResult()
}

fun GenericDependenciesResolver.tryResolve(artifactCoordinates: GenericArtifactCoordinates): Iterable<File>? =
    if (accepts(artifactCoordinates)) resolve(artifactCoordinates).let {
        when (it) {
            is ResolveArtifactResult.Success -> it.files
            else -> null
        }
    } else null

fun GenericDependenciesResolver.tryAddRepository(repositoryCoordinates: GenericRepositoryCoordinates) =
    if (accepts(repositoryCoordinates)) {
        addRepository(repositoryCoordinates)
        true
    } else false

fun GenericDependenciesResolver.tryResolve(artifactCoordinates: String): Iterable<File>? =
    tryResolve(BasicArtifactCoordinates(artifactCoordinates))

fun GenericDependenciesResolver.tryAddRepository(repositoryCoordinates: String, id: String? = null): Boolean =
    tryAddRepository(BasicRepositoryCoordinates(repositoryCoordinates, id))