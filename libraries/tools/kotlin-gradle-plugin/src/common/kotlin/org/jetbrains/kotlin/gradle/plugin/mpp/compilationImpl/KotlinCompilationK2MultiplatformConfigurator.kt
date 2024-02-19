/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.usesK2
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure

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
                compilation.allKotlinSourceSets
                    .groupBy(keySelector = { it.fragmentName() }) { sourceSet -> sourceSet.kotlin.asFileTree }
                    .map { (fragmentName, sourceFiles) ->
                        K2MultiplatformStructure.Fragment(fragmentName, sourceFiles.reduce { acc, fileTree -> acc + fileTree })
                    }
            })

            compileTask.multiplatformStructure.defaultFragmentName.set(compilation.defaultSourceSet.fragmentName())
        }
    }
}
