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

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils

interface CompilerArgumentAware<T : CommonToolArguments> {
    @get:Internal
    val serializedCompilerArguments: List<String>
        get() = ArgumentUtils.convertArgumentsToStringList(prepareCompilerArguments())

    @get:Internal
    val serializedCompilerArgumentsIgnoreClasspathIssues: List<String>
        get() = ArgumentUtils.convertArgumentsToStringList(prepareCompilerArguments(ignoreClasspathResolutionErrors = true))

    @get:Internal
    val defaultSerializedCompilerArguments: List<String>
        get() = createCompilerArgs()
            .also { setupCompilerArgs(it, defaultsOnly = true) }
            .let(ArgumentUtils::convertArgumentsToStringList)

    fun createCompilerArgs(): T

    // This function is called via reflection from IntelliJ Kotlin plugin, so keeping the old signature around too.
    // Since this is an interface, we can't use @JvmOverloads here.
    fun setupCompilerArgs(args: T, defaultsOnly: Boolean = false, ignoreClasspathResolutionErrors: Boolean = false) =
        setupCompilerArgs(
            args = args,
            defaultsOnly = defaultsOnly,
            ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors,
            includeClasspath = true
        )

    fun setupCompilerArgs(args: T, defaultsOnly: Boolean = false, ignoreClasspathResolutionErrors: Boolean = false, includeClasspath: Boolean = true)
}

internal fun <T : CommonToolArguments> CompilerArgumentAware<T>.prepareCompilerArguments(ignoreClasspathResolutionErrors: Boolean = false, includeClasspath : Boolean = true) =
    createCompilerArgs().also { setupCompilerArgs(it, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors, includeClasspath = includeClasspath) }
