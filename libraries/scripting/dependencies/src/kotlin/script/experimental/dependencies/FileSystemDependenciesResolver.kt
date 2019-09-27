/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.dependencies.impl.makeResolveFailureResult
import kotlin.script.experimental.dependencies.impl.toRepositoryUrlOrNull

class FileSystemDependenciesResolver(vararg paths: File) : ExternalDependenciesResolver {

    private fun String.toRepositoryFileOrNull(): File? =
        File(this).takeIf { it.exists() && it.isDirectory }

    private fun RepositoryCoordinates.toFilePath() =
        (this.toRepositoryUrlOrNull()?.takeIf { it.protocol == "file" }?.path ?: string).toRepositoryFileOrNull()

    override fun addRepository(repositoryCoordinates: RepositoryCoordinates) {
        val repoDir = repositoryCoordinates.toFilePath()
            ?: throw IllegalArgumentException("Invalid repository location: '${repositoryCoordinates}'")
        localRepos.add(repoDir)
    }

    override suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<List<File>> {
        if (!acceptsArtifact(artifactCoordinates)) throw IllegalArgumentException("Path is invalid")

        val messages = mutableListOf<String>()

        for (repo in localRepos) {
            // TODO: add coordinates and wildcard matching
            val file = if (repo == null) File(artifactCoordinates) else File(repo, artifactCoordinates)
            when {
                !file.exists() -> messages.add("File '$file' does not exists")
                !file.isFile && !file.isDirectory -> messages.add("Path '$file' is neither file nor directory")
                else -> return ResultWithDiagnostics.Success(listOf(file))
            }
        }
        return makeResolveFailureResult(messages)
    }

    override fun acceptsArtifact(artifactCoordinates: String) =
        !artifactCoordinates.isBlank() // TODO: make check stronger, e.g. using NIO's Path

    override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean = repositoryCoordinates.toFilePath() != null

    private val localRepos = arrayListOf<File?>(null)

    init {
        for (path in paths) {
            require(path.exists() && path.isDirectory) { "Invalid flat lib directory repository path '$path'" }
        }
        localRepos.addAll(paths)
    }
}