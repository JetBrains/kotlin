/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.jetbrains.kotlin.test.sharding.ParameterizedTestShardingTest.ShardingArgumentsProvider
import org.jetbrains.kotlin.test.sharding.TestShardingPostDiscoveryFilterTest.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.platform.launcher.TestIdentifier
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestShardTimingsTest {

    private val task = ":repo:test-runtime:test"

    @Test
    fun `longest entries first, each to the least loaded shard`() {
        val durations = mapOf("A" to 50L, "B" to 40L, "C" to 30L, "D" to 20L, "E" to 15L, "F" to 5L)
        for (salt in listOf(":a:test", ":b:test", ":c:test")) {
            val assignment = TestShardTimings.assign(durations, totalShards = 3, salt.encodeToByteArray())

            /* A, B, C on their own shards; D joins C (30), E joins B (40), F joins A (50, tied with C + D and ordered by the salt) or C + D */
            val groups = assignment.entries.groupBy({ it.value }, { it.key }).values.map { it.toSet() }.toSet()
            assertTrue(
                groups == setOf(setOf("A", "F"), setOf("B", "E"), setOf("C", "D")) || groups == setOf(setOf("A"), setOf("B", "E"), setOf("C", "D", "F")),
                "Unexpected assignment for '$salt': $assignment",
            )
        }
    }

    @Test
    fun `equal loads are ordered by the task`() {
        /* Every task puts its only class on the first shard of its own order, instead of all on shard 1 */
        val shards = (1..64).map { TestShardTimings.assign(mapOf("A" to 1L), totalShards = 8, ":task$it".encodeToByteArray()).getValue("A") }
        assertEquals((1..8).toSet(), shards.toSet())
    }

    @Test
    fun `assignment does not depend on the order of the file`() {
        val durations = (1..200).associate { "Class$it" to (it % 7) * 1000L }
        val shuffled = durations.entries.shuffled(kotlin.random.Random(42)).associate { it.key to it.value }

        assertEquals(TestShardTimings.assign(durations, 5, task.encodeToByteArray()), TestShardTimings.assign(shuffled, 5, task.encodeToByteArray()))
    }

    @Test
    fun `parses comments and sums repeated classes`(@TempDir dir: Path) {
        val file = dir.resolve("timings.tsv")
        file.writeText(
            """
            # recorded by build 1
            org.example.A	100
            org.example.A	20

            org.example.B${'$'}Nested#test	7
            """.trimIndent()
        )

        assertEquals(mapOf("org.example.A" to 120L, "org.example.B\$Nested#test" to 7L), TestShardTimings.parse(file.toFile()).durations)
    }

    @Test
    fun `malformed lines fail`(@TempDir dir: Path) {
        val file = dir.resolve("timings.tsv").apply { writeText("org.example.A 100") }
        assertFailsWith<IllegalStateException> { TestShardTimings.parse(file.toFile()) }
    }

    @Test
    fun `a missing file has no timings`(@TempDir dir: Path) {
        val configuration = TestShardingConfiguration(currentShard = 1, totalShards = 2, timingsFile = dir.resolve("missing.tsv").toString(), task = task)
        assertNull(configuration.timedShardOf(PlainTest1::class.java.name, methodName = null))
    }

    @Test
    fun `timed classes run on their shard and untimed classes fall back to hashing`(@TempDir dir: Path) {
        val testClasses = arrayOf(
            PlainTest1::class.java, PlainTest2::class.java, PlainTest3::class.java,
            PlainTest4::class.java, PlainTest5::class.java, PlainTest6::class.java,
        )
        /* PlainTest5 and PlainTest6 have no recorded duration */
        val timings = writeTimings(
            dir,
            PlainTest1::class.java to 100, PlainTest2::class.java to 60, PlainTest3::class.java to 50, PlainTest4::class.java to 10,
        )
        val configuration = TestShardingConfiguration(timingsFile = timings.path, task = task)

        val allTests = executeTests(configuration, *testClasses)
        val shards = (1..2).map { shard -> executeTests(configuration.copy(currentShard = shard, totalShards = 2), *testClasses) }
        checkShardDistribution(allTests, *shards.toTypedArray())
        checkClassesAreNotSplit(allTests, *shards.toTypedArray())

        /* 100 and 10 on one shard, 60 and 50 on the other */
        assertEquals(
            setOf(setOf(PlainTest1::class.java.name, PlainTest4::class.java.name), setOf(PlainTest2::class.java.name, PlainTest3::class.java.name)),
            shards.map { it.timedClasses() }.toSet(),
        )

        /* The fallback is the hash-based assignment */
        val hashed = (1..2).map { shard -> executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 2), *testClasses) }
        for (untimed in listOf(PlainTest5::class.java.name, PlainTest6::class.java.name)) {
            assertEquals(hashed.indexOfFirst { it.any { test -> test.className == untimed } }, shards.indexOfFirst { it.any { test -> test.className == untimed } })
        }
    }

    @Test
    fun `timed classes are not split by dynamic or parameterized sharding`(@TempDir dir: Path) {
        val testClasses = arrayOf(TimedFactory::class.java, TimedParameterizedTest::class.java)
        val timings = writeTimings(dir, TimedFactory::class.java to 10, TimedParameterizedTest::class.java to 10)
        val configuration = TestShardingConfiguration(timingsFile = timings.path, task = task)

        val allTests = executeTests(configuration, *testClasses)
        assertEquals(6 + 6, allTests.size)

        val shards = (1..2).map { shard -> executeTests(configuration.copy(currentShard = shard, totalShards = 2), *testClasses) }
        checkShardDistribution(allTests, *shards.toTypedArray())
        checkClassesAreNotSplit(allTests, *shards.toTypedArray())
        /* Without timings, the factory and the templates would spread their tests over both shards */
        shards.forEach { shard -> assertEquals(1, shard.map { it.className }.distinct().size, "Expected one whole class per shard") }
    }

    @Test
    fun `methods with a recorded duration are split from their class`(@TempDir dir: Path) {
        val className = PlainTest::class.java.name
        val timings = dir.resolve("timings.tsv").toFile()
        /* 'a' and 'b' alone are longer than the other six methods together */
        timings.writeText("$className#a\t100\n$className#b\t100\n$className\t60")
        val configuration = TestShardingConfiguration(timingsFile = timings.path, task = task)

        val allTests = executeTests(configuration, PlainTest::class.java)
        val shards = (1..3).map { shard -> executeTests(configuration.copy(currentShard = shard, totalShards = 3), PlainTest::class.java) }
        checkShardDistribution(allTests, *shards.toTypedArray())

        /* 'a', 'b' and the rest of the class on their own shards */
        assertEquals(
            setOf(listOf("a()"), listOf("b()"), listOf("c()", "d()", "e()", "f()", "g()", "h()")),
            shards.map { shard -> shard.map { it.displayName }.sorted() }.toSet(),
        )
    }

    private fun writeTimings(dir: Path, vararg durations: Pair<Class<*>, Long>): File {
        val file = dir.resolve("timings-${durations.hashCode()}.tsv").toFile()
        file.writeText(durations.joinToString("\n") { (testClass, milliseconds) -> "${testClass.name}\t$milliseconds" })
        return file
    }

    private fun List<TestIdentifier>.timedClasses(): Set<String> {
        val timed = listOf(PlainTest1::class.java, PlainTest2::class.java, PlainTest3::class.java, PlainTest4::class.java).map { it.name }
        return map { it.className }.filter { it in timed }.toSet()
    }

    @Tag(testFixtureTag)
    class TimedFactory {
        @TestFactory
        @DynamicTestSharding
        context(_: DynamicTestShardingContext)
        fun tests(): List<DynamicTest> = (1..6).map { "case $it" }.shardBy { it }.map { name -> dynamicTest(name) {} }
    }

    @Tag(testFixtureTag)
    class TimedParameterizedTest {
        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(ShardingArgumentsProvider::class)
        fun first(@Suppress("unused") value: String) = Unit

        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(ShardingArgumentsProvider::class)
        fun second(@Suppress("unused") value: String) = Unit
    }
}
