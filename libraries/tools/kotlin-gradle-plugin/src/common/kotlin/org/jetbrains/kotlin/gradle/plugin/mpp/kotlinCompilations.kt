/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.closure
import java.util.*
import java.util.concurrent.Callable

internal fun KotlinCompilation<*>.isMain(): Boolean =
    name == KotlinCompilation.MAIN_COMPILATION_NAME

internal fun KotlinCompilation<*>.isTest(): Boolean =
    name == KotlinCompilation.TEST_COMPILATION_NAME

/**
 * see https://youtrack.jetbrains.com/issue/KT-45412
 * Some implementations of [KotlinCompilation] are not including their [KotlinCompilation.defaultSourceSet] into [kotlinSourceSet]s
 * This helper function might disappear in the future, once the behaviour of those [KotlinCompilation] implementations is streamlined.
 * @return [KotlinCompilation.kotlinSourceSets] + [KotlinCompilation.defaultSourceSet]
 */
internal val KotlinCompilation<*>.kotlinSourceSetsIncludingDefault: Set<KotlinSourceSet> get() = kotlinSourceSets + defaultSourceSet

internal fun addCommonSourcesToKotlinCompileTask(
    project: Project,
    taskName: String,
    sourceFileExtensions: Iterable<String>,
    sources: () -> Any
) = addSourcesToKotlinCompileTask(project, taskName, sourceFileExtensions, lazyOf(true), sources)

// FIXME this function dangerously ignores an incorrect type of the task (e.g. if the actual task is a K/N one); consider reporting a failure
internal fun addSourcesToKotlinCompileTask(
    project: Project,
    taskName: String,
    sourceFileExtensions: Iterable<String>,
    addAsCommonSources: Lazy<Boolean> = lazyOf(false),
    /** Evaluated as project.files(...) */
    sources: () -> Any
) {
    fun AbstractKotlinCompile<*>.configureAction() {
        // In this call, the super-implementation of `source` adds the directories files to the roots of the union file tree,
        // so it's OK to pass just the source roots.
        setSource(Callable(sources))
        with(sourceFileExtensions.toSet()) {
            if (isNotEmpty()) {
                include(flatMap { ext -> ext.fileExtensionCasePermutations().map { "**/*.$it" } })
            }
        }

        // The `commonSourceSet` is passed to the compiler as-is, converted with toList
        commonSourceSet.from(
            Callable<Any> { if (addAsCommonSources.value) sources else emptyList<Any>() }
        )
    }

    project.tasks
        // To configure a task that may have not yet been created at this point, use 'withType-matching-configureEach`:
        .withType(AbstractKotlinCompile::class.java)
        .matching { it.name == taskName }
        .configureEach { compileKotlinTask ->
            compileKotlinTask.configureAction()
        }
}

internal val KotlinCompilation<*>.associateWithClosure: Iterable<KotlinCompilation<*>>
    get() = this.closure { it.associateWith }

internal fun KotlinCompilation<*>.disambiguateName(simpleName: String): String {
    return lowerCamelCaseName(
        target.disambiguationClassifier,
        compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
        simpleName
    )
}

private typealias CompilationsBySourceSet = Map<KotlinSourceSet, Set<KotlinCompilation<*>>>

internal object CompilationSourceSetUtil {
    private const val EXT_NAME = "kotlin.compilations.bySourceSets"

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreateProperty(
        project: Project,
        initialize: Property<CompilationsBySourceSet>.() -> Unit
    ): Property<CompilationsBySourceSet> {
        val ext = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        if (!ext.has(EXT_NAME)) {
            ext.set(EXT_NAME, project.objects.property(Any::class.java as Class<CompilationsBySourceSet>).also(initialize))
        }
        return ext.get(EXT_NAME) as Property<CompilationsBySourceSet>
    }

    // FIXME: the results include the compilations of the metadata target; the callers should care about filtering them out
    //        if they need only the platform compilations
    // TODO: create a separate util function: `platformCompilationsBySourceSets`
    // TODO: visit all call sites, check if they handle the metadata compilations correctly
    fun compilationsBySourceSets(project: Project): CompilationsBySourceSet {
        val compilationNamesBySourceSetName = getOrCreateProperty(project) {
            var shouldFinalizeValue = false

            set(project.provider {
                val kotlinExtension = project.kotlinExtension
                val targets = when (kotlinExtension) {
                    is KotlinMultiplatformExtension -> kotlinExtension.targets
                    is KotlinSingleTargetExtension<*> -> listOf(kotlinExtension.target)
                    else -> emptyList()
                }

                val compilations = targets.flatMap { it.compilations }

                val result = mutableMapOf<KotlinSourceSet, MutableSet<KotlinCompilation<*>>>().apply {
                    compilations.forEach { compilation ->
                        compilation.allKotlinSourceSets.forEach { sourceSet ->
                            getOrPut(sourceSet) { mutableSetOf() }.add(compilation)
                        }
                    }
                    kotlinExtension.sourceSets.forEach { sourceSet ->
                        // For source sets not taking part in any compilation, keep an empty set to avoid errors on access by key
                        getOrPut(sourceSet) { mutableSetOf() }
                    }
                }

                if (shouldFinalizeValue) {
                    set(result)
                }

                return@provider result
            })

            project.gradle.taskGraph.whenReady { shouldFinalizeValue = true }

            // In case the value is first queried after the task graph has been calculated, finalize the value as soon as a task executes:
            object : TaskExecutionListener {
                override fun beforeExecute(task: Task) = Unit
                override fun afterExecute(task: Task, state: TaskState) {
                    shouldFinalizeValue = true
                }
            }
        }

        return compilationNamesBySourceSetName.get()
    }
}

private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

internal fun filterModuleName(moduleName: String): String =
    moduleName.replace(invalidModuleNameCharactersRegex, "_")
