/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer.Companion.generateBinaryName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

interface JsBinary {
    val compilation: KotlinJsCompilation
    val name: String
    val mode: KotlinJsBinaryMode
}

sealed class JsIrBinary(
    override val compilation: KotlinJsCompilation,
    override val name: String,
    override val mode: KotlinJsBinaryMode
) : JsBinary {
    val linkTaskName: String = linkTaskName()

    val linkTask: TaskProvider<KotlinJsIrLink>
        get() = target.project.tasks
            .withType(KotlinJsIrLink::class.java)
            .named(linkTaskName)

    private fun linkTaskName(): String =
        lowerCamelCaseName(
            "compile",
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
    compilation: KotlinJsCompilation,
    name: String,
    mode: KotlinJsBinaryMode
) : JsIrBinary(
    compilation,
    name,
    mode
) {
    val executeTaskBaseName: String =
        generateBinaryName(
            compilation,
            mode,
            null
        )
}

class Library(
    compilation: KotlinJsCompilation,
    name: String,
    mode: KotlinJsBinaryMode
) : JsIrBinary(
    compilation,
    name,
    mode
) {
    val executeTaskBaseName: String =
        generateBinaryName(
            compilation,
            mode,
            null
        )
}

// Hack for legacy
internal val JsBinary.executeTaskBaseName: String
    get() = generateBinaryName(
        compilation,
        mode,
        null
    )