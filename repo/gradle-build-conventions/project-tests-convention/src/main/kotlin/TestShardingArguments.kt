/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider

internal const val CURRENT_TEST_SHARD_KEY = "tests.currentShard"
internal const val TOTAL_TEST_SHARDS_KEY = "tests.totalShards"
internal const val TEST_SHARD_SEED_KEY = "tests.shardSeed"

internal val Project.testShardingArguments: TestShardingArguments
    get() {
        val arguments = objects.newInstance(TestShardingArguments::class.java)
        arguments.currentShard.set(project.providers.gradleProperty(CURRENT_TEST_SHARD_KEY).map { it.toInt() })
        arguments.totalShards.set(project.providers.gradleProperty(TOTAL_TEST_SHARDS_KEY).map { it.toInt() })
        arguments.shardSeed.set(project.providers.gradleProperty(TEST_SHARD_SEED_KEY).map { it.toInt() })
        return arguments
    }

abstract class TestShardingArguments : CommandLineArgumentProvider {

    @get:Input
    @get:Optional
    abstract val currentShard: Property<Int>

    @get:Input
    @get:Optional
    abstract val totalShards: Property<Int>

    @get:Input
    @get:Optional
    abstract val shardSeed: Property<Int>

    override fun asArguments(): Iterable<String> {
        if (!currentShard.isPresent && !totalShards.isPresent) {
            return emptyList()
        }

        val currentShard = currentShard.get()
        val totalShards = totalShards.get()

        require(totalShards > 0) {
            "Expected 'totalShards' to be a positive integer. Found: '$totalShards'"
        }

        require(currentShard > 0) {
            "Expected 'currentShard' to be a positive integer. Found: '$currentShard'"
        }

        require(currentShard <= totalShards) {
            "Expected 'currentShard' to not exceed 'totalShards'. Found 'currentShard=$currentShard', totalShards=$totalShards"
        }

        return listOfNotNull(
            "-D$CURRENT_TEST_SHARD_KEY=${currentShard}",
            "-D$TOTAL_TEST_SHARDS_KEY=${totalShards}",
            if (shardSeed.isPresent) "-D$TEST_SHARD_SEED_KEY=${shardSeed.get()}" else null,
        )
    }
}
