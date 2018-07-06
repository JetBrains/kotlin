/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSetOutput
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet

interface KotlinCompilation: Named, HasAttributes, HasKotlinDependencies {
    val target: KotlinTarget

    val compilationName: String

    val kotlinSourceSets: List<KotlinSourceSet>

    val compileDependencyConfigurationName: String

    var compileDependencyFiles: FileCollection

    val output: SourceSetOutput

    val platformType get() = target.platformType

    val compileKotlinTaskName: String

    val compileAllTaskName: String

    companion object {
        const val MAIN_COMPILATION_NAME = "main"
        const val TEST_COMPILATION_NAME = "test"
    }

    fun source(sourceSet: KotlinSourceSet)

    override fun getName(): String = compilationName
}

interface KotlinCompilationToRunnableFiles : KotlinCompilation {
    val runtimeDependencyConfigurationName: String

    var runtimeDependencyFiles: FileCollection
}

interface KotlinCompilationWithResources : KotlinCompilation {
    val processResourcesTaskName: String
}