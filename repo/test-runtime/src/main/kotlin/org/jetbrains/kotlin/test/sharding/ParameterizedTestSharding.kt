/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor
import org.junit.jupiter.params.provider.Arguments
import org.junit.platform.engine.UniqueId
import kotlin.jvm.optionals.getOrNull

/**
 * Spreads the invocations of a parameterized test across shards.
 * Without it, all invocations of the test run on a single shard.
 *
 * The arguments source must call [shard], which keeps only the invocations of the current shard.
 * A shard may get no invocations, so use `allowZeroInvocations = true`.
 * Can be used as a meta-annotation.
 *
 * ```kotlin
 * @ParameterizedTest(allowZeroInvocations = true)
 * @ParameterizedTestSharding
 * @ArgumentsSource(VersionsProvider::class)
 * fun test(version: String) { /* Test this case. */ }
 *
 * class VersionsProvider : ArgumentsProvider {
 *     override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext) =
 *         listOf("1.0", "2.0").map { Arguments.of(it) }.shard(context).toList().stream()
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(ParameterizedTestShardingExtension::class)
@Tag(testsShardDynamicTagKey)
annotation class ParameterizedTestSharding

/**
 * Returns the [Arguments] of the current shard, in their original order.
 * Returns all [Arguments] if sharding is disabled or the test is not annotated with [ParameterizedTestSharding].
 *
 * Pass the [context] given to the arguments source.
 * Every shard must compute the same split, so all arguments must be [Comparable] or have a stable `toString()`.
 *
 * Every shard gets an equal number of invocations.
 * Invocations with the same leading arguments (e.g., the same Gradle version) land on neighbouring shards.
 * Tests of the same class split equal arguments the same way, unless sharded by method (see [ShardByMethod]).
 */
fun Iterable<Arguments>.shard(context: ExtensionContext): Iterable<Arguments> {
    /* Not annotated with @ParameterizedTestSharding: the discovery-time sharding already assigned the whole template to this shard */
    if (testsShardDynamicTagKey !in context.tags) return this

    /*
     * Mark the template as sharded, so that ParameterizedTestShardingExtension lets its invocations run.
     * This must happen before any early return below: a shard receiving no invocations is still correctly sharded,
     * and so is a template running without sharding.
     */
    val templateContext = context.testTemplateContext()
    templateContext.getStore(ParameterizedTestShardingExtension.namespace).put(ParameterizedTestShardingExtension.isShardedKey, true)

    /* Sharding is disabled: every shard runs everything */
    val configuration = readTestShardingConfiguration(context)
    if (!configuration.isShardingEnabled) return this

    val list = toList()
    if (list.isEmpty()) return list

    /*
     * Every shard computes the same assignment independently, so the inputs must be identical on all shards:
     * - the rows are ordered by the arguments themselves (see MatrixTestSharding), not by the order in which they were provided,
     *   which requires the arguments to be comparable or to have a stable 'toString()',
     * - the salt is the sharding distribution key of the template (not of an invocation), so all invocations of one template see the same salt:
     *   the test class by default (so templates of one class are shifted alike),
     *   or the unique id of the template when sharding by method (so each template is shifted differently).
     */
    val matrix = MatrixTestSharding(
        matrix = list.map { it.get().toList() },
        salt = templateContext.shardingDistributionKey(configuration).encodeToByteArray(),
        totalShards = configuration.totalShards,
        shardSeed = configuration.shardSeed,
    )

    /* Keep only the invocations of this shard, in their original order */
    return list.filterIndexed { row, _ ->
        matrix.getShardOfRow(row) == configuration.currentShard
    }
}

/*
Internal Implementation Part
 */

/**
 * Arguments sources may also be called with the context of an individual invocation (e.g., by execution conditions).
 * Resolving the template keeps the salt identical in both cases.
 */
private fun ExtensionContext.testTemplateContext(): ExtensionContext {
    return generateSequence(this) { it.parent.getOrNull() }
        .firstOrNull { context -> UniqueId.parse(context.uniqueId).lastSegment.type == TestTemplateTestDescriptor.SEGMENT_TYPE }
        ?: error("'shard' requires the context of a test template, but got '$uniqueId'")
}

internal class ParameterizedTestShardingExtension : BeforeEachCallback {

    companion object {
        val namespace: ExtensionContext.Namespace = create("tests.sharding.parameterized")
        const val isShardedKey = "isSharded"
    }

    override fun beforeEach(context: ExtensionContext) {
        /* The store of the invocation also resolves values stored by its test template */
        if (context.getStore(namespace).get(isShardedKey) != true) {
            error(buildString {
                appendLine("'@${ParameterizedTestSharding::class.java.simpleName}' requires a call to 'shard()'")
                appendLine("Call 'shard()' in the arguments source of '${context.displayName}'")
            })
        }
    }
}
