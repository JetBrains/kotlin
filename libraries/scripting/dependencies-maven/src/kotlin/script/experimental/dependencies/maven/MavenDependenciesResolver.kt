/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies.maven

import com.jcabi.aether.Aether
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.DependencyResolutionException
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.artifact.JavaScopes
import java.io.File
import java.util.*
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.makeResolveFailureResult
import kotlin.script.experimental.dependencies.impl.toRepositoryUrlOrNull

val mavenCentral = RemoteRepository("maven-central", "default", "https://repo.maven.apache.org/maven2/")

class MavenRepositoryCoordinates(
    url: String,
    val username: String?,
    val password: String?,
    val privateKeyFile: String?,
    val passPhrase: String?
) : RepositoryCoordinates(url)

class MavenDependenciesResolver : ExternalDependenciesResolver {

    override fun acceptsArtifact(artifactCoordinates: String): Boolean =
        artifactCoordinates.toMavenArtifact() != null

    override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean {
        return repositoryCoordinates.toRepositoryUrlOrNull() != null
    }

    // TODO: make robust
    val localRepo = File(File(System.getProperty("user.home")!!, ".m2"), "repository")

    val repos: ArrayList<RemoteRepository> = arrayListOf()

    private fun remoteRepositories() = if (repos.isEmpty()) arrayListOf(mavenCentral) else repos

    private fun allRepositories() = remoteRepositories().map { it.url!!.toString() } + localRepo.toString()

    private fun String.toMavenArtifact(): DefaultArtifact? =
        if (this.isNotBlank() && this.count { it == ':' } == 2) DefaultArtifact(this)
        else null

    override suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<List<File>> {

        val artifactId = artifactCoordinates.toMavenArtifact()!!

        try {
            val deps = Aether(remoteRepositories(), localRepo).resolve(artifactId, JavaScopes.RUNTIME)
            if (deps != null)
                return ResultWithDiagnostics.Success(deps.map { it.file })
        } catch (e: DependencyResolutionException) {

        }
        return makeResolveFailureResult(allRepositories().map { "$it: $artifactId not found" })
    }

    private fun tryResolveEnvironmentVariable(str: String) =
        if (str.startsWith("$")) System.getenv(str.substring(1)) ?: str
        else str

    override fun addRepository(repositoryCoordinates: RepositoryCoordinates) {
        val url = repositoryCoordinates.toRepositoryUrlOrNull()
            ?: throw IllegalArgumentException("Invalid Maven repository URL: ${repositoryCoordinates}")
        val repo = RemoteRepository(
            repositoryCoordinates.string,
            "default",
            url.toString()
        )
        if (repositoryCoordinates is MavenRepositoryCoordinates) {
            val username = repositoryCoordinates.username?.let(::tryResolveEnvironmentVariable)
            val password = repositoryCoordinates.password?.let(::tryResolveEnvironmentVariable)
            repo.authentication = Authentication(username, password, repositoryCoordinates.privateKeyFile, repositoryCoordinates.passPhrase)
        }
        repos.add(repo)
    }
}