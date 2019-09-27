/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrNull

open class RepositoryCoordinates(val string: String)

interface ExternalDependenciesResolver {

    fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean
    fun acceptsArtifact(artifactCoordinates: String): Boolean

    suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<List<File>>
    fun addRepository(repositoryCoordinates: RepositoryCoordinates)
}

fun ExternalDependenciesResolver.acceptsRepository(repositoryCoordinates: String): Boolean =
    acceptsRepository(RepositoryCoordinates(repositoryCoordinates))

fun ExternalDependenciesResolver.addRepository(repositoryCoordinates: String) = addRepository(RepositoryCoordinates(repositoryCoordinates))

suspend fun ExternalDependenciesResolver.tryResolve(artifactCoordinates: String): List<File>? =
    if (acceptsArtifact(artifactCoordinates)) resolve(artifactCoordinates).valueOrNull() else null

fun ExternalDependenciesResolver.tryAddRepository(repositoryCoordinates: String) =
    tryAddRepository(RepositoryCoordinates(repositoryCoordinates))

fun ExternalDependenciesResolver.tryAddRepository(repositoryCoordinates: RepositoryCoordinates) =
    if (acceptsRepository(repositoryCoordinates)) {
        addRepository(repositoryCoordinates)
        true
    } else false