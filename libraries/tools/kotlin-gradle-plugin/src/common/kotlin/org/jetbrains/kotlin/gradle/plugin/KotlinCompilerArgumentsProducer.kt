/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ContributeCompilerArgumentsContext
import kotlin.reflect.KClass
import kotlin.reflect.cast

@InternalKotlinGradlePluginApi
interface KotlinCompilerArgumentsProducer {

    enum class ArgumentType {
        Primitive,
        DependencyClasspath,
        PluginClasspath,
        Sources;

        companion object {
            val all = values().toSet()
        }
    }

    interface CreateCompilerArgumentsContext {
        fun <T : CommonCompilerArguments> create(type: KClass<T>, action: ContributeCompilerArgumentsContext<T>.() -> Unit): T

        companion object {
            val default: CreateCompilerArgumentsContext = CreateCompilerArgumentsContext()
            val lenient: CreateCompilerArgumentsContext = CreateCompilerArgumentsContext(isLenient = true)

            inline fun <reified T : CommonCompilerArguments> CreateCompilerArgumentsContext.create(
                noinline action: ContributeCompilerArgumentsContext<T>.() -> Unit
            ) = create(T::class, action)
        }
    }

    interface ContributeCompilerArgumentsContext<T : CommonCompilerArguments> {
        fun <T> tryLenient(action: () -> T): T?
        fun contribute(type: ArgumentType, contribution: (T) -> Unit)
    }

    fun createCompilerArguments(
        context: CreateCompilerArgumentsContext = CreateCompilerArgumentsContext()
    ): CommonCompilerArguments
}

internal fun CreateCompilerArgumentsContext(
    includeArgumentTypes: Set<KotlinCompilerArgumentsProducer.ArgumentType> = KotlinCompilerArgumentsProducer.ArgumentType.all,
    isLenient: Boolean = false
): KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext {
    return CreateCompilerArgumentsContextImpl(includeArgumentTypes, isLenient)
}

private class CreateCompilerArgumentsContextImpl(
    private val includeArgumentTypes: Set<KotlinCompilerArgumentsProducer.ArgumentType>,
    private val isLenient: Boolean
) : KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext {

    override fun <T : CommonCompilerArguments> create(
        type: KClass<T>, action: ContributeCompilerArgumentsContext<T>.() -> Unit
    ): T {
        val constructor = type.java.constructors.firstOrNull { it.parameters.isEmpty() }
            ?: throw IllegalArgumentException("'${type.qualifiedName}' does not have an empty constructor")
        val arguments = type.cast(constructor.newInstance())
        ContributeCompilerArgumentsContextImpl(arguments).also(action)
        return arguments
    }

    private inner class ContributeCompilerArgumentsContextImpl<T : CommonCompilerArguments>(
        private val arguments: T
    ) : ContributeCompilerArgumentsContext<T> {

        override fun <T> tryLenient(action: () -> T): T? {
            return try {
                action()
            } catch (t: Throwable) {
                if (isLenient) null else throw t
            }
        }

        override fun contribute(type: KotlinCompilerArgumentsProducer.ArgumentType, contribution: (T) -> Unit) {
            if (type in includeArgumentTypes) contribution(arguments)
        }
    }
}
