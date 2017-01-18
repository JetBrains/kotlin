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


import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.File

open class JarFileScriptDependenciesResolver() : AnnotationTriggeredScriptResolver {
    override val acceptedAnnotations = listOf(DirRepository::class, DependsOnJar::class)
    override val autoImports = listOf(DependsOnJar::class.java.`package`.name + ".*")

    private val resolvers = java.util.concurrent.ConcurrentLinkedDeque<LocalJarResolver>(listOf(DirectResolver()))

    override fun resolveForAnnotation(annotation: Annotation): List<File> {
        return when (annotation) {
            is DirRepository -> {
                FlatLibDirectoryResolver.Companion.tryCreate(annotation)
                        ?.apply { resolvers.add(this) }
                ?: throw IllegalArgumentException("Illegal argument for DirRepository annotation: $annotation")
                emptyList()
            }
            is DependsOnJar -> {
                resolvers.asSequence().mapNotNull { it.tryResolve(annotation) }.firstOrNull()?.toList() ?:
                throw Exception("Unable to resolve dependency $annotation")

            }
            else -> throw Exception("Unknown annotation ${annotation.javaClass}")
        }

    }


    interface LocalJarResolver {
        fun tryResolve(dependsOn: DependsOnJar): Iterable<File>?
    }

    class DirectResolver : LocalJarResolver {
        override fun tryResolve(dependsOn: DependsOnJar): Iterable<File>? {
            return dependsOn.filename.check(String::isNotBlank)
                    ?.let(::File)
                    ?.check { it.exists() && (it.isFile || it.isDirectory) }
                    ?.let { listOf(it) }
        }
    }

    class FlatLibDirectoryResolver(val path: File) : LocalJarResolver {
        init {
            if (!path.exists() || !path.isDirectory) throw IllegalArgumentException("Invalid flat lib directory repository path '$path'")
        }

        override fun tryResolve(dependsOn: DependsOnJar): Iterable<File>? =
                // TODO: add coordinates and wildcard matching
                dependsOn.filename.check(String::isNotBlank)
                        ?.let { File(path, it) }
                        ?.check { it.exists() && (it.isFile || it.isDirectory) }
                        ?.let { listOf(it) }

        companion object {
            fun tryCreate(annotation: DirRepository): FlatLibDirectoryResolver? =
                    annotation.path.check(String::isNotBlank)
                            ?.let(::File)
                            ?.check { it.exists() && it.isDirectory }
                            ?.let(::FlatLibDirectoryResolver)
        }
    }
}
