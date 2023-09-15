/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MoveLambdaOutsideParentheses")

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.internal.KAPT_GENERATE_STUBS_PREFIX
import org.jetbrains.kotlin.gradle.internal.getKaptTaskName
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.addSourcesToKotlinCompileTask
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.whenKaptEnabled
import org.jetbrains.kotlin.tooling.core.extrasFactoryProperty
import java.util.concurrent.Callable

internal class KotlinCompilationSourceSetInclusion(
    private val addSourcesToCompileTask: AddSourcesToCompileTask = DefaultAddSourcesToCompileTask
) {

    fun include(compilation: InternalKotlinCompilation<*>, sourceSet: KotlinSourceSet) {
        /* Check if the source set was already included */
        if (!compilation.includedSourceSets.add(sourceSet)) return

        addSourcesToCompileTask.addSources(
            compilation, sourceSet, addAsCommonSources = lazy {
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
            compilation.project.launchInStage(KotlinPluginLifecycle.Stage.FinaliseCompilations) {
                defaultSourceSetLanguageSettingsChecker.runAllChecks(compilation.defaultSourceSet, sourceSet)
            }
        }
    }

    private companion object {

        /**
         *  Used to store already processed source sets on the compilation instance itself
         * to avoid re-processing of unnecessary source sets!
         */
        val InternalKotlinCompilation<*>.includedSourceSets: MutableSet<KotlinSourceSet>
                by extrasFactoryProperty(KotlinCompilationSourceSetInclusion::class.java.name, { hashSetOf() })
    }


    fun interface AddSourcesToCompileTask {
        fun addSources(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>)
    }

    object DefaultAddSourcesToCompileTask : AddSourcesToCompileTask {
        override fun addSources(
            compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>
        ) {
            addSourcesToKotlinCompileTask(
                project = compilation.project,
                taskName = compilation.compileKotlinTaskName,
                sourceFileExtensions = sourceSet.customSourceFilesExtensions,
                addAsCommonSources = addAsCommonSources,
                sources = { sourceSet.kotlin }
            )

            compilation.project.whenKaptEnabled {
                val kaptGenerateStubsTaskName = getKaptTaskName(compilation.compileKotlinTaskName, KAPT_GENERATE_STUBS_PREFIX)
                addSourcesToKotlinCompileTask(
                    project = compilation.project,
                    taskName = kaptGenerateStubsTaskName,
                    sourceFileExtensions = sourceSet.customSourceFilesExtensions,
                    addAsCommonSources = addAsCommonSources,
                    sources = { sourceSet.kotlin }
                )
            }
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

    /**
     * Will use the [delegate] to add sources to the compile task only if the source set was added directly to the compilation
     * (not if the source set is just part of the dependsOn closure).
     *
     * This is necessary for metadata compilations, where dependsOn source sets will be binary dependencies.
     * In contrast: Platform compilations (jvm, android, linuxX64, ...) will add *all* sources to the compile task
     * and compiles them together.
     */
    class AddSourcesWithoutDependsOnClosure(
        private val delegate: AddSourcesToCompileTask = DefaultAddSourcesToCompileTask
    ) : AddSourcesToCompileTask {
        override fun addSources(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
            if (sourceSet !in compilation.kotlinSourceSets) return
            delegate.addSources(compilation, sourceSet, addAsCommonSources)
        }
    }
}
