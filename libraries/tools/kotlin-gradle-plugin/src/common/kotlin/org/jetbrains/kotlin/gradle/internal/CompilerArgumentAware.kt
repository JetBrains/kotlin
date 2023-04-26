/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.copyBeanTo
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils.convertArgumentsToStringList
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType


/**
 * Only relevant for supporting older IDEs (before 2023.2)
 */
@Deprecated("Replaced by KotlinCompilerArgumentsProducer")
interface CompilerArgumentAware<T : CommonToolArguments> : KotlinCompilerArgumentsProducer {

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    @get:Internal
    val serializedCompilerArguments: List<String>
        get() = convertArgumentsToStringList(prepareCompilerArguments())

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    @get:Internal
    val serializedCompilerArgumentsIgnoreClasspathIssues: List<String>
        get() = convertArgumentsToStringList(prepareCompilerArguments(ignoreClasspathResolutionErrors = true))

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    @get:Internal
    val defaultSerializedCompilerArguments: List<String>
        get() = createCompilerArguments(
            CreateCompilerArgumentsContext(includeArgumentTypes = setOf(ArgumentType.Primitive))
        ).let(ArgumentUtils::convertArgumentsToStringList)

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    fun createCompilerArgs(): T = createCompilerArguments(CreateCompilerArgumentsContext(includeArgumentTypes = emptySet()))

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    fun setupCompilerArgs(args: T, defaultsOnly: Boolean = false, ignoreClasspathResolutionErrors: Boolean = false) {
        val newArgs = createCompilerArguments(
            CreateCompilerArgumentsContext(
                includeArgumentTypes = if (defaultsOnly) setOf(ArgumentType.Primitive) else includedArgumentTypes,
                isLenient = ignoreClasspathResolutionErrors
            )
        )
        copyBeanTo(from = newArgs, to = args)
    }

    override fun createCompilerArguments(context: KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext): T

    @Suppress("DEPRECATION")
    private fun <T : CommonToolArguments> CompilerArgumentAware<T>.prepareCompilerArguments(
        ignoreClasspathResolutionErrors: Boolean = false
    ): T = createCompilerArguments(
        CreateCompilerArgumentsContext(
            includeArgumentTypes = includedArgumentTypes,
            isLenient = ignoreClasspathResolutionErrors
        )
    )
}

private val includedArgumentTypes = setOf(
    ArgumentType.Primitive,
    ArgumentType.PluginClasspath,
)
