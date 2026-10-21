/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Assigns test classes to shards by their recorded times, so that the shards take equally long.
 *
 * The [timings] list the test classes of all test tasks of a build, one `<task path>\t<class name>\t<milliseconds>` per line
 * (as written by the `test-inventory` tool of kotlin-infrastructure).
 * The classes go to shards longest first, each to the shard with the least time so far (the lowest one on a tie).
 * Classes of all tasks are assigned together, because a shard runs all of its tasks.
 * Every shard computes the same assignment from the same timings.
 *
 * All tests of a class go to its shard, including the invocations of a [ParameterizedTestSharding] template.
 * Classes missing from the timings are sharded by hash (see [calculateTestShard]).
 */
internal class TimedTestSharding(timings: List<String>, taskPath: String, shardCount: Int) {
    private val classShards = HashMap<String, Int>()

    init {
        val shardTimes = LongArray(shardCount)
        timings.filter { it.isNotBlank() }
            .map { it.split('\t') }
            .sortedWith(compareByDescending<List<String>> { it[2].toLong() }.thenBy { it[0] }.thenBy { it[1] })
            .forEach { (task, className, time) ->
                val shard = shardTimes.indices.minBy { shardTimes[it] }
                shardTimes[shard] += time.toLong()
                if (task == taskPath) classShards[className] = shard
            }
    }

    /** Returns the zero-based shard index of the class, or `null` if the timings do not list it for the task */
    fun shardFor(className: String): Int? = classShards[className]
}

private val timedTestShardings = ConcurrentHashMap<TestShardingConfiguration, TimedTestSharding>()

/** The [TimedTestSharding] of the [TestShardingConfiguration.shardTimings], read once per JVM, or `null` without timings */
internal val TestShardingConfiguration.timedTestSharding: TimedTestSharding?
    get() {
        if (!isShardingEnabled || shardTimings == null || shardTask == null) return null
        return timedTestShardings.computeIfAbsent(this) { TimedTestSharding(File(shardTimings).readLines(), shardTask, shardCount) }
    }
