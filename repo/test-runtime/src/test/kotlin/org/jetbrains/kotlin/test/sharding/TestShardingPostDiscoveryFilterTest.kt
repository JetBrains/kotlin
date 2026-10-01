/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestShardingPostDiscoveryFilterTest {

    @Test
    fun `plain tests - shard by method`() {
        val allTests = executeTests(TestShardingConfiguration(shardByMethod = true), PlainTest::class.java)
        assertEquals(8, allTests.size)

        val singleShard = executeTests(
            TestShardingConfiguration(currentShard = 1, totalShards = 1, shardByMethod = true), PlainTest::class.java
        )
        checkShardDistribution(allTests, singleShard)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3, shardByMethod = true), PlainTest::class.java)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
    }

    @Test
    fun `plain tests - sharded by class`() {
        val allTests = executeTests(TestShardingConfiguration(), *plainTestClasses)
        assertEquals(12, allTests.size)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), *plainTestClasses)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
        checkClassesAreNotSplit(allTests, *shards.toTypedArray())
    }

    @Test
    fun `TestFactory - shard by method`() {
        val allTests = executeTests(TestShardingConfiguration(shardByMethod = true), TestFactoryTest::class.java)
        assertEquals(21, allTests.size)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3, shardByMethod = true), TestFactoryTest::class.java)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
    }

    @Test
    fun `TestTemplate - shard by method`() {
        val allTests = executeTests(TestShardingConfiguration(shardByMethod = true), TestTemplateTest::class.java)
        assertEquals(21, allTests.size)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3, shardByMethod = true), TestTemplateTest::class.java)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
    }

    @Test
    fun `parameterized tests - shard by method`() {
        val allTests = executeTests(TestShardingConfiguration(shardByMethod = true), ParameterizedTests::class.java)
        assertEquals(21, allTests.size)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3, shardByMethod = true), ParameterizedTests::class.java)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
    }

    @Test
    fun `TestFactory - sharded by class`() {
        val allTests = executeTests(TestShardingConfiguration(), *testFactoryClasses)
        assertEquals(36, allTests.size)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), *testFactoryClasses)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
        checkClassesAreNotSplit(allTests, *shards.toTypedArray())
    }

    @Test
    fun `parameterized tests - sharded by class`() {
        val allTests = executeTests(TestShardingConfiguration(), *parameterizedTestClasses)
        assertEquals(36, allTests.size)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), *parameterizedTestClasses)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
        checkClassesAreNotSplit(allTests, *shards.toTypedArray())
    }

    @Test
    fun `ShardByMethod annotation`() {
        val testClasses = arrayOf(ShardByMethodTest::class.java, *plainTestClasses)
        val allTests = executeTests(TestShardingConfiguration(), *testClasses)
        assertEquals(28, allTests.size)

        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), *testClasses)
        }
        checkShardDistribution(allTests, *shards.toTypedArray())

        /* Tests of the annotated class are distributed individually */
        shards.forEachIndexed { index, shard ->
            assertTrue(
                shard.any { it.className == ShardByMethodTest::class.java.name },
                "Expected shard ${index + 1} to contain tests of 'ShardByMethodTest'"
            )
        }

        /* Tests of classes without the annotation are kept together */
        checkClassesAreNotSplit(allTests.filter { it.className != ShardByMethodTest::class.java.name }, *shards.toTypedArray())
    }

    @Test
    fun `removing a shard preserves the remaining shards`() {
        val keys = (0 until 64).map { "test$it".encodeToByteArray() }
        val threeShards = keys.map { key -> calculateTestShard(key, totalShards = 3, shardSeed = 0) }
        val twoShards = keys.map { key -> calculateTestShard(key, totalShards = 2, shardSeed = 0) }
        val oneShard = keys.map { key -> calculateTestShard(key, totalShards = 1, shardSeed = 0) }

        assertEquals(setOf(1, 2, 3), threeShards.toSet())
        assertEquals(setOf(1, 2), twoShards.toSet())
        assertEquals(setOf(1), oneShard.toSet())

        keys.indices.forEach { index ->
            if (threeShards[index] <= 2) {
                assertEquals(threeShards[index], twoShards[index], "Expected key $index to remain on shard ${threeShards[index]}")
            }
        }
    }

    @Tag(testFixtureTag)
    class PlainTest {
        @Test fun a() = Unit
        @Test fun b() = Unit
        @Test fun c() = Unit
        @Test fun d() = Unit
        @Test fun e() = Unit
        @Test fun f() = Unit
        @Test fun g() = Unit
        @Test fun h() = Unit
    }

    @Tag(testFixtureTag)
    class PlainTest1 {
        @Test fun a() = Unit
        @Test fun b() = Unit
    }

    @Tag(testFixtureTag)
    class PlainTest2 {
        @Test fun a() = Unit
        @Test fun b() = Unit
    }

    @Tag(testFixtureTag)
    class PlainTest3 {
        @Test fun a() = Unit
        @Test fun b() = Unit
    }

    @Tag(testFixtureTag)
    class PlainTest4 {
        @Test fun a() = Unit
        @Test fun b() = Unit
    }

    @Tag(testFixtureTag)
    class PlainTest5 {
        @Test fun a() = Unit
        @Test fun b() = Unit
    }

    @Tag(testFixtureTag)
    class PlainTest6 {
        @Test fun a() = Unit
        @Test fun b() = Unit
    }

    private val plainTestClasses = arrayOf(
        PlainTest1::class.java, PlainTest2::class.java, PlainTest3::class.java,
        PlainTest4::class.java, PlainTest5::class.java, PlainTest6::class.java,
    )

    @Tag(testFixtureTag)
    @ShardByMethod
    class ShardByMethodTest {
        @Test fun a() = Unit
        @Test fun b() = Unit
        @Test fun c() = Unit
        @Test fun d() = Unit
        @Test fun e() = Unit
        @Test fun f() = Unit
        @Test fun g() = Unit
        @Test fun h() = Unit
        @Test fun i() = Unit
        @Test fun j() = Unit
        @Test fun k() = Unit
        @Test fun l() = Unit
        @Test fun m() = Unit
        @Test fun n() = Unit
        @Test fun o() = Unit
        @Test fun p() = Unit
    }

    @Tag(testFixtureTag)
    class TestFactoryTest {
        @TestFactory fun a() = generateTests()
        @TestFactory fun b() = generateTests()
        @TestFactory fun c() = generateTests()
        @TestFactory fun d() = generateTests()
        @TestFactory fun e() = generateTests()
        @TestFactory fun f() = generateTests()
        @TestFactory fun g() = generateTests()
    }

    @Tag(testFixtureTag)
    class TestFactoryTest1 {
        @TestFactory fun a() = generateTests()
        @TestFactory fun b() = generateTests()
    }

    @Tag(testFixtureTag)
    class TestFactoryTest2 {
        @TestFactory fun a() = generateTests()
        @TestFactory fun b() = generateTests()
    }

    @Tag(testFixtureTag)
    class TestFactoryTest3 {
        @TestFactory fun a() = generateTests()
        @TestFactory fun b() = generateTests()
    }

    @Tag(testFixtureTag)
    class TestFactoryTest4 {
        @TestFactory fun a() = generateTests()
        @TestFactory fun b() = generateTests()
    }

    @Tag(testFixtureTag)
    class TestFactoryTest5 {
        @TestFactory fun a() = generateTests()
        @TestFactory fun b() = generateTests()
    }

    @Tag(testFixtureTag)
    class TestFactoryTest6 {
        @TestFactory fun a() = generateTests()
        @TestFactory fun b() = generateTests()
    }

    private val testFactoryClasses = arrayOf(
        TestFactoryTest1::class.java, TestFactoryTest2::class.java, TestFactoryTest3::class.java,
        TestFactoryTest4::class.java, TestFactoryTest5::class.java, TestFactoryTest6::class.java,
    )

    class ContextProvider : TestTemplateInvocationContextProvider {
        override fun supportsTestTemplate(context: ExtensionContext): Boolean = true

        override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
            return listOf("1", "2", "3").map<String, TestTemplateInvocationContext> { name ->
                object : TestTemplateInvocationContext {
                    override fun getDisplayName(invocationIndex: Int): String = name
                }
            }.stream()
        }
    }

    @Tag(testFixtureTag)
    class TestTemplateTest {
        @TestTemplate @ExtendWith(ContextProvider::class) fun a() = Unit
        @TestTemplate @ExtendWith(ContextProvider::class) fun b() = Unit
        @TestTemplate @ExtendWith(ContextProvider::class) fun c() = Unit
        @TestTemplate @ExtendWith(ContextProvider::class) fun d() = Unit
        @TestTemplate @ExtendWith(ContextProvider::class) fun e() = Unit
        @TestTemplate @ExtendWith(ContextProvider::class) fun f() = Unit
        @TestTemplate @ExtendWith(ContextProvider::class) fun g() = Unit
    }

    @Tag(testFixtureTag)
    class ParameterizedTests {
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun c(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun d(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun e(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun f(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun g(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class ParameterizedTest1 {
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class ParameterizedTest2 {
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class ParameterizedTest3 {
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class ParameterizedTest4 {
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class ParameterizedTest5 {
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class ParameterizedTest6 {
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(@Suppress("unused") value: String) = Unit
        @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(@Suppress("unused") value: String) = Unit
    }

    private val parameterizedTestClasses = arrayOf(
        ParameterizedTest1::class.java, ParameterizedTest2::class.java, ParameterizedTest3::class.java,
        ParameterizedTest4::class.java, ParameterizedTest5::class.java, ParameterizedTest6::class.java,
    )
}

private fun generateTests(): List<DynamicTest> = listOf("1", "2", "3").map { name -> dynamicTest(name) { } }
