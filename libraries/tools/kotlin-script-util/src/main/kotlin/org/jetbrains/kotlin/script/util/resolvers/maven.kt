/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script.util.resolvers

import com.jcabi.aether.Aether
import org.jetbrains.kotlin.script.util.DependsOn
import java.io.File
import java.util.*
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.DependencyResolutionException
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.artifact.JavaScopes

val mavenCentral = RemoteRepository("maven-central", "default", "http://repo1.maven.org/maven2/")

class MavenResolver(val reportError: ((String) -> Unit) = {}): Resolver {

    // TODO: make robust
    val localRepo = File(File(System.getProperty("user.home")!!, ".m2"), "repository")

    val repos: ArrayList<RemoteRepository> = arrayListOf()

    private fun currentRepos() = if (repos.isEmpty()) arrayListOf(mavenCentral) else repos

    override fun tryResolve(dependsOn: DependsOn): Iterable<File>? {
        if (dependsOn.value.count { it == ':' } == 2) {
            try {
                val deps = Aether(currentRepos(), localRepo).resolve(
                        DefaultArtifact(dependsOn.value),
                        JavaScopes.RUNTIME)
                if (deps != null)
                    return deps.map { it.file }
                else {
                    reportError("resolving [${dependsOn.value}] failed: no results")
                }
            }
            catch (e: DependencyResolutionException) {
                reportError("resolving [${dependsOn.value}] failed: $e")
            }
            return listOf()
        }
        return null
    }
}
