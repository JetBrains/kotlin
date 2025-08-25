/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.dsl.usesK2
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.isSharedSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.retrieveExternalDependencies
import org.jetbrains.kotlin.gradle.targets.native.internal.retrievePlatformDependenciesWithNativeDistribution
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.gradle.utils.lazyFuture
import org.jetbrains.kotlin.utils.topologicalSort

internal object KotlinCompilationK2MultiplatformConfigurator : KotlinCompilationImplFactory.PreConfigure {
    override fun configure(compilation: KotlinCompilationImpl) {
        val project = compilation.project
        compilation.project.tasks.configureEach { compileTask ->
            if (compileTask.name != compilation.compileKotlinTaskName) return@configureEach
            if (compileTask !is K2MultiplatformCompilationTask) return@configureEach

            /**
             * Returns fragment name of [this]
             * by default it is name of [KotlinSourceSet] but for android it should name of compilation's default source set.
             * i.e. all android-specific source sets (fragments) should be combined into one.
             * See KT-62508 for detailed explanation
             */
            fun KotlinSourceSet.fragmentName(): String =
                if (androidSourceSetInfoOrNull != null) {
                    compilation.defaultSourceSet.name
                } else {
                    name
                }

            compileTask.multiplatformStructure.refinesEdges.set(compilation.project.provider {
                if (!compileTask.compilerOptions.usesK2.get()) return@provider emptyList()
                compilation.allKotlinSourceSets.flatMap { sourceSet ->
                    sourceSet.dependsOn.mapNotNull { dependsOn ->
                        val from = sourceSet.fragmentName()
                        val to = dependsOn.fragmentName()
                        if (from == to) return@mapNotNull null
                        K2MultiplatformStructure.RefinesEdge(from, to)
                    }
                }
            })

            compileTask.multiplatformStructure.fragments.set(compilation.project.provider {
                if (!compileTask.compilerOptions.usesK2.get()) return@provider emptyList()

                val mostCommonFragmentPerNativePlatforms = project.lazyFuture {
                    val refinementGraph = buildMap<String, MutableSet<String>> {
                        compileTask.multiplatformStructure.refinesEdges.get().forEach { edge ->
                            getOrPut(edge.fromFragmentName) { mutableSetOf() }.add(edge.toFragmentName)
                        }
                    }

                    compilation.allKotlinSourceSets
                        .filter { it.internal.isSharedSourceSet() }
                        .filter { it.isNativeSourceSet.await() }
                        .map { // fragment -> native platforms
                            it.fragmentName() to it.internal.awaitPlatformCompilations().filterIsInstance<AbstractKotlinNativeCompilation>()
                                .map { compilation -> compilation.konanTarget.name }.toSet()
                        }
                        .groupBy({ it.second }) { it.first } // native platforms -> fragments
                        .mapValues { (_, fragments) -> fragments.toSet() }
                        .mapValues { (_, fragments) -> // the most common fragments go first
                            topologicalSort(fragments) {
                                refinementGraph[this]?.filter { it in fragments } ?: emptySet()
                            }.reversed()
                        }
                        .mapValues { (_, fragments) -> fragments.first() }
                }

                compilation.allKotlinSourceSets
                    .groupBy { it.fragmentName() }
                    .map { (fragmentName, sourceSets) ->
                        val sourceFiles = sourceSets.map { it.kotlin.asFileTree }
                            .reduce { acc, fileTree -> acc + fileTree }
                        K2MultiplatformStructure.Fragment(
                            fragmentName,
                            sourceFiles,
                            if (project.kotlinPropertiesProvider.separateKmpCompilation.get()) {
                                compilation.project.retrieveFragmentDependencies(
                                    sourceSets,
                                    fragmentName,
                                    mostCommonFragmentPerNativePlatforms
                                )
                            } else {
                                project.files()
                            }
                        )
                    }
            })

            compileTask.multiplatformStructure.defaultFragmentName.set(compilation.defaultSourceSet.fragmentName())
        }
    }

    private fun Project.retrieveFragmentDependencies(
        sourceSets: List<KotlinSourceSet>,
        fragmentName: String,
        mostCommonFragmentPerNativePlatformsFuture: Future<Map<Set<String>, String>>,
    ): FileCollection = filesProvider {
        future {
            buildSet {
                for (sourceSet in sourceSets) {
                    val internalSourceSet = sourceSet.internal
                    if (!internalSourceSet.isSharedSourceSet()) continue
                    if (internalSourceSet.isNativeSourceSet.await()) {
                        val mostCommonFragmentPerNativePlatforms = mostCommonFragmentPerNativePlatformsFuture.await()
                        val mostCommonNativeFragment = mostCommonFragmentPerNativePlatforms.maxBy { it.key.size }.value
                        if (mostCommonNativeFragment == fragmentName) {
                            add(project.konanDistribution.stdlib)
                        }
                        val metadataCompilation = internalSourceSet.compilations.filterIsInstance<KotlinSharedNativeCompilation>()
                            .find { it.name == sourceSet.name }
                        if (metadataCompilation != null) {
                            val nativePlatforms = internalSourceSet.awaitPlatformCompilations()
                                .filterIsInstance<AbstractKotlinNativeCompilation>()
                                .map { compilation -> compilation.konanTarget.name }.toSet()
                            if (mostCommonFragmentPerNativePlatforms[nativePlatforms] == fragmentName) {
                                add(metadataCompilation.retrievePlatformDependenciesWithNativeDistribution())
                            }
                        }
                    }
                    // We do not need transitive dependencies defined on higher levels of the hierarchy here
                    add(sourceSet.retrieveExternalDependencies(transitive = false))
                }
            }
        }.getOrThrow()
    }
}
