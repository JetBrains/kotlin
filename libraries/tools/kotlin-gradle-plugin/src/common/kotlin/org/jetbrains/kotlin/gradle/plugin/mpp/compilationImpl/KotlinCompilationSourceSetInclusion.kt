/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MoveLambdaOutsideParentheses")

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.addSourcesToKotlinCompileTask
import org.jetbrains.kotlin.gradle.plugin.mpp.addSourcesToKotlinNativeCompileTask
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation

internal interface KotlinCompilationSourceSetInclusion {
    fun include(compilation: InternalKotlinCompilation<*>, sourceSet: KotlinSourceSet)
}

internal class DefaultKotlinCompilationSourceSetInclusion(
    private val addSourcesToCompileTask: AddSourcesToCompileTask
) : KotlinCompilationSourceSetInclusion {


    interface AddSourcesToCompileTask {
        fun addSources(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>)

        object Default : AddSourcesToCompileTask {
            override fun addSources(
                compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>
            ) {
                addSourcesToKotlinCompileTask(
                    compilation.project,
                    compilation.compileKotlinTaskName,
                    sourceSet.customSourceFilesExtensions,
                    addAsCommonSources,
                    { sourceSet.kotlin }
                )
            }
        }

        object Native : AddSourcesToCompileTask {
            override fun addSources(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
                addSourcesToKotlinNativeCompileTask(
                    compilation.project, compilation.compileKotlinTaskName, { sourceSet.kotlin }, addAsCommonSources
                )
            }
        }
    }

    private val processedSourceSets = hashSetOf<KotlinSourceSet>()

    override fun include(compilation: InternalKotlinCompilation<*>, sourceSet: KotlinSourceSet) {
        if (!processedSourceSets.add(sourceSet)) return

        addSourcesToCompileTask.addSources(
            compilation, sourceSet,
            addAsCommonSources = lazy {
                compilation.project.kotlinExtension.sourceSets.any { otherSourceSet ->
                    sourceSet in otherSourceSet.dependsOn
                }
            }
        )

        // Use `forced = false` since `api`, `implementation`, and `compileOnly` may be missing in some cases like
        // old Java & Android projects:
        compilation.project.addExtendsFromRelation(
            compilation.apiConfigurationName,
            sourceSet.apiConfigurationName,
            forced = false
        )

        compilation.project.addExtendsFromRelation(
            compilation.implementationConfigurationName,
            sourceSet.implementationConfigurationName,
            forced = false
        )
        compilation.project.addExtendsFromRelation(
            compilation.compileOnlyConfigurationName,
            sourceSet.compileOnlyConfigurationName,
            forced = false
        )

        compilation.project.addExtendsFromRelation(
            compilation.runtimeOnlyConfigurationName,
            sourceSet.runtimeOnlyConfigurationName,
            forced = false
        )

        if (sourceSet.name != compilation.defaultSourceSet.name) {
            // Temporary solution for checking consistency across source sets participating in a compilation that may
            // not be interconnected with the dependsOn relation: check the settings as if the default source set of
            // the compilation depends on the one added to the compilation:
            defaultSourceSetLanguageSettingsChecker.runAllChecks(compilation.defaultSourceSet, sourceSet)
        }
    }
}
