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
import kotlin.script.experimental.dependencies.impl.*
import kotlin.script.experimental.dependencies.maven.impl.AetherResolveSession
import kotlin.script.experimental.dependencies.maven.impl.mavenCentral

@Deprecated(
    "This class is not functional and left only for compatibility reasons. Use kotlin.script.experimental.dependencies.ExternalDependenciesResolver.Options for passing authorization options",
    replaceWith = ReplaceWith("RepositoryCoordinates(url)", "kotlin.script.experimental.dependencies.RepositoryCoordinates")
)
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

    private fun String.toMavenArtifact(): DefaultArtifact? =
        if (this.isNotBlank() && this.count { it == ':' } >= 2) DefaultArtifact(this)
        else null

    override suspend fun resolve(
        artifactCoordinates: String,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?
    ): ResultWithDiagnostics<List<File>> {

        val artifactId = artifactCoordinates.toMavenArtifact()!!

        return try {
            val dependencyScopes = options.dependencyScopes ?: listOf(JavaScopes.COMPILE, JavaScopes.RUNTIME)
            val transitive = options.transitive ?: true
            val deps = AetherResolveSession(
                localRepo, remoteRepositories()
            ).resolve(
                artifactId, dependencyScopes.joinToString(","), transitive, null
            )
            ResultWithDiagnostics.Success(deps.map { it.file })
        } catch (e: DependencyResolutionException) {
            makeResolveFailureResult(e.message ?: "unknown error", sourceCodeLocation)
        }
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
        val repoId = repositoryCoordinates.string.replace(FORBIDDEN_CHARS, "_")
        val repo = RemoteRepository.Builder(repoId, "default", url.toString()).apply {
            /**
             * Here we set all the authentication information we have, unconditionally.
             * Actual information that will be used (as well as lower-level checks,
             * such as nullability or emptiness) is determined by implementation.
             *
             * @see org.eclipse.aether.transport.wagon.WagonTransporter.getProxy
             * @see org.apache.maven.wagon.shared.http.AbstractHttpClientWagon.openConnectionInternal
             */
            setAuthentication(
                AuthenticationBuilder().apply {
                    val mavenRepo = repositoryCoordinates as? MavenRepositoryCoordinates
                    val username = options.username ?: mavenRepo?.username
                    val password = options.password ?: mavenRepo?.password
                    addUsername(username?.let(::tryResolveEnvironmentVariable))
                    addPassword(password?.let(::tryResolveEnvironmentVariable))
                    addPrivateKey(
                        options.privateKeyFile?.let(::tryResolveEnvironmentVariable),
                        options.privateKeyPassphrase?.let(::tryResolveEnvironmentVariable)
                    )
                }.build()
            )
        }.build()

        repos.add(repo)
        return true.asSuccess()
    }

    companion object {
        /**
         * These characters are forbidden in Windows, Linux or Mac file names.
         * As the repository ID is used in metadata filename generation
         * (see [org.eclipse.aether.internal.impl.SimpleLocalRepositoryManager.getRepositoryKey]),
         * they should be replaced with an allowed character.
         */
        private val FORBIDDEN_CHARS = Regex("[/\\\\:<>\"|?*]")
    }
}