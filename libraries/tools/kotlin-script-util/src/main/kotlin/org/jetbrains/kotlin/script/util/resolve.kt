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

package org.jetbrains.kotlin.script.util

import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.script.util.resolvers.DirectResolver
import org.jetbrains.kotlin.script.util.resolvers.FlatLibDirectoryResolver
import org.jetbrains.kotlin.script.util.resolvers.MavenResolver
import org.jetbrains.kotlin.script.util.resolvers.Resolver
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Future
import kotlin.reflect.KClass

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
        script.annotations.forEach {
            when (it) {
                is Repository -> File(it.value).check { it.exists() && it.isDirectory }?.let { resolvers.add(FlatLibDirectoryResolver(it)) }
                                                ?: throw IllegalArgumentException("Illegal argument for Repository annotation: ${it.value}")
                is DependsOn -> {}
                is InvalidScriptResolverAnnotation -> throw Exception("Invalid annotation ${it.name}", it.error)
                else -> throw Exception("Unknown annotation ${it.javaClass}")
            }
        }
        return script.annotations.filterIsInstance(DependsOn::class.java).flatMap { dep ->
            resolvers.asSequence().mapNotNull { it.tryResolve(dep) }.firstOrNull() ?:
                    throw Exception("Unable to resolve dependency $dep")
        }
    }
}

class AnnotationsBasedResolver :
        KotlinAnnotatedScriptDependenciesResolver(emptyList(), arrayListOf(DirectResolver(), MavenResolver()))
