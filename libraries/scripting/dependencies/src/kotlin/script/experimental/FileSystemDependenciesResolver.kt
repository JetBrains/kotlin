/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

class FileSystemDependenciesResolver(vararg paths: File) : GenericDependenciesResolver() {

    private fun String.toRepositoryFileOrNull(): File? =
        File(this).takeIf { it.exists() && it.isDirectory }

    private fun RepositoryCoordinates.toFilePath() =
        (this.toRepositoryUrlOrNull()?.takeIf { it.protocol == "file" }?.path ?: string).toRepositoryFileOrNull()

    override fun addRepository(repositoryCoordinates: RepositoryCoordinates) {
        val repoDir = repositoryCoordinates.toFilePath() ?: throw Exception("Invalid repository location: '${repositoryCoordinates}'")
        localRepos.add(repoDir)
    }

    override suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<Iterable<File>> {
        if (!acceptsArtifact(artifactCoordinates)) throw Exception("Path is invalid")

        val messages = mutableListOf<String>()

        val path = artifactCoordinates
        for (repo in localRepos) {
            // TODO: add coordinates and wildcard matching
            val file = if (repo == null) File(path) else File(repo, path)
            when {
                !file.exists() -> messages.add("File '${file.canonicalPath}' does not exists")
                !file.isFile && !file.isDirectory -> messages.add("Path '${file.canonicalPath}' is neither file nor directory")
                else -> return ResultWithDiagnostics.Success(listOf(file))
            }
        }
        return makeResolveFailureResult(messages)
    }

    override fun acceptsArtifact(artifactCoordinates: String) =
        !artifactCoordinates.isBlank() && artifactCoordinates.count { it == ':' } < 2

    override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean = repositoryCoordinates.toFilePath() != null

    private val localRepos = arrayListOf<File?>(null)

    init {
        for (path in paths) {
            require(path.exists() && path.isDirectory) { "Invalid flat lib directory repository path '$path'" }
        }
        localRepos.addAll(paths)
    }
}