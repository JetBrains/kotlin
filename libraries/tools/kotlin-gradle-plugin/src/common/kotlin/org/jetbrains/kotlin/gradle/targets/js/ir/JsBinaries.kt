/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer.Companion.generateBinaryName
import org.jetbrains.kotlin.gradle.targets.js.subtargets.createDefaultDistribution
import org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

interface JsBinary {
    val compilation: KotlinJsCompilation
    val name: String
    val mode: KotlinJsBinaryMode
    val distribution: Distribution
}

sealed class JsIrBinary(
    final override val compilation: KotlinJsCompilation,
    final override val name: String,
    override val mode: KotlinJsBinaryMode
) : JsBinary {
    override val distribution: Distribution =
        createDefaultDistribution(compilation.target.project, compilation.target.targetName, name)

    val linkTaskName: String = linkTaskName()

    var generateTs: Boolean = false

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

    val linkSyncTaskName: String = linkSyncTaskName()

    val validateGeneratedTsTaskName: String = validateTypeScriptTaskName()

    val linkSyncTask: TaskProvider<IncrementalSyncTask>
        get() = target.project.tasks
            .withType<IncrementalSyncTask>()
            .named(linkSyncTaskName)

    private fun linkSyncTaskName(): String =
        lowerCamelCaseName(
            compilation.target.disambiguationClassifier,
            compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            name,
            COMPILE_SYNC
        )

    private fun validateTypeScriptTaskName(): String =
        lowerCamelCaseName(
            compilation.target.disambiguationClassifier,
            compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            name,
            TypeScriptValidationTask.NAME
        )

    val target: KotlinTarget
        get() = compilation.target

    val project: Project
        get() = target.project
}

open class Executable(
    compilation: KotlinJsCompilation,
    name: String,
    mode: KotlinJsBinaryMode
) : JsIrBinary(
    compilation,
    name,
    mode
) {
    override val distribution: Distribution =
        createDefaultDistribution(
            compilation.target.project,
            compilation.target.targetName,
            super.distribution.distributionName
        )

    val executeTaskBaseName: String =
        generateBinaryName(
            compilation,
            mode,
            null
        )
}

open class ExecutableWasm(
    compilation: KotlinJsCompilation,
    name: String,
    mode: KotlinJsBinaryMode
) : Executable(
    compilation,
    name,
    mode
) {
    val optimizeTaskName: String = optimizeTaskName()

    val optimizeTask: TaskProvider<BinaryenExec>
        get() = target.project.tasks
            .withType<BinaryenExec>()
            .named(optimizeTaskName)

    private fun optimizeTaskName(): String =
        "${linkTaskName}Optimize"
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

internal const val COMPILE_SYNC = "compileSync"