/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider

internal const val TEST_SHARD_INDEX_KEY = "kotlin.build.test.shard.index"
internal const val TEST_SHARD_COUNT_KEY = "kotlin.build.test.shard.count"
internal const val TEST_SHARD_SEED_KEY = "kotlin.build.test.shard.seed"

internal val Project.testShardingArguments: TestShardingArguments
    get() {
        val arguments = objects.newInstance(TestShardingArguments::class.java)
        arguments.shardIndex.set(project.providers.gradleProperty(TEST_SHARD_INDEX_KEY).map { it.toInt() })
        arguments.shardCount.set(project.providers.gradleProperty(TEST_SHARD_COUNT_KEY).map { it.toInt() })
        arguments.shardSeed.set(project.providers.gradleProperty(TEST_SHARD_SEED_KEY).map { it.toInt() })
        return arguments
    }

abstract class TestShardingArguments : CommandLineArgumentProvider {

    @get:Input
    @get:Optional
    abstract val shardIndex: Property<Int>

    @get:Input
    @get:Optional
    abstract val shardCount: Property<Int>

    @get:Input
    @get:Optional
    abstract val shardSeed: Property<Int>

    override fun asArguments(): Iterable<String> {
        if (!shardIndex.isPresent && !shardCount.isPresent) {
            return emptyList()
        }

        val shardIndex = shardIndex.get()
        val shardCount = shardCount.get()

        require(shardCount > 0) {
            "Expected '$TEST_SHARD_COUNT_KEY' to be a positive integer. Found: '$shardCount'"
        }

        require(shardIndex in 0 until shardCount) {
            "Expected '$TEST_SHARD_INDEX_KEY' to be in 0 until $shardCount. Found: '$shardIndex'"
        }

        return listOfNotNull(
            "-D$TEST_SHARD_INDEX_KEY=${shardIndex}",
            "-D$TEST_SHARD_COUNT_KEY=${shardCount}",
            if (shardSeed.isPresent) "-D$TEST_SHARD_SEED_KEY=${shardSeed.get()}" else null,
        )
    }
}
