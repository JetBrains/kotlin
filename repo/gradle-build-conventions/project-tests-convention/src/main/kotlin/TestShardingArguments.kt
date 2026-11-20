/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

internal const val TEST_SHARD_INDEX_KEY = "kotlin.build.test.shard.index"
internal const val TEST_SHARD_COUNT_KEY = "kotlin.build.test.shard.count"
internal const val TEST_SHARD_SEED_KEY = "kotlin.build.test.shard.seed"
internal const val TEST_SHARD_TIMINGS_KEY = "kotlin.build.test.shard.timings"
internal const val TEST_SHARD_TASK_KEY = "kotlin.build.test.shard.task"

/** The directory of the test shard timings, one `<name>.tsv` file per build, selected by [TEST_SHARD_TIMINGS_KEY] */
internal const val TEST_SHARD_TIMINGS_DIRECTORY = "repo/test-timings"

internal val Project.testShardingArguments: TestShardingArguments
    get() {
        val arguments = objects.newInstance(TestShardingArguments::class.java)
        arguments.shardIndex.set(project.providers.gradleProperty(TEST_SHARD_INDEX_KEY).map { it.toInt() })
        arguments.shardCount.set(project.providers.gradleProperty(TEST_SHARD_COUNT_KEY).map { it.toInt() })
        arguments.shardSeed.set(project.providers.gradleProperty(TEST_SHARD_SEED_KEY).map { it.toInt() })
        val shardTimingsDirectory = project.rootDir.resolve(TEST_SHARD_TIMINGS_DIRECTORY)
        arguments.shardTimings.fileProvider(
            project.providers.gradleProperty(TEST_SHARD_TIMINGS_KEY)
                .map { shardTimingsDirectory.resolve("$it.tsv") }
                .filter { it.exists() }
        )
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

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val shardTimings: RegularFileProperty

    /** The path of the test task, which takes its classes from [shardTimings] */
    @get:Input
    @get:Optional
    abstract val shardTask: Property<String>

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
            if (shardTimings.isPresent) "-D$TEST_SHARD_TIMINGS_KEY=${shardTimings.get().asFile.absolutePath}" else null,
            if (shardTimings.isPresent) "-D$TEST_SHARD_TASK_KEY=${shardTask.get()}" else null,
        )
    }
}
