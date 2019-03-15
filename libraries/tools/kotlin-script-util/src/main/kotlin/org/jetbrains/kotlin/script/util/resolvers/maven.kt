/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:DependsOn("org.funktionale:funktionale:0.9.6")

package org.jetbrains.kotlin.script.util.resolvers

import com.jcabi.aether.Aether
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryWithBridge
import org.jetbrains.kotlin.script.util.resolvers.experimental.MavenArtifactCoordinates
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.DependencyResolutionException
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.artifact.JavaScopes
import java.io.File
import java.util.*

val mavenCentral = RemoteRepository("maven-central", "default", "https://repo.maven.apache.org/maven2/")

class MavenResolver(val reportError: ((String) -> Unit)? = null): GenericRepositoryWithBridge {

    // TODO: make robust
    val localRepo = File(File(System.getProperty("user.home")!!, ".m2"), "repository")

    val repos: ArrayList<RemoteRepository> = arrayListOf()

    private fun currentRepos() = if (repos.isEmpty()) arrayListOf(mavenCentral) else repos

    private fun String?.isValidParam() = this?.isNotBlank() ?: false

    override fun tryResolve(artifactCoordinates: GenericArtifactCoordinates): Iterable<File>? {

        fun error(msg: String) {
            reportError?.invoke(msg) ?: throw RuntimeException(msg)
        }

        fun String?.orNullIfBlank(): String? = this?.takeUnless(String::isBlank)

        val artifactId: DefaultArtifact = with(artifactCoordinates) {
            if (this is MavenArtifactCoordinates && (groupId.isValidParam() || artifactId.isValidParam())) {
                DefaultArtifact(
                    groupId.orNullIfBlank(),
                    artifactId.orNullIfBlank(),
                    null,
                    version.orNullIfBlank()
                )
            } else {
                val coordinatesString = string
                if (coordinatesString.isValidParam() && coordinatesString.count { it == ':' } == 2) {
                    DefaultArtifact(coordinatesString)
                } else {
                    error("Unknown set of arguments to maven resolver: $coordinatesString")
                    return null
                }
            }
        }

        try {
            val deps = Aether(currentRepos(), localRepo).resolve( artifactId, JavaScopes.RUNTIME)
            if (deps != null)
                return deps.map { it.file }
            else {
                error("resolving ${artifactId.artifactId} failed: no results")
            }
        } catch (e: DependencyResolutionException) {
            reportError?.invoke("resolving ${artifactId.artifactId} failed: $e") ?: throw e
        }
        return null
    }

    override fun tryAddRepository(repositoryCoordinates: GenericRepositoryCoordinates): Boolean {
        val url = repositoryCoordinates.url
        if (url != null) {
            repos.add(
                RemoteRepository(
                    if (repositoryCoordinates.name.isValidParam()) repositoryCoordinates.name else url.host,
                    "default",
                    url.toString()
                )
            )
            return true
        }
        return false
    }
}
