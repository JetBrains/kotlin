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

package org.jetbrains.kotlin.script.resolver


import com.jcabi.aether.Aether
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.rethrow
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.DependencyResolutionException
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.artifact.JavaScopes
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ConcurrentLinkedDeque

open class MavenScriptDependenciesResolver() : AnnotationTriggeredScriptResolver {
    override val acceptedAnnotations = listOf(MavenRepository::class, DependsOnMaven::class)
    override val autoImports = listOf(DependsOnMaven::class.java.`package`.name + ".*")

    val resolver = MavenResolver()

    override fun resolveForAnnotation(annotation: Annotation): List<File> {
        return when (annotation) {
            is MavenRepository -> {
                resolver.tryAddRepo(annotation)
                emptyList()
            }
            is DependsOnMaven -> {
                resolver.tryResolve(annotation)?.toList() ?:
                throw Exception("Unable to resolve dependency $annotation")
            }
            else -> throw Exception("Unknown annotation ${annotation.javaClass}")
        }
    }


    interface MavenJarResolver {
        fun tryAddRepo(annotation: MavenRepository): Boolean
        fun tryResolve(dependsOn: DependsOnMaven): Iterable<File>?
    }

    class MavenResolver(val reportError: ((String) -> Unit)? = null) : MavenJarResolver {
        private val mavenCentral = RemoteRepository("maven-central", "default", "http://repo1.maven.org/maven2/")

        // TODO: make robust
        private val localRepo = File(File(System.getProperty("user.home")!!, ".m2"), "repository")

        private val repos: ConcurrentLinkedDeque<RemoteRepository> = ConcurrentLinkedDeque()

        private fun currentRepos() = if (repos.isEmpty()) arrayListOf(mavenCentral) else repos.toList()

        private fun String.isValidParam() = isNotBlank()

        override fun tryResolve(dependsOn: DependsOnMaven): Iterable<File>? {

            fun error(msg: String) {
                reportError?.invoke(msg) ?: throw RuntimeException(msg)
            }

            fun String?.orNullIfBlank(): String? = this?.check(String::isNotBlank)

            val parts = dependsOn.fullGAV.split(':')
            if (parts.size < 3 || parts.size > 4 || parts.any { it.isNullOrBlank() }) {
                error("Unknown set of arguments to maven resolver: ${dependsOn.fullGAV}, should be `group:artifact:version` or `group:artifact:classifier:version`")
                return null
            }
            val artifactId: DefaultArtifact = DefaultArtifact(dependsOn.fullGAV)

            try {
                val deps = Aether(currentRepos(), localRepo).resolve(artifactId, JavaScopes.RUNTIME)
                if (deps != null)
                    return deps.map { it.file }
                else {
                    error("resolving ${artifactId.artifactId} failed: no results")
                }
            }
            catch (e: DependencyResolutionException) {
                reportError?.invoke("resolving ${artifactId.artifactId} failed: $e") ?: rethrow(e)
            }
            return null
        }

        override fun tryAddRepo(annotation: MavenRepository): Boolean {
            val urlStr = annotation.url.check { it.isValidParam() } ?: annotation.url.check { it.isValidParam() } ?: return false
            try {
                URL(urlStr)
            }
            catch (_: MalformedURLException) {
                return false
            }
            repos.add(RemoteRepository(if (annotation.id.isValidParam()) annotation.id else "unknown-${System.currentTimeMillis()}",
                                       "default", urlStr))
            return true
        }
    }

}