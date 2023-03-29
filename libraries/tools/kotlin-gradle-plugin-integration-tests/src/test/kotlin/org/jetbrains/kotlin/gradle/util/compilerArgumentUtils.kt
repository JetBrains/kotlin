/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.gradle.BaseGradleIT
import kotlin.reflect.KClass

inline fun <reified T : CommonCompilerArguments> BuildResult.parseCompilerArguments(): T {
    return parseCompilerArguments(T::class)
}

inline fun <reified T : CommonCompilerArguments> BaseGradleIT.CompiledProject.parseCompilerArguments(): T {
    return parseCompilerArguments(T::class)
}

fun <T : CommonCompilerArguments> BuildResult.parseCompilerArguments(type: KClass<T>): T {
    return parseCompilerArgumentsFromBuildOutput(type, output)
}

fun <T : CommonCompilerArguments> BaseGradleIT.CompiledProject.parseCompilerArguments(type: KClass<T>): T {
    return parseCompilerArgumentsFromBuildOutput(type, output)
}

fun <T : CommonCompilerArguments> parseCompilerArgumentsFromBuildOutput(type: KClass<T>, buildOutput: String): T {
    val arguments = findCommandLineArguments(buildOutput).lines().map { it.trim() }
    return parseCommandLineArguments(type, arguments)
}

fun findCommandLineArguments(buildOutput: String): String {
    val delimiter = "Arguments = ["
    require(delimiter in buildOutput)

    return buildOutput
        .substringAfter("Arguments = [")
        .substringBefore("]")
}
