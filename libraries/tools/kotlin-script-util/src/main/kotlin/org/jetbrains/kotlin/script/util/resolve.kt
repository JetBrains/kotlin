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

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.script.util

import org.jetbrains.kotlin.script.util.resolvers.DirectResolver
import org.jetbrains.kotlin.script.util.resolvers.FlatLibDirectoryResolver
import org.jetbrains.kotlin.script.util.resolvers.MavenResolver
import org.jetbrains.kotlin.script.util.resolvers.Resolver
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.util.concurrent.Future
import kotlin.script.dependencies.KotlinScriptExternalDependencies
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.dependencies.asFuture
import kotlin.script.templates.AcceptedAnnotations

@Deprecated("Use new resolving classes from kotlin-scripting-dependencies and kotlin-scripting-dependencies-maven")
open class KotlinAnnotatedScriptDependenciesResolver(val baseClassPath: List<File>, resolvers: Iterable<Resolver>)
    : ScriptDependenciesResolver
{
    private val resolvers: MutableList<Resolver> = resolvers.toMutableList()

    inner class ResolvedDependencies(previousDependencies: KotlinScriptExternalDependencies?, depsFromAnnotations: List<File> ) : KotlinScriptExternalDependencies {
        override val classpath = if (resolvers.isEmpty()) baseClassPath  else baseClassPath + depsFromAnnotations
        override val imports = if (previousDependencies != null) emptyList() else listOf(DependsOn::class.java.`package`.name + ".*")
    }

    @AcceptedAnnotations(DependsOn::class, Repository::class)
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
                         previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> {
        val depsFromAnnotations: List<File> = resolveFromAnnotations(script)
        return (if (previousDependencies != null && depsFromAnnotations.isEmpty()) previousDependencies
                else ResolvedDependencies(previousDependencies, depsFromAnnotations)
               ).asFuture()
    }

    private fun resolveFromAnnotations(script: ScriptContents): List<File> {
        script.annotations.forEach { annotation ->
            when (annotation) {
                is Repository -> {
                    val isFlat: Boolean = resolvers.firstIsInstanceOrNull<FlatLibDirectoryResolver>()?.tryAddRepo(annotation)
                        ?: (FlatLibDirectoryResolver.tryCreate(annotation)?.also { resolvers.add(it) } != null)
                    if (!isFlat) {
                        resolvers.find { it !is FlatLibDirectoryResolver && it.tryAddRepo(annotation) }
                            ?: throw IllegalArgumentException("Illegal argument for Repository annotation: $annotation")
                    }
                }
                is DependsOn -> {}
                is InvalidScriptResolverAnnotation -> throw Exception("Invalid annotation ${annotation.name}", annotation.error)
                else -> throw Exception("Unknown annotation ${annotation.javaClass}")
            }
        }
        return script.annotations.filterIsInstance(DependsOn::class.java).flatMap { dep ->
            resolvers.asSequence().mapNotNull { it.tryResolve(dep) }.firstOrNull() ?:
                    throw Exception("Unable to resolve dependency $dep")
        }
    }
}

@Deprecated("Use FileSystemDependenciesResolver from kotlin-scripting-dependencies instead")
class LocalFilesResolver :
        KotlinAnnotatedScriptDependenciesResolver(emptyList(), arrayListOf(DirectResolver()))

@Deprecated("Use CompoundDependenciesResolver and MavenDependenciesResolver from kotlin-scripting-dependencies and kotlin-scripting-dependencies-maven")
class FilesAndMavenResolver :
        KotlinAnnotatedScriptDependenciesResolver(emptyList(), arrayListOf(DirectResolver(), MavenResolver()))

