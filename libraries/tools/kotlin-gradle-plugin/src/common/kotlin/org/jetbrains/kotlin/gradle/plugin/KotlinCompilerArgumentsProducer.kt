/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ContributeCompilerArgumentsContext
import kotlin.reflect.KClass
import kotlin.reflect.cast

@InternalKotlinGradlePluginApi
interface KotlinCompilerArgumentsProducer {

    enum class ArgumentType {
        Primitive,
        PluginClasspath,
        DependencyClasspath,
        Sources;

        companion object {
            val all = values().toSet()
        }
    }

    interface CreateCompilerArgumentsContext {
        fun <T : CommonToolArguments> create(type: KClass<T>, action: ContributeCompilerArgumentsContext<T>.() -> Unit): T

        companion object {
            val default: CreateCompilerArgumentsContext = CreateCompilerArgumentsContext()
            val lenient: CreateCompilerArgumentsContext = CreateCompilerArgumentsContext(isLenient = true)

            inline fun <reified T : CommonToolArguments> CreateCompilerArgumentsContext.create(
                noinline action: ContributeCompilerArgumentsContext<T>.() -> Unit
            ) = create(T::class, action)
        }
    }

    interface ContributeCompilerArgumentsContext<T : CommonToolArguments> {
        /**
         * This method shall be used for any [action] used to build arguments, that could potentially throw an exception
         * (like resolving dependencies). There are some scenarios (like IDE import), where we want to be lenient
         * and provide arguments on a 'best effort bases'.
         */
        fun <T> runSafe(action: () -> T): T?
        fun primitive(contribution: (args: T) -> Unit)
        fun pluginClasspath(contribution: (args: T) -> Unit)
        fun dependencyClasspath(contribution: (args: T) -> Unit)
        fun sources(contribution: (args: T) -> Unit)
    }

    fun createCompilerArguments(
        context: CreateCompilerArgumentsContext = CreateCompilerArgumentsContext.default
    ): CommonToolArguments
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

    override fun <T : CommonToolArguments> create(
        type: KClass<T>, action: ContributeCompilerArgumentsContext<T>.() -> Unit
    ): T {
        val constructor = type.java.constructors.firstOrNull { it.parameters.isEmpty() }
            ?: throw IllegalArgumentException("'${type.qualifiedName}' does not have an empty constructor")
        val arguments = type.cast(constructor.newInstance())
        ContributeCompilerArgumentsContextImpl(arguments, includeArgumentTypes, isLenient).also(action)
        return arguments
    }

    private class ContributeCompilerArgumentsContextImpl<T : CommonToolArguments>(
        private val arguments: T,
        private val includedArgumentTypes: Set<KotlinCompilerArgumentsProducer.ArgumentType>,
        private val isLenient: Boolean
    ) : ContributeCompilerArgumentsContext<T> {

        private inline fun applyContribution(contribution: (args: T) -> Unit) {
            try {
                contribution(arguments)
            } catch (t: Throwable) {
                if (!isLenient) throw t
            }
        }

        override fun <T> runSafe(action: () -> T): T? {
            return try {
                action()
            } catch (t: Throwable) {
                if (isLenient) null else throw t
            }
        }

        override fun primitive(contribution: (args: T) -> Unit) {
            if (KotlinCompilerArgumentsProducer.ArgumentType.Primitive in includedArgumentTypes) {
                applyContribution(contribution)
            }
        }

        override fun pluginClasspath(contribution: (args: T) -> Unit) {
            if (KotlinCompilerArgumentsProducer.ArgumentType.PluginClasspath in includedArgumentTypes) {
                applyContribution(contribution)
            }
        }

        override fun dependencyClasspath(contribution: (args: T) -> Unit) {
            if (KotlinCompilerArgumentsProducer.ArgumentType.DependencyClasspath in includedArgumentTypes) {
                applyContribution(contribution)
            }
        }

        override fun sources(contribution: (args: T) -> Unit) {
            if (KotlinCompilerArgumentsProducer.ArgumentType.Sources in includedArgumentTypes) {
                applyContribution(contribution)
            }
        }
    }
}
