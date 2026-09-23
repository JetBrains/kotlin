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
 * Lets a JUnit test factory split its generated tests across shards instead of assigning the whole factory to one shard.
 * The factory runs on every shard and must request a [DynamicTestShardingContext] and call [shardTestsBy] exactly once
 * before returning normally, even for empty inputs or when sharding is disabled.
 * A missing call fails the factory before any generated tests execute. Factories that abort or throw retain their original outcome.
 *
 * ```kotlin
 * @TestFactory
 * @DynamicTestSharding
 * context(_: DynamicTestShardingContext)
 * fun tests() = listOf("first", "second").shardTestsBy { it }.map { name ->
 *     dynamicTest(name) { /* Test this case. */ }
 * }
 * ```
 */
@ExtendWith(DynamicTestShardingExtension::class)
@Tag(testsShardDynamicTagKey)
annotation class DynamicTestSharding

/**
 * Context supplied by JUnit to a method annotated with [DynamicTestSharding].
 * Declare it as a context parameter to make [shardTestsBy] available in the method body.
 */
sealed interface DynamicTestShardingContext

/**
 * Returns the elements assigned to the current shard, preserving their order, or all elements when sharding is disabled.
 * Call exactly once per [DynamicTestShardingContext], before the factory returns, not during lazy iteration or inside test bodies.
 * Create dynamic tests from the returned elements; the result may be empty on a shard.
 *
 * [key] must return a stable identifier for each test case, identical across shards and runs.
 * Equal keys stay together; assignments are deterministic for the same `tests.totalShards` and `tests.shardSeed`.
 * The current shard is selected by `tests.currentShard` (1-based); when unset, no elements are filtered out.
 */
context(sharding: DynamicTestShardingContext)
fun <T> Iterable<T>.shardTestsBy(key: (T) -> String): List<T> = when (sharding) {
    is DynamicTestShardingContextImpl -> sharding.sharedTestsBy(this) { value -> key(value).encodeToByteArray() }
}

/*
Internal Implementation Part
 */

internal const val testsShardDynamicTagKey = "tests.sharding.dynamic"
internal val testsShardDynamicTag = TestTag.create("tests.sharding.dynamic")

internal class DynamicTestShardingExtension : ParameterResolver,
    BeforeTestExecutionCallback,
    InvocationInterceptor {

    companion object {
        val namespace: ExtensionContext.Namespace = create("tests.sharding.danymic")
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
        val sharding = DynamicTestShardingContextImpl()
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
            error("'@${DynamicTestSharding::class.java.simpleName} requires a call to 'shardDynamicTestBy()'")
        }

        return result
    }
}

@OptIn(ExperimentalAtomicApi::class)
private class DynamicTestShardingContextImpl : DynamicTestShardingContext {
    val isSharded: AtomicBoolean = AtomicBoolean(false)


    fun <T> sharedTestsBy(elements: Iterable<T>, key: (T) -> ByteArray): List<T> {
        if (!isSharded.compareAndSet(expectedValue = false, newValue = true)) {
            error("'shardTestsByBinaryKey' can only be called once")
        }

        return elements.filter { value -> isCurrentShard(key(value)) }
    }
}
