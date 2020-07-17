/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies.maven

import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.repository.AuthenticationBuilder
import java.io.File
import java.util.*
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.makeResolveFailureResult
import kotlin.script.experimental.dependencies.impl.toRepositoryUrlOrNull
import kotlin.script.experimental.dependencies.maven.impl.AetherResolveSession
import kotlin.script.experimental.dependencies.maven.impl.mavenCentral
import kotlin.script.experimental.dependencies.impl.dependencyScopes


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

    private fun allRepositories() = remoteRepositories() + localRepo

    private fun String.toMavenArtifact(): DefaultArtifact? =
        if (this.isNotBlank() && this.count { it == ':' } >= 2) DefaultArtifact(this)
        else null

    override suspend fun resolve(
        artifactCoordinates: String,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?
    ): ResultWithDiagnostics<List<File>> {

        val artifactId = artifactCoordinates.toMavenArtifact()!!

        try {
            val dependencyScopes = options.dependencyScopes ?: listOf(JavaScopes.COMPILE, JavaScopes.RUNTIME)
            val deps = AetherResolveSession(
                localRepo, remoteRepositories()
            ).resolve(
                artifactId, dependencyScopes.joinToString(",")
            )
            if (deps != null)
                return ResultWithDiagnostics.Success(deps.map { it.file })
        } catch (e: DependencyResolutionException) {
            return makeResolveFailureResult(e.message ?: "unknown error", sourceCodeLocation)
        }
        return makeResolveFailureResult(allRepositories().map { "$it: $artifactId not found" }, sourceCodeLocation)
    }

    private fun tryResolveEnvironmentVariable(str: String) =
        if (str.startsWith("$")) System.getenv(str.substring(1)) ?: str
        else str

    override fun addRepository(
        repositoryCoordinates: RepositoryCoordinates,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?
    ): ResultWithDiagnostics<Boolean> {
        val url = repositoryCoordinates.toRepositoryUrlOrNull()
            ?: return false.asSuccess()
        val repo = RemoteRepository.Builder(
            repositoryCoordinates.string,
            "default",
            url.toString()
        )
        if (repositoryCoordinates is MavenRepositoryCoordinates) {
            val username = repositoryCoordinates.username?.let(::tryResolveEnvironmentVariable)
            val password = repositoryCoordinates.password?.let(::tryResolveEnvironmentVariable)
            if (username != null) {
                val auth = AuthenticationBuilder().apply {
                    addUsername(username)
                    if (password != null) {
                        addPassword(password)
                    }
                }
                repo.setAuthentication(auth.build())
            }
        }
        repos.add(repo.build())
        return true.asSuccess()
    }
}