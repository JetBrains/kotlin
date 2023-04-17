/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer

internal class IdeCompilerArgumentsResolverImpl : IdeCompilerArgumentsResolver {

    override fun resolveCompilerArguments(any: Any): List<String>? {
        return when (any) {
            is KotlinCompilerArgumentsProducer -> resolveCompilerArguments(any)
            is KotlinCompilation<*> -> resolveCompilerArguments(any.compileTaskProvider.orNull ?: return null)
            else -> return null
        }
    }

    private fun resolveCompilerArguments(producer: KotlinCompilerArgumentsProducer): List<String> {
        val compilerArguments = producer.createCompilerArguments(
            CreateCompilerArgumentsContext(
                isLenient = true,
                includeArgumentTypes = setOf(
                    KotlinCompilerArgumentsProducer.ArgumentType.Primitive,
                    /*
                    Always resolve the plugin classpath: This is still consumed by the associated IDE plugins.
                    e.g: the kotlinx.serialisation IDE plugin relies on this classpath.
                     */
                    KotlinCompilerArgumentsProducer.ArgumentType.PluginClasspath,
                ),
            )
        )
        return compilerArguments.toArgumentStrings(
            shortArgumentKeys = true,
            compactArgumentValues = false
        )
    }
}