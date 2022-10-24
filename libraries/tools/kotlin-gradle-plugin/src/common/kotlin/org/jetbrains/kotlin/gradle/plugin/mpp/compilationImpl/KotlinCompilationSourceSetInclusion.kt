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
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.getOrPut
import java.util.concurrent.Callable

internal interface KotlinCompilationSourceSetInclusion {
    fun include(compilation: InternalKotlinCompilation<*>, sourceSet: KotlinSourceSet)
}

internal class DefaultKotlinCompilationSourceSetInclusion(
    private val addSourcesToCompileTask: AddSourcesToCompileTask = DefaultAddSourcesToCompileTask
) : KotlinCompilationSourceSetInclusion {

    override fun include(compilation: InternalKotlinCompilation<*>, sourceSet: KotlinSourceSet) {
        /* Check if the source set was already included */
        if (!compilation.includedSourceSets.add(sourceSet)) return

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

    private companion object {
        /**
         * Key used to store already processed source sets on the compilation instance itself
         * to avoid re-processing of unnecessary source sets!
         */
        private val includedSourceSetsKey = extrasKeyOf<MutableSet<KotlinSourceSet>>(
            DefaultKotlinCompilationSourceSetInclusion::class.java.name
        )

        val InternalKotlinCompilation<*>.includedSourceSets: MutableSet<KotlinSourceSet>
            get() = extras.getOrPut(includedSourceSetsKey, { hashSetOf() })
    }


    fun interface AddSourcesToCompileTask {
        fun addSources(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>)

        companion object {
            fun composite(vararg elements: AddSourcesToCompileTask?): AddSourcesToCompileTask =
                CompositeAddSourcesToCompileTask(listOfNotNull(*elements))
        }
    }

    private class CompositeAddSourcesToCompileTask(private val children: List<AddSourcesToCompileTask>) : AddSourcesToCompileTask {
        override fun addSources(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
            return children.forEach { child -> child.addSources(compilation, sourceSet, addAsCommonSources) }
        }
    }

    object NativeAddSourcesToCompileTask : AddSourcesToCompileTask {
        override fun addSources(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
            val sourceFiles = { sourceSet.kotlin }
            compilation.project.tasks.withType(KotlinNativeCompile::class.java)
                .matching { it.name == compilation.compileKotlinTaskName }
                .configureEach { task ->
                    task.setSource(sourceFiles)
                    task.commonSources.from(
                        compilation.project.files(Callable { if (addAsCommonSources.value) sourceFiles() else emptyList() })
                    )
                }
        }
    }

    object DefaultAddSourcesToCompileTask : AddSourcesToCompileTask {
        override fun addSources(
            compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>
        ) = addSourcesToKotlinCompileTask(
            project = compilation.project,
            taskName = compilation.compileKotlinTaskName,
            sourceFileExtensions = sourceSet.customSourceFilesExtensions,
            addAsCommonSources = addAsCommonSources,
            sources = { sourceSet.kotlin }
        )
    }
}
