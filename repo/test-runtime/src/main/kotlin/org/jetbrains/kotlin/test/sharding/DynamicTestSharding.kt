/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.platform.engine.TestTag
import java.lang.reflect.Method
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.optionals.getOrNull
import kotlin.text.appendLine


/**
 * Spreads the dynamic tests of a `@TestFactory` across shards.
 * Without it, all tests of the factory run on a single shard.
 *
 * The factory runs on every shard. It must declare a [DynamicTestShardingContext]
 * and call [shardBy] exactly once, which keeps only the test cases of the current shard.
 *
 * ```kotlin
 * @TestFactory
 * @DynamicTestSharding
 * context(_: DynamicTestShardingContext)
 * fun tests() = listOf("first", "second").shardBy { it }.map { name ->
 *     dynamicTest(name) { /* Test this case. */ }
 * }
 * ```
 */
@ExtendWith(DynamicTestShardingExtension::class)
@Tag(testsShardDynamicTagKey)
annotation class DynamicTestSharding

/** Provided by JUnit to [DynamicTestSharding] factories. Declare it as a context parameter to use [shardBy]. */
sealed interface DynamicTestShardingContext

/**
 * Returns the elements of the current shard, in their original order (all elements if sharding is disabled).
 *
 * [key] must be stable across shards and runs (e.g., a name). Elements with equal keys stay on the same shard.
 * Call it once, directly in the factory: not lazily and not inside a test.
 */
context(sharding: DynamicTestShardingContext)
fun <T> Iterable<T>.shardBy(key: (T) -> String): List<T> = when (sharding) {
    is DynamicTestShardingContextImpl -> sharding.filterCurrentShard(this) { value -> key(value).encodeToByteArray() }
}

/*
Internal Implementation Part
 */

internal class DynamicTestShardingExtension : ParameterResolver,
    BeforeTestExecutionCallback,
    InvocationInterceptor {

    companion object {
        val namespace: ExtensionContext.Namespace = create("tests.sharding.dynamic")
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean {
        return parameterContext.parameter.type == DynamicTestShardingContext::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): DynamicTestShardingContext {
        val sharding = DynamicTestShardingContextImpl(readTestShardingConfiguration(extensionContext))
        extensionContext.getStore(namespace).put(DynamicTestShardingContext::class.java, sharding)
        return sharding
    }


    override fun beforeTestExecution(context: ExtensionContext) {
        if ((context.testMethod.getOrNull() ?: return).parameterTypes.none { it == DynamicTestShardingContext::class.java }) {
            error(buildString {
                appendLine("'@${DynamicTestSharding::class.java.simpleName} requires ${DynamicTestShardingContext::class.simpleName}'")
                appendLine("Request an instance of ${DynamicTestShardingContext::class.simpleName} as test parameter")
            })
        }
    }

    override fun <T> interceptTestFactoryMethod(
        invocation: InvocationInterceptor.Invocation<T>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ): T {
        val result = invocation.proceed()
        val sharding = extensionContext.getStore(namespace).get(DynamicTestShardingContext::class.java) ?: error(buildString {
            appendLine("'@${DynamicTestSharding::class.java.simpleName} requires ${DynamicTestShardingContext::class.simpleName}'")
            appendLine("Request an instance of ${DynamicTestShardingContext::class.simpleName} as test parameter")
        })

        sharding as DynamicTestShardingContextImpl
        if (!sharding.isSharded.load()) {
            error("'@${DynamicTestSharding::class.java.simpleName} requires a call to 'shardBy()'")
        }

        return result
    }
}

@OptIn(ExperimentalAtomicApi::class)
private class DynamicTestShardingContextImpl(
    private val configuration: TestShardingConfiguration,
) : DynamicTestShardingContext {
    val isSharded: AtomicBoolean = AtomicBoolean(false)

    fun <T> filterCurrentShard(elements: Iterable<T>, key: (T) -> ByteArray): List<T> {
        if (!isSharded.compareAndSet(expectedValue = false, newValue = true)) {
            error("'shardBy' can only be called once")
        }

        return elements.filter { value -> isCurrentShard(key(value), configuration) }
    }
}
