/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

sealed class JsBinary(
    internal val target: KotlinTarget,
    internal val name: String,
    internal val type: BuildVariantKind
) {
    val linkTaskName: String = linkTaskName()

    val linkTask: TaskProvider<KotlinJsIrLink>
        get() = target.project.tasks
            .withType(KotlinJsIrLink::class.java)
            .named(linkTaskName)

    private fun linkTaskName(): String =
        lowerCamelCaseName(
            "compile",
            type.name.toLowerCase(),
            name,
            "Kotlin",
            target.targetName
        )

    val project: Project
        get() = target.project
}

class Executable(
    target: KotlinTarget,
    name: String,
    type: BuildVariantKind
) : JsBinary(
    target,
    name,
    type
)

class TestExecutable(
    target: KotlinTarget,
    name: String,
    type: BuildVariantKind
) : JsBinary(
    target,
    name,
    type
)