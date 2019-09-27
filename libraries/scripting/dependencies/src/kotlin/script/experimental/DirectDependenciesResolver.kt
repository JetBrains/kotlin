/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryCoordinates
import java.io.File
import java.lang.Exception

class DirectDependenciesResolver : GenericDependenciesResolver {

    private fun makeResolveFailureResult(location: String, message: String) = ResolveArtifactResult.Failure(listOf(ResolveAttemptFailure(location, message)))

    override fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResolveArtifactResult {
        if(!accepts(artifactCoordinates)) throw IllegalArgumentException("Invalid arguments: $artifactCoordinates")
        val file = File(artifactCoordinates.string)
        if(!file.exists()) return makeResolveFailureResult(file.canonicalPath, "File doesn't exist")
        if(!file.isFile && !file.isDirectory) return makeResolveFailureResult(file.canonicalPath, "Path is neither file nor directory")
        return ResolveArtifactResult.Success(listOf(file))
    }

    override fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates) =
        throw Exception("DirectDependenciesResolver doesn't support adding repositories")

    override fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean = false

    override fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean =
        !artifactCoordinates.string.isBlank() && !artifactCoordinates.string.contains(':')
}