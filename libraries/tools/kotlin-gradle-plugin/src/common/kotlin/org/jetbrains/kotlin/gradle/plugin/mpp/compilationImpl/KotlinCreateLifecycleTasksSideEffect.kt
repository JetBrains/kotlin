/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.DefaultTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.util.concurrent.Callable

internal val KotlinCreateLifecycleTasksSideEffect = KotlinCompilationSideEffect { compilation ->
    val project = compilation.target.project

    project.registerTask<DefaultTask>(compilation.compileAllTaskName) {
        it.group = LifecycleBasePlugin.BUILD_GROUP
        it.description = "Assembles outputs for compilation '${compilation.name}' of target '${compilation.target.name}'"
        it.inputs.files(Callable {
            // the task may not be registered at this point, reference it lazily
            @Suppress("DEPRECATION")
            compilation.compileKotlinTaskProvider.map { it.outputs.files }
        })

        if (compilation is KotlinJvmCompilation && (compilation.target as? KotlinJvmTarget)?.withJavaEnabled == true) {
            it.inputs.files({ compilation.compileJavaTaskProvider?.map { it.outputs.files } })
        }

        it.inputs.files(compilation.output.resourcesDirProvider)
    }
    compilation.output.classesDirs.from(project.files().builtBy(compilation.compileAllTaskName))
}