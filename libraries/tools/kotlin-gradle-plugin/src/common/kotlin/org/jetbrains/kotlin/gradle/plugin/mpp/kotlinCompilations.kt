/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.fileExtensionCasePermutations
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.tooling.core.closure
import java.util.concurrent.Callable

internal fun KotlinCompilation<*>.isMain(): Boolean =
    name == KotlinCompilation.MAIN_COMPILATION_NAME

internal fun KotlinCompilation<*>.isTest(): Boolean =
    name == KotlinCompilation.TEST_COMPILATION_NAME

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

private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

internal fun filterModuleName(moduleName: String): String =
    moduleName.replace(invalidModuleNameCharactersRegex, "_")

internal inline fun <reified T : KotlinCommonOptions> InternalKotlinCompilation<*>.castKotlinOptionsType(): InternalKotlinCompilation<T> {
    this.kotlinOptions as T
    @Suppress("UNCHECKED_CAST")
    return this as InternalKotlinCompilation<T>
}

internal inline fun <reified T : KotlinCommonCompilerOptions> HasCompilerOptions<*>.castCompilerOptionsType(): HasCompilerOptions<T> {
    this.options as T
    @Suppress("UNCHECKED_CAST")
    return this as HasCompilerOptions<T>
}
