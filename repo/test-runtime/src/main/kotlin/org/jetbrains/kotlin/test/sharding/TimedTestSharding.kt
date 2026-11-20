/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import java.io.File

/**
 * Assigns test classes to shards by their recorded times, so that the shards take equally long.
 *
 * The [timings] list the test classes of all test tasks of a build, one `<task path>\t<class name>\t<milliseconds>` line
 * per class and task (as written by the `test-inventory` tool of kotlin-infrastructure).
 * The classes go to shards longest first, each to the shard with the least time so far (the lowest one on a tie).
 * Classes of all tasks are assigned together, because a shard runs all of its tasks.
 * Every shard computes the same assignment from the same timings.
 *
 * All tests of a class go to its shard, including the invocations of a [ParameterizedTestSharding] template,
 * even if the class is sharded by method (see [ShardByMethod]).
 * Classes missing from the timings are sharded by hash (see [calculateTestShard]).
 */
internal class TimedTestSharding(timings: List<String>, taskPath: String, shardCount: Int) {
    private class ClassTime(val task: String, val className: String, val time: Long)

    private val classShards = HashMap<String, Int>()

    init {
        val listedClasses = HashSet<Pair<String, String>>()
        val classTimes = timings.withIndex().filter { it.value.isNotBlank() }.map { (index, line) ->
            val fields = line.split('\t')
            val time = fields.getOrNull(2)?.toLongOrNull()
            if (fields.size != 3 || time == null) {
                error("Invalid line ${index + 1} of the test shard timings: '$line'; Expected '<task path>\\t<class name>\\t<milliseconds>'")
            }
            if (!listedClasses.add(fields[0] to fields[1])) {
                error("Duplicate task and class in line ${index + 1} of the test shard timings: '$line'; Expected one line per task and class")
            }
            ClassTime(fields[0], fields[1], time)
        }

        val shardTimes = LongArray(shardCount)
        for (classTime in classTimes.sortedWith(compareByDescending<ClassTime> { it.time }.thenBy { it.task }.thenBy { it.className })) {
            val shard = shardTimes.indices.minBy { shardTimes[it] }
            shardTimes[shard] += classTime.time
            if (classTime.task == taskPath) classShards[classTime.className] = shard
        }
    }

    /** Returns the zero-based shard index of the class, or `null` if the timings do not list it for the task */
    fun shardFor(className: String): Int? = classShards[className]
}

/**
 * The [TimedTestSharding] of this JVM, read once from the [TestShardingConfiguration] of its system properties,
 * or `null` without timings.
 */
internal val timedTestSharding: TimedTestSharding? by lazy {
    val configuration = readTestShardingConfigurationFromSystemProperties()
    if (!configuration.isShardingEnabled) return@lazy null
    val timings = configuration.shardTimings ?: return@lazy null
    val taskPath = configuration.shardTask ?: return@lazy null
    TimedTestSharding(File(timings).readLines(), taskPath, configuration.shardCount)
}
