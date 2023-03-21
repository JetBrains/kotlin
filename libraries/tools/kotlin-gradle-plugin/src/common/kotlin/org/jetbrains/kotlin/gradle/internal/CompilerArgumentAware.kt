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
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils

@Deprecated("Replaced by KotlinCompilerArgumentsProducer")
interface CompilerArgumentAware<T : CommonToolArguments> {
    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION")
    @get:Internal
    val serializedCompilerArguments: List<String>
        get() = ArgumentUtils.convertArgumentsToStringList(prepareCompilerArguments())

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION")
    @get:Internal
    val serializedCompilerArgumentsIgnoreClasspathIssues: List<String>
        get() = ArgumentUtils.convertArgumentsToStringList(prepareCompilerArguments(ignoreClasspathResolutionErrors = true))

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION")
    @get:Internal
    val defaultSerializedCompilerArguments: List<String>
        get() = createCompilerArgs()
            .also { setupCompilerArgs(it, defaultsOnly = true) }
            .let(ArgumentUtils::convertArgumentsToStringList)

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.WARNING)
    fun createCompilerArgs(): T

    @Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.WARNING)
    fun setupCompilerArgs(args: T, defaultsOnly: Boolean = false, ignoreClasspathResolutionErrors: Boolean = false)
}

@Deprecated("Replaced by KotlinCompilerArgumentsProducer", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
internal fun <T : CommonToolArguments> CompilerArgumentAware<T>.prepareCompilerArguments(ignoreClasspathResolutionErrors: Boolean = false) =
    createCompilerArgs().also { setupCompilerArgs(it, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors) }
