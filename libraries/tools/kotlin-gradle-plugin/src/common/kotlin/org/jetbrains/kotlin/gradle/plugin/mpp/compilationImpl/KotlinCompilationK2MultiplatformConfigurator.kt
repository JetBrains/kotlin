/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.usesK2
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.defaultImpl
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.isSharedSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.retrieveExternalDependencies
import org.jetbrains.kotlin.gradle.targets.native.internal.cinteropCommonizerDependencies
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget
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

                val refinementGraph = buildMap<String, MutableSet<String>> {
                    compileTask.multiplatformStructure.refinesEdges.get().forEach { edge ->
                        getOrPut(edge.fromFragmentName) { mutableSetOf() }.add(edge.toFragmentName)
                    }
                }

                val mostCommonFragmentPerNativePlatforms = project.lazyFuture {
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

                val sourceSetPerFragments = compilation.allKotlinSourceSets.groupBy { it.fragmentName() }
                val friendSourceSetsPerFragment = calculateFriendSourceSets(refinementGraph, sourceSetPerFragments)

                sourceSetPerFragments.map { (fragmentName, sourceSets) ->
                    val sourceFiles = sourceSets.map { it.defaultImpl.allKotlin.asFileTree }
                        .reduce { acc, fileTree -> acc + fileTree }
                    K2MultiplatformStructure.Fragment(
                        fragmentName = fragmentName,
                        sources = sourceFiles,
                        dependencies = if (project.kotlinPropertiesProvider.separateKmpCompilation.get()) {
                            compilation.project.retrieveFragmentDependencies(
                                sourceSets,
                                fragmentName,
                                mostCommonFragmentPerNativePlatforms,
                                compilation.kotlinSourceSets,
                            )
                        } else project.files(),
                        friends = if (project.kotlinPropertiesProvider.separateKmpCompilation.get()) {
                            val friendDependencies = sourceSets.map {
                                val friendSourceSets = friendSourceSetsPerFragment[fragmentName].orEmpty()
                                compilation.project.retrieveFragmentFriends(it, friendSourceSets)
                            }
                            project.files(friendDependencies)
                        } else project.files(),
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
        directlyCompiledSourceSets: Set<KotlinSourceSet>,
    ): FileCollection = filesProvider {
        future {
            buildSet {
                for (sourceSet in sourceSets) {
                    // Skip leaf source-set
                    if (sourceSet in directlyCompiledSourceSets) continue
                    val internalSourceSet = sourceSet.internal
                    if (internalSourceSet.isNativeSourceSet.await()) {
                        val mostCommonFragmentPerNativePlatforms = mostCommonFragmentPerNativePlatformsFuture.await()
                        val mostCommonNativeFragment = mostCommonFragmentPerNativePlatforms.maxByOrNull { it.key.size }?.value
                        // 'null' could happen in case of a project with only native target
                        // and, essentially, "common" source set becomes native one.
                        if (mostCommonNativeFragment == null || mostCommonNativeFragment == fragmentName) {
                            add(project.konanDistribution.stdlib)
                        }

                        sourceSet.commonizerTarget.await()?.let {
                            add(it.retrievePlatformDependenciesWithNativeDistribution(project))
                        }

                        add(cinteropCommonizerDependencies(sourceSet))
                    }
                    // We do not need transitive dependencies defined on higher levels of the hierarchy here
                    add(sourceSet.retrieveExternalDependencies(transitive = false))
                }
            }
        }.getOrThrow()
    }

    /**
     * [getVisibleSourceSetsFromAssociateCompilations] returns accumulated set of friend sourceset.
     *       common
     *       /     \
     *     web     jvm
     *    /   \
     *   js  wasm
     *
     * So, for example, in this project structure it will return the following values:
     *   commonTest -> [commonMain]
     *      webTest -> [commonMain, webMain]
     *       jsTest -> [commonMain, webMain, jsMain]
     *     wasmTest -> [commonMain, webMain, jsMain]
     *      jvmTest -> [commonMain, jvmMain]
     *
     * But the compiler expects dependencies in `-Xfragment-friend dependencies` to be unique and deduplicated.
     * And this function aims to deduplicate dependency lists:
     *   commonTest -> [commonMain]
     *      webTest -> [webMain]
     *       jsTest -> [jsMain]
     *     wasmTest -> [jsMain]
     *      jvmTest -> [jvmMain]
     */
    private fun calculateFriendSourceSets(
        refinementGraph: Map<String, Set<String>>,
        sourceSetPerFragments: Map<String, List<KotlinSourceSet>>,
    ): Map<String, List<KotlinSourceSet>> {
        val fullFriends = sourceSetPerFragments.mapValues { (_, sourceSets) ->
            sourceSets.flatMap { getVisibleSourceSetsFromAssociateCompilations(it) }.distinct()
        }
        val result = mutableMapOf<String, List<KotlinSourceSet>>()

        for (fragment in sourceSetPerFragments.keys) {
            val friendSourceSets = fullFriends[fragment].orEmpty().toMutableList()
            for (dependsOn in refinementGraph[fragment].orEmpty()) {
                friendSourceSets -= fullFriends[dependsOn].orEmpty()
            }
            result[fragment] = friendSourceSets
        }

        return result
    }

    private fun Project.retrieveFragmentFriends(
        sourceSet: KotlinSourceSet,
        friendSourceSets: List<KotlinSourceSet>,
    ): FileCollection = filesProvider {
        future {
            val metadataTarget = multiplatformExtension.awaitMetadataTarget()
            val relatedCompilationOutputs = friendSourceSets.mapNotNull { friendSourceSet ->
                val platformCompilations = friendSourceSet.internal.awaitPlatformCompilations()
                val compilation = if (platformCompilations.size > 1) {
                    val metadataCompilation = metadataTarget.compilations.findByName(friendSourceSet.name)
                    if (metadataCompilation == null) {
                        logger.warn("Cannot find metadata compilation for friend source set: ${friendSourceSet.name}, but it was expected when calculating friend source sets for source set: ${sourceSet.name}")
                        return@mapNotNull null
                    }
                    metadataCompilation
                } else {
                    platformCompilations.single()
                }

                compilation.output.classesDirs
            }

            relatedCompilationOutputs
        }.getOrThrow()
    }
}
