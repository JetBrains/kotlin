/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.usesK2
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure

internal object KotlinCompilationK2MultiplatformConfigurator : KotlinCompilationImplFactory.PreConfigure {
    override fun configure(compilation: KotlinCompilationImpl) {
        compilation.project.tasks.configureEach { compileTask ->
            if (compileTask.name != compilation.compileKotlinTaskName) return@configureEach
            if (compileTask !is K2MultiplatformCompilationTask) return@configureEach

            compileTask.multiplatformStructure.refinesEdges.set(compilation.project.provider {
                if (!compileTask.compilerOptions.usesK2.get()) return@provider emptyList()
                compilation.allKotlinSourceSets.flatMap { sourceSet ->
                    sourceSet.dependsOn.map { dependsOn ->
                        K2MultiplatformStructure.RefinesEdge(sourceSet.name, dependsOn.name)
                    }
                }
            })

            compileTask.multiplatformStructure.fragments.set(compilation.project.provider {
                if (!compileTask.compilerOptions.usesK2.get()) return@provider emptyList()
                compilation.allKotlinSourceSets.map { sourceSet ->
                    K2MultiplatformStructure.Fragment(sourceSet.name, sourceSet.kotlin.asFileTree)
                }
            })
        }
    }
}
