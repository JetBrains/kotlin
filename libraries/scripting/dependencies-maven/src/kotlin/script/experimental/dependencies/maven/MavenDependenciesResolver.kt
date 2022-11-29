/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies.maven

import org.eclipse.aether.RepositoryException
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.repository.AuthenticationBuilder
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.*
import kotlin.script.experimental.dependencies.maven.impl.AetherResolveSession
import kotlin.script.experimental.dependencies.maven.impl.ResolutionKind
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
            val kind = when (options.partialResolution) {
                true -> ResolutionKind.TRANSITIVE_PARTIAL
                false, null -> when(options.transitive) {
                    true, null -> ResolutionKind.TRANSITIVE
                    false -> ResolutionKind.NON_TRANSITIVE
                }
            }
            val classifier = options.classifier
            val extension = options.extension
            AetherResolveSession(
                null, remoteRepositories()
            ).resolve(
                artifactId, dependencyScopes.joinToString(","), kind, null, classifier, extension
            )
        } catch (e: RepositoryException) {
            makeResolveFailureResult(e, sourceCodeLocation)
        }
    }

    private fun tryResolveEnvironmentVariable(
        str: String?,
        optionName: String,
        location: SourceCode.LocationWithId?
    ): ResultWithDiagnostics<String?> {
        if (str == null) return null.asSuccess()
        if (!str.startsWith("$")) return str.asSuccess()
        val envName = str.substring(1)
        val envValue: String? = System.getenv(envName)
        if (envValue.isNullOrEmpty()) return ResultWithDiagnostics.Failure(
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                "Environment variable `$envName` for $optionName is not set",
                ScriptDiagnostic.Severity.ERROR,
                location
            )
        )
        return envValue.asSuccess()
    }


    override fun addRepository(
        repositoryCoordinates: RepositoryCoordinates,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?
    ): ResultWithDiagnostics<Boolean> {
        val url = repositoryCoordinates.toRepositoryUrlOrNull()
            ?: return false.asSuccess()
        val repoId = repositoryCoordinates.string.replace(FORBIDDEN_CHARS, "_")

        @Suppress("DEPRECATION")
        val mavenRepo = repositoryCoordinates as? MavenRepositoryCoordinates
        val usernameRaw = options.username ?: mavenRepo?.username
        val passwordRaw = options.password ?: mavenRepo?.password

        val reports = mutableListOf<ScriptDiagnostic>()
        fun getFinalValue(optionName: String, rawValue: String?): String? {
            return tryResolveEnvironmentVariable(rawValue, optionName, sourceCodeLocation)
                .onFailure { reports.addAll(it.reports) }
                .valueOrNull()
        }

        val username = getFinalValue("username", usernameRaw)
        val password = getFinalValue("password", passwordRaw)
        val privateKeyFile = getFinalValue("private key file", options.privateKeyFile)
        val privateKeyPassphrase = getFinalValue("private key passphrase", options.privateKeyPassphrase)

        if (reports.isNotEmpty()) {
            return ResultWithDiagnostics.Failure(reports)
        }

        /**
         * Here we set all the authentication information we have, unconditionally.
         * Actual information that will be used (as well as lower-level checks,
         * such as nullability or emptiness) is determined by implementation.
         *
         * @see org.eclipse.aether.transport.wagon.WagonTransporter.getProxy
         * @see org.apache.maven.wagon.shared.http.AbstractHttpClientWagon.openConnectionInternal
         */
        val auth = AuthenticationBuilder()
            .addUsername(username)
            .addPassword(password)
            .addPrivateKey(
                privateKeyFile,
                privateKeyPassphrase
            )
            .build()

        val repo = RemoteRepository.Builder(repoId, "default", url.toString())
            .setAuthentication(auth)
            .build()

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

        private fun makeResolveFailureResult(
            exception: Throwable,
            location: SourceCode.LocationWithId?
        ): ResultWithDiagnostics.Failure {
            val allCauses = generateSequence(exception) { e: Throwable -> e.cause }.toList()
            val primaryCause = allCauses.firstOrNull { it is ArtifactResolutionException } ?: exception

            val message = buildString {
                append(primaryCause::class.simpleName)
                if (primaryCause.message != null) {
                    append(": ")
                    append(primaryCause.message)
                }
            }

            return makeResolveFailureResult(listOf(message), location, exception)
        }
    }
}