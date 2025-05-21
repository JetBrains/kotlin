/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.usesK2
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.isSharedSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.retrieveExternalDependencies
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.future

internal object KotlinCompilationK2MultiplatformConfigurator : KotlinCompilationImplFactory.PreConfigure {
    override fun configure(compilation: KotlinCompilationImpl) {
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
                val project = compilation.project

                val rawFragments = compilation.allKotlinSourceSets
                    .groupBy { it.fragmentName() }
                    .map { (fragmentName, sourceSets) ->
                        val sourceFiles = sourceSets.map { it.kotlin.asFileTree }
                            .reduce { acc, fileTree -> acc + fileTree }
                        K2MultiplatformStructure.Fragment(
                            fragmentName,
                            sourceFiles,
                            if (project.kotlinPropertiesProvider.separateKmpCompilation.get()) {
                                compilation.project.retrieveFragmentDependencies(sourceSets)
                            } else {
                                project.files()
                            }
                        )
                    }

                if (project.kotlinPropertiesProvider.separateKmpCompilationDeduplicateDependencies.get()) {
                    project.deduplicateFragmentDependencies(rawFragments, compileTask.multiplatformStructure.refinesEdges.get())
                } else {
                    rawFragments
                }
            })

            compileTask.multiplatformStructure.defaultFragmentName.set(compilation.defaultSourceSet.fragmentName())
        }
    }

    private fun Project.retrieveFragmentDependencies(
        sourceSets: List<KotlinSourceSet>,
    ): FileCollection = filesProvider {
        future {
            buildSet {
                for (sourceSet in sourceSets) {
                    if (!sourceSet.internal.isSharedSourceSet()) continue
                    add(sourceSet.retrieveExternalDependencies())
                }
            }
        }.getOrThrow()
    }

    /**
     * Removes duplicate dependency definitions based on the refinement hierarchy.
     * If a dependency is defined in a parent fragment (via refinement relationship),
     * it's unnecessary to redefine it in the child fragment.
     */
    private fun Project.deduplicateFragmentDependencies(
        fragments: List<K2MultiplatformStructure.Fragment>,
        refinesEdges: Set<K2MultiplatformStructure.RefinesEdge>,
    ): List<K2MultiplatformStructure.Fragment> {
        val refinementGraph = buildMap<String, MutableSet<String>> {
            refinesEdges.forEach { edge ->
                getOrPut(edge.fromFragmentName) { mutableSetOf() }.add(edge.toFragmentName)
            }
        }

        // Compute all transitive parent fragments for each fragment
        val allParentFragments: Map<String, Set<String>> = buildMap {
            fun collectParents(fragmentName: String): Set<String> {
                this[fragmentName]?.let { return it }

                val parents = mutableSetOf<String>()
                val directParents = refinementGraph[fragmentName] ?: emptySet()

                parents.addAll(directParents)
                directParents.forEach { parent ->
                    parents.addAll(collectParents(parent))
                }

                this[fragmentName] = parents
                return parents
            }

            fragments.forEach { fragment ->
                getOrPut(fragment.fragmentName) { collectParents(fragment.fragmentName) }
            }
        }

        val perFragmentDependencies = fragments.associate { fragment ->
            fragment.fragmentName to fragment.dependencies
        }

        return fragments.map {
            K2MultiplatformStructure.Fragment(
                it.fragmentName,
                it.sources,
                filesProvider {
                    val fragmentName = it.fragmentName
                    val dependencies = perFragmentDependencies[fragmentName]!!
                    val parentDependencies = objects.fileCollection()
                    for (parentFragmentName in allParentFragments.getValue(fragmentName)) {
                        parentDependencies.from(perFragmentDependencies.getValue(parentFragmentName))
                    }
                    dependencies - parentDependencies
                }
            )
        }
    }
}
