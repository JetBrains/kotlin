/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import org.jetbrains.kotlin.script.util.Repository
import org.jetbrains.kotlin.script.util.resolvers.experimental.BasicRepositoryCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryCoordinates
import java.io.File
import java.lang.Exception

class FlatLibDirectoryDependenciesResolver(vararg paths: File) : GenericDependenciesResolver {

    override fun addRepository(repositoryCoordinates: GenericRepositoryCoordinates) {
        val repoDir = repositoryCoordinates.file ?: throw Exception("Invalid repository location: '${repositoryCoordinates.string}'")
        localRepos.add(repoDir)
    }

    override fun resolve(artifactCoordinates: GenericArtifactCoordinates): ResolveArtifactResult {
        if(!accepts(artifactCoordinates)) throw Exception("Path is empty")

        val resolveAttempts = mutableListOf<ResolveAttemptFailure>()

        val path = artifactCoordinates.string
        for (repo in localRepos) {
            // TODO: add coordinates and wildcard matching
            val file = File(repo, path)
            when {
                !file.exists() -> resolveAttempts.add(ResolveAttemptFailure(file.canonicalPath, "File not exists"))
                !file.isFile && !file.isDirectory -> resolveAttempts.add(ResolveAttemptFailure(file.canonicalPath, "Path is neither file nor directory"))
                else -> return ResolveArtifactResult.Success(listOf(file))
            }
        }
        return ResolveArtifactResult.Failure(resolveAttempts)
    }

    override fun accepts(artifactCoordinates: GenericArtifactCoordinates): Boolean {
        return artifactCoordinates.string.takeUnless(String::isBlank)?.let { path ->
            localRepos.any { File(it, path).exists() }
        } ?: false
    }

    override fun accepts(repositoryCoordinates: GenericRepositoryCoordinates): Boolean = repositoryCoordinates.file != null

    private val localRepos = arrayListOf<File>()

    init {
        for (path in paths) {
            if (!path.exists() || !path.isDirectory) throw IllegalArgumentException("Invalid flat lib directory repository path '$path'")
        }
        localRepos.addAll(paths)
    }

    companion object {

        fun tryCreate(annotation: Repository): FlatLibDirectoryDependenciesResolver? = tryCreate(
            BasicRepositoryCoordinates(
                annotation.url.takeUnless(String::isBlank) ?: annotation.value, annotation.id.takeUnless(String::isBlank)
            )
        )

        fun tryCreate(repositoryCoordinates: GenericRepositoryCoordinates): FlatLibDirectoryDependenciesResolver? =
            repositoryCoordinates.file?.let { FlatLibDirectoryDependenciesResolver(it) }
    }
}