/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import java.io.File
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Recorded execution times of the test classes of one test task, used to balance its shards by duration instead of by hash.
 * The Gradle test tasks pass `<project directory>/test-shard-timings/<task name>.tsv` (`tests.shardTimings`) when it exists.
 *
 * The file has one line per test class, separated by tabs:
 * ```
 * # comment
 * <class>[#<method>]	<milliseconds>
 * ```
 * - `<class>`: the binary name of the test's own class (e.g. `org.example.OuterTest$Nested`),
 * - `#<method>`: optionally, a test method of the class (its JVM name, without parameters).
 *   Splits a class that alone would be longer than a shard: the listed methods are assigned on their own,
 *   the other methods of the class with the `<class>` line, if there is one.
 *
 * Classes and methods in the file are assigned whole, by [assign]: a test template or factory is not split further.
 * Classes missing from the file (e.g. new classes) keep the hash-based sharding, see [calculateTestShard].
 */
internal class TestShardTimings(
    /** Milliseconds by class (or class and method, as `<class>#<method>`) */
    val durations: Map<String, Long>,
) {
    private val assignments = ConcurrentHashMap<Pair<Int, String>, Map<String, Int>>()

    /**
     * The (1-based) shard of the test method [methodName] of [className], by the duration of the method or else of its class,
     * or `null` if neither has a recorded duration. [salt] identifies the task, see [assign].
     */
    fun shardOf(className: String, methodName: String?, totalShards: Int, salt: String, seed: Int): Int? {
        val assignment = assignments.computeIfAbsent(totalShards to salt) { assign(durations, totalShards, salt.encodeToByteArray(), seed) }
        return methodName?.let { assignment["$className#$it"] } ?: assignment[className]
    }

    companion object {
        private val cache = ConcurrentHashMap<String, TestShardTimings>()

        /** Reads the file once per JVM, as every test descriptor asks for its shard. A missing file has no timings. */
        fun load(path: String): TestShardTimings = cache.computeIfAbsent(File(path).absolutePath) { parse(File(it)) }

        fun parse(file: File): TestShardTimings {
            if (!file.isFile) return TestShardTimings(emptyMap())
            val durations = mutableMapOf<String, Long>()
            file.readLines().forEachIndexed { index, line ->
                if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                val parts = line.split('\t')
                val milliseconds = parts.getOrNull(1)?.toLongOrNull()
                if (parts.size != 2 || milliseconds == null || milliseconds < 0) {
                    error("Malformed line ${index + 1} in '$file': '$line'. Expected '<class>[#<method>]\\t<milliseconds>'")
                }
                durations[parts[0]] = (durations[parts[0]] ?: 0) + milliseconds
            }
            return TestShardTimings(durations)
        }

        /**
         * Longest processing time first: each entry, from the longest to the shortest, goes to the shard with the least total duration so far.
         * This keeps the longest shard within 4/3 of the optimum, and in practice close to the average.
         *
         * Each task is balanced on its own, so shards with equal loads must not be chosen in the same order by every task:
         * the first entries of all small tasks would pile up on the first shard. Equal loads are ordered by the rendezvous score
         * of the shard for the task ([salt]), as in [calculateTestShard]. Equal durations are ordered by name.
         * Every shard computes the same assignment independently, as nothing but the file, the salt and the seed decides it.
         */
        fun assign(durations: Map<String, Long>, totalShards: Int, salt: ByteArray = byteArrayOf(), seed: Int = 0): Map<String, Int> {
            if (totalShards < 1) error("Invalid 'totalShards': $totalShards; Expected >= 1")
            class Shard(val id: Int, val rank: ULong, var load: Long)

            val shards = PriorityQueue<Shard>(compareBy<Shard> { it.load }.thenByDescending { it.rank }.thenBy { it.id })
            for (shard in 1..totalShards) shards.add(Shard(shard, calculateShardScore(shard, salt, seed), 0L))

            val assignment = HashMap<String, Int>(durations.size * 2)
            for ((key, milliseconds) in durations.entries.sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })) {
                val shard = shards.poll()
                assignment[key] = shard.id
                shard.load += milliseconds
                shards.add(shard)
            }
            return assignment
        }
    }
}
