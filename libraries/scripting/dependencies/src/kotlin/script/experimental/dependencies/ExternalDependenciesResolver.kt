/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver.Options

open class RepositoryCoordinates(val string: String)

interface ExternalDependenciesResolver {
    interface Options {
        object Empty : Options {
            override fun value(name: String): String? = null
            override fun flag(name: String): Boolean? = null
        }

        fun value(name: String): String?
        fun flag(name: String): Boolean?
    }

    fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean
    fun acceptsArtifact(artifactCoordinates: String): Boolean

    suspend fun resolve(
        artifactCoordinates: String,
        options: Options = Options.Empty,
        sourceCodeLocation: SourceCode.LocationWithId? = null
    ): ResultWithDiagnostics<List<File>>

    fun addRepository(
        repositoryCoordinates: RepositoryCoordinates,
        options: Options = Options.Empty,
        sourceCodeLocation: SourceCode.LocationWithId? = null
    ): ResultWithDiagnostics<Boolean>
}

fun ExternalDependenciesResolver.acceptsRepository(repositoryCoordinates: String): Boolean =
    acceptsRepository(RepositoryCoordinates(repositoryCoordinates))

fun ExternalDependenciesResolver.addRepository(
    repositoryCoordinates: String,
    options: Options = Options.Empty,
    sourceCodeLocation: SourceCode.LocationWithId? = null
) = addRepository(RepositoryCoordinates(repositoryCoordinates), options, sourceCodeLocation)

