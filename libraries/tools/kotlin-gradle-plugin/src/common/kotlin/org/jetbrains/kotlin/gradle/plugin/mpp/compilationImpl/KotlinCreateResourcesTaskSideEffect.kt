/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask

internal val KotlinCreateResourcesTaskSideEffect = KotlinCompilationSideEffect { compilation ->
    val project = compilation.project
    compilation.internal.processResourcesTaskName?.let { processResourcesTaskName ->
        val resourceSet = project.files({ compilation.allKotlinSourceSets.map { it.resources } })
        val resourcesDestinationDir = project.file(compilation.output.resourcesDir)
        val resourcesTask = project.locateOrRegisterTask<ProcessResources>(processResourcesTaskName) { resourcesTask ->
            resourcesTask.description = "Processes ${resourceSet}."
            resourcesTask.from(resourceSet)
            resourcesTask.into(resourcesDestinationDir)
        }
        compilation.output.resourcesDirProvider = resourcesTask.map { resourcesDestinationDir }
    }
}