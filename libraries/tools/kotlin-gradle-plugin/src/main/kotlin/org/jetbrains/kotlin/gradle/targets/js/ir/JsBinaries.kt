/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

sealed class JsBinary(
    internal val name: String,
    internal val type: KotlinJsIrType,
    internal val compilation: KotlinJsCompilation
) {
    val linkTaskName: String = linkTaskName(type)

    val linkTask: TaskProvider<KotlinJsIrLink>
        get() = target.project.tasks.named(linkTaskName) as TaskProvider<KotlinJsIrLink>

    private fun linkTaskName(type: KotlinJsIrType): String =
        lowerCamelCaseName(
            "compile",
            type.name.toLowerCase(),
            name,
            "Kotlin",
            target.targetName
        )

    val target: KotlinTarget
        get() = compilation.target

    val project: Project
        get() = target.project
}

class Executable(
    name: String,
    type: KotlinJsIrType,
    compilation: KotlinJsCompilation
) : JsBinary(
    name,
    type,
    compilation
)

class TestExecutable(
    name: String,
    type: KotlinJsIrType,
    compilation: KotlinJsCompilation
) : JsBinary(
    name,
    type,
    compilation
)