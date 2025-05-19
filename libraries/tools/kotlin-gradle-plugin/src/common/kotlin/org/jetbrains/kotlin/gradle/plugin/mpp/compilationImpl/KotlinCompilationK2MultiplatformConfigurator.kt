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

                compilation.allKotlinSourceSets
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
            })

            compileTask.multiplatformStructure.defaultFragmentName.set(compilation.defaultSourceSet.fragmentName())
        }
    }

    private fun Project.retrieveFragmentDependencies(
        sourceSets: List<KotlinSourceSet>,
    ): FileCollection = project.filesProvider {
        future {
            buildSet {
                for (sourceSet in sourceSets) {
                    if (!sourceSet.internal.isSharedSourceSet()) continue
                    add(sourceSet.retrieveExternalDependencies())
                }
            }
        }.getOrThrow()
    }
}
