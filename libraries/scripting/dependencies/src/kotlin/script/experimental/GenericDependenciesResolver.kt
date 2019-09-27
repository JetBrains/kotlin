/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrNull

open class RepositoryCoordinates(val string: String)

abstract class GenericDependenciesResolver {

    abstract fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean
    abstract fun acceptsArtifact(artifactCoordinates: String): Boolean

    abstract suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<Iterable<File>>
    abstract fun addRepository(repositoryCoordinates: RepositoryCoordinates)

    protected fun makeResolveFailureResult(message: String) = makeResolveFailureResult(listOf(message))

    protected fun makeResolveFailureResult(messages: Iterable<String>) =
        ResultWithDiagnostics.Failure(messages.map { ScriptDiagnostic(it, ScriptDiagnostic.Severity.WARNING) })

    protected fun RepositoryCoordinates.toRepositoryUrlOrNull(): URL? =
        try {
            URL(string)
        } catch (_: MalformedURLException) {
            null
        }
}

fun GenericDependenciesResolver.acceptsRepository(repositoryCoordinates: String): Boolean =
    acceptsRepository(RepositoryCoordinates(repositoryCoordinates))

fun GenericDependenciesResolver.addRepository(repositoryCoordinates: String) = addRepository(RepositoryCoordinates(repositoryCoordinates))

suspend fun GenericDependenciesResolver.tryResolve(artifactCoordinates: String): Iterable<File>? =
    if (acceptsArtifact(artifactCoordinates)) resolve(artifactCoordinates).valueOrNull() else null

fun GenericDependenciesResolver.tryAddRepository(repositoryCoordinates: String) =
    tryAddRepository(RepositoryCoordinates(repositoryCoordinates))

fun GenericDependenciesResolver.tryAddRepository(repositoryCoordinates: RepositoryCoordinates) =
    if (acceptsRepository(repositoryCoordinates)) {
        addRepository(repositoryCoordinates)
        true
    } else false