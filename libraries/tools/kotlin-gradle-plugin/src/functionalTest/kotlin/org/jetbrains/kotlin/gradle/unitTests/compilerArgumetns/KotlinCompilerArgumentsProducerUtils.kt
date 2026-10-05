/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils.convertArgumentsToStringList
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.PluginClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.Primitive

internal val KotlinCompilerArgumentsProducer.serializedCompilerArgumentsIgnoreClasspathIssues: List<String>
    get() = convertArgumentsToStringList(prepareCompilerArguments(ignoreClasspathResolutionErrors = true))

@Suppress("UNCHECKED_CAST")
private fun <T : CommonToolArguments> KotlinCompilerArgumentsProducer.prepareCompilerArguments(
    ignoreClasspathResolutionErrors: Boolean = false
): T = createCompilerArguments(
    CreateCompilerArgumentsContext(
        includeArgumentTypes = includedArgumentTypes,
        isLenient = ignoreClasspathResolutionErrors
    )
) as T

private val includedArgumentTypes = setOf(Primitive, PluginClasspath)
