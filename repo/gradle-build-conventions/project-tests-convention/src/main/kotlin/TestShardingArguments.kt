/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

internal const val CURRENT_TEST_SHARD_KEY = "tests.currentShard"
internal const val TOTAL_TEST_SHARDS_KEY = "tests.totalShards"
internal const val TEST_SHARD_SEED_KEY = "tests.shardSeed"

internal const val TEST_SHARD_TIMINGS_KEY = "tests.shardTimings"
internal const val TEST_SHARD_TASK_KEY = "tests.shardTask"

/**
 * Recorded durations of the test classes of a test task (see `TestShardTimings` in `:repo:test-runtime`):
 * `<project directory>/test-shard-timings/<task name>.tsv`. Sharded runs of a task with this file balance its shards by duration.
 */
internal const val TEST_SHARD_TIMINGS_DIRECTORY = "test-shard-timings"

internal fun Project.testShardingArguments(task: Test): TestShardingArguments {
    val arguments = objects.newInstance(TestShardingArguments::class.java)
    val currentShard = providers.gradleProperty(CURRENT_TEST_SHARD_KEY).map { it.toInt() }
    arguments.currentShard.set(currentShard)
    arguments.totalShards.set(providers.gradleProperty(TOTAL_TEST_SHARDS_KEY).map { it.toInt() })
    arguments.shardSeed.set(providers.gradleProperty(TEST_SHARD_SEED_KEY).map { it.toInt() })

    /*
    Only an input of sharded runs: refreshing the timings must not invalidate the cache of unsharded runs.
    The file may not exist: a missing input file is fingerprinted as missing, so adding it later reruns the task.
     */
    val timingsFile = layout.projectDirectory.file("$TEST_SHARD_TIMINGS_DIRECTORY/${task.name}.tsv").asFile
    arguments.timingsPath.set(currentShard.map { timingsFile.absolutePath })
    arguments.timings.from(currentShard.map { listOf(timingsFile) }.orElse(emptyList()))

    /* Orders shards with equal loads differently in every task, see 'TestShardTimings.assign' */
    arguments.task.set(task.path)
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

    /* The content decides the assignment of test classes to shards, the location does not */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val timings: ConfigurableFileCollection

    /* Set for sharded runs only, see 'timings' */
    @get:Internal
    abstract val timingsPath: Property<String>

    @get:Input
    abstract val task: Property<String>

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
            if (timingsPath.isPresent) "-D$TEST_SHARD_TIMINGS_KEY=${timingsPath.get()}" else null,
            "-D$TEST_SHARD_TASK_KEY=${task.get()}",
        )
    }
}
