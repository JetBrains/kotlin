/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.kotlin.code

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.testFederation.TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY
import org.jetbrains.kotlin.testFederation.TEST_FEDERATION_ENABLED_ENV_KEY
import org.jetbrains.kotlin.testFederation.TEST_FEDERATION_MODE_ENV_KEY
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * End-to-end check of the test sharding with nested Gradle builds:
 * the `-Pkotlin.build.test.shard.*` properties reach the test JVM, the sharding filter is registered by the service loader,
 * `tests.shardByMethod` is passed through, and a shard without tests passes.
 * The detailed sharding behaviour is tested by the fast tests in `repo/test-runtime/src/test`,
 * except for the timings, which are read once per test JVM (see `TimedTestSharding`).
 */
class TestShardingFunctionalTest {

    @Test
    fun `junit5 - plain old Tests - shard by method enabled`() {
        junit5SourcesDirectory.resolve("Test.kt").writeCode(
            """
            import kotlin.test.Test

            class MyTest {
                @Test fun a() = Unit
                @Test fun b() = Unit
                @Test fun c() = Unit
                @Test fun d() = Unit
                @Test fun e() = Unit
                @Test fun f() = Unit
                @Test fun g() = Unit
                @Test fun h() = Unit
            }
        """.trimIndent()
        )

        val runner = createGradleRunner()
        val allTestsResult = runner.runTests(shardByMethod = true).parseExecutedTests()
        assertEquals(8, allTestsResult.size)
        val singleShardResult = runner.runTests(shardIndex = 0, shardCount = 1, shardByMethod = true).parseExecutedTests()
        checkShardDistribution(allTestsResult, singleShardResult)
        val shard0Result = runner.runTests(shardIndex = 0, shardCount = 3, shardByMethod = true).parseExecutedTests()
        val shard1Result = runner.runTests(shardIndex = 1, shardCount = 3, shardByMethod = true).parseExecutedTests()
        val shard2Result = runner.runTests(shardIndex = 2, shardCount = 3, shardByMethod = true).parseExecutedTests()
        checkShardDistribution(allTestsResult, shard0Result, shard1Result, shard2Result)
    }

    @Test
    fun `junit5 - plain old Tests - sharded by class`() {
        for (index in 1..10) {
            junit5SourcesDirectory.resolve("MyTest$index.kt").writeCode(
                """
                import kotlin.test.Test

                class MyTest$index {
                    @Test fun a() = Unit
                    @Test fun b() = Unit
                    @Test fun c() = Unit
                    @Test fun d() = Unit
                    @Test fun e() = Unit
                    @Test fun f() = Unit
                    @Test fun g() = Unit
                    @Test fun h() = Unit
                }
                """.trimIndent()
            )
        }

        val runner = createGradleRunner()
        val allTests = runner.runTests().parseExecutedTests()
        assertEquals(80, allTests.size)
        val singleShard = runner.runTests(shardIndex = 0, shardCount = 1).parseExecutedTests()
        checkShardDistribution(allTests, singleShard)

        val shards = (0 until 3).map { shard ->
            runner.runTests(shardIndex = shard, shardCount = 3).parseExecutedTests()
        }

        checkShardDistribution(allTests, *shards.toTypedArray())
        assertEquals(allTests.size, shards.flatten().size)
        checkClassesAreNotSplit(allTests, *shards.toTypedArray())
    }

    @Test
    fun `gradle sharding properties are passed to the runtime`() {
        junit5SourcesDirectory.resolve("Test.kt").writeCode(
            """
            import kotlin.test.Test
            import kotlin.test.assertEquals

            class MyTest {
                @Test fun configuration() {
                    assertEquals("0", System.getProperty("kotlin.build.test.shard.index"))
                    assertEquals("1", System.getProperty("kotlin.build.test.shard.count"))
                    assertEquals("42", System.getProperty("kotlin.build.test.shard.seed"))
                }
            }
            """.trimIndent()
        )

        val result = createGradleRunner().runTests(shardIndex = 0, shardCount = 1, shardSeed = 42)
        assertEquals(1, result.parseExecutedTests().size)
        assertContains(result.output, "Running tests in shard index 0 (shard count: 1)")
    }

    @Test
    fun `test shard timings keep every listed class on its shard`() {
        shardTimingsFile.createParentDirectories().writeText(
            listOf("Templates" to 100, "MyTest1" to 60, "MyTest2" to 50, "Outer\$Inner" to 40, "MyTest3" to 10, "Outer" to 5)
                .joinToString("") { [className, time] -> "$targetProjectPath:junit5Tests\t$className\t$time\n" }
        )
        for (index in 1..3) {
            junit5SourcesDirectory.resolve("MyTest$index.kt").writeCode(
                """
                import kotlin.test.Test

                class MyTest$index {
                    @Test fun a() = Unit
                    @Test fun b() = Unit
                    @Test fun c() = Unit
                    @Test fun d() = Unit
                    @Test fun e() = Unit
                    @Test fun f() = Unit
                    @Test fun g() = Unit
                    @Test fun h() = Unit
                }
                """.trimIndent()
            )
        }
        junit5SourcesDirectory.resolve("Outer.kt").writeCode(
            """
            import kotlin.test.Test
            import org.junit.jupiter.api.Nested

            class Outer {
                @Test fun a() = Unit

                @Nested
                inner class Inner {
                    @Test fun b() = Unit
                }
            }
            """.trimIndent()
        )
        junit5SourcesDirectory.resolve("Templates.kt").writeCode(
            """
            import org.jetbrains.kotlin.test.sharding.ParameterizedTestSharding
            import org.jetbrains.kotlin.test.sharding.shard
            import org.junit.jupiter.api.extension.ExtensionContext
            import org.junit.jupiter.params.ParameterizedTest
            import org.junit.jupiter.params.provider.Arguments
            import org.junit.jupiter.params.provider.ArgumentsProvider
            import org.junit.jupiter.params.provider.ArgumentsSource
            import org.junit.jupiter.params.support.ParameterDeclarations

            class Templates {
                @ParameterizedTest
                @ParameterizedTestSharding
                @ArgumentsSource(Values::class)
                fun test(value: String) = Unit

                class Values : ArgumentsProvider {
                    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext) =
                        listOf("a", "b", "c", "d", "e", "f").map { Arguments.of(it) }.shard(context).toList().stream()
                }
            }
            """.trimIndent()
        )

        val runner = createGradleRunner()
        val allTests = runner.runTests().parseExecutedTests()
        /* Sharding by method would split every class: the timings keep them whole, with a nested class on a shard of its own */
        val shards = (0 until 3).map { shard ->
            runner.runTests(shardIndex = shard, shardCount = 3, shardByMethod = true, shardTimings = shardTimingsFile.nameWithoutExtension)
        }
        assertContains(
            shards[0].output,
            "Running tests in shard index 0 (shard count: 3), by the timings of 'repo/test-timings/${shardTimingsFile.name}'",
        )

        val shardTests = shards.map { it.parseExecutedTests() }
        checkShardDistribution(allTests, *shardTests.toTypedArray())
        assertEquals(
            listOf(setOf("Templates > test(String)"), setOf("MyTest1", "MyTest3", "Outer"), setOf("MyTest2", "Outer > Inner")),
            shardTests.map { shard -> shard.map { it.className }.toSet() },
        )
    }

    @Test
    fun `missing test shard timings fall back to hashing`() {
        junit5SourcesDirectory.resolve("Test.kt").writeCode(
            """
            import kotlin.test.Test
            import kotlin.test.assertNull

            class MyTest {
                @Test fun configuration() {
                    assertNull(System.getProperty("kotlin.build.test.shard.timings"))
                }
            }
            """.trimIndent()
        )

        val result = createGradleRunner().runTests(shardIndex = 0, shardCount = 1, shardTimings = shardTimingsFile.nameWithoutExtension)
        assertEquals(1, result.parseExecutedTests().size)
        assertContains(result.output, "by hash, missing 'repo/test-timings/${shardTimingsFile.name}'")
    }

    @Test
    fun `a shard without tests passes`() {
        junit5SourcesDirectory.resolve("Test.kt").writeCode(
            """
            import kotlin.test.Test

            class MyTest {
                @Test fun test() = Unit
            }
            """.trimIndent()
        )

        /* A single class leaves one of the shards empty; nightly builds set no tag filters, so Gradle checks that tests are discovered */
        val runner = createGradleRunner()
        val shards = (0 until 2).map { shard -> runner.runTests(shardIndex = shard, shardCount = 2, nightly = true).parseExecutedTests() }
        assertEquals(listOf(0, 1), shards.map { it.size }.sorted())
    }

    @Test
    fun `invalid shard indices and counts are rejected`() {
        junit5SourcesDirectory.resolve("Test.kt").writeCode(
            """
            import kotlin.test.Test

            class MyTest {
                @Test fun test() = Unit
            }
            """.trimIndent()
        )

        val runner = createGradleRunner()
        for (shardIndex in listOf(-1, 3)) {
            val result = runner.runTests(shardIndex = shardIndex, shardCount = 3, expectFailure = true)
            assertContains(result.output, "kotlin.build.test.shard.index")
        }
        for (shardCount in listOf(0, -1)) {
            val result = runner.runTests(shardIndex = 0, shardCount = shardCount, expectFailure = true)
            assertContains(result.output, "kotlin.build.test.shard.count")
        }
    }

    /* Test Framework Code */

    private val targetProjectPath = ":repo:test-runtime"
    private val junit5ResultsDirectory = Path("repo/test-runtime/build/test-results/junit5Tests")
    private val junit5SourcesDirectory = Path("repo/test-runtime/src/junit5Tests/kotlin")
    private val shardTimingsFile = Path("repo/test-timings/TestShardingFunctionalTest.tsv")

    @BeforeEach
    @AfterEach
    fun cleanup() {
        junit5ResultsDirectory.deleteRecursively()
        junit5SourcesDirectory.deleteRecursively()
        shardTimingsFile.deleteIfExists()
    }

    data class TestResult(
        val className: String, val testName: String, val status: String,
    )

    /**
     * @param shardIndex the zero-based index of the shard to run
     * @param shardByMethod shards all test classes by their methods instead of keeping classes together
     * @param nightly sets `-Pnightly`: when `false`, the nightly tests are excluded by a tag filter
     */
    private fun GradleRunner.runTests(
        shardIndex: Int? = null, shardCount: Int? = null, shardByMethod: Boolean = false,
        shardSeed: Int? = null, shardTimings: String? = null, nightly: Boolean? = null, expectFailure: Boolean = false,
    ): BuildResult {
        withArguments(
            *listOfNotNull(
                "$targetProjectPath:junit5Tests",
                "--no-build-cache",
                if (shardIndex != null) "-Pkotlin.build.test.shard.index=$shardIndex" else null,
                if (shardCount != null) "-Pkotlin.build.test.shard.count=$shardCount" else null,
                if (shardSeed != null) "-Pkotlin.build.test.shard.seed=$shardSeed" else null,
                if (shardByMethod) "-Ptests.shardByMethod=true" else null,
                if (shardTimings != null) "-Pkotlin.build.test.shard.timings=$shardTimings" else null,
                if (nightly != null) "-Pnightly=$nightly" else null,

                /* Allow debugging of test Gradle process */
                *defaultGradleArguments(xmx = "1g").toTypedArray(),

                /* Allow debugging the nested test execution */
                if (ideaDebuggerDispatchPort != null) "-Ptests.additionalJvmArgument=" +
                        issueNewDebugSessionJvmArguments("Nested Test Execution").joinToString(" ") else null
            ).toTypedArray()
        )
        return if (expectFailure) buildAndFail() else build()
    }

    private fun Path.writeCode(@Language("kotlin") code: String) {
        createParentDirectories()
        writeText(code)
    }

    private fun BuildResult.parseExecutedTests(): List<TestResult> {
        val pattern = Regex("""(?<container>.*) > (?<test>.*) (?<status>[A-Z]+)""")
        return outputReader.lineSequence().mapNotNull { line ->
            val match = pattern.matchEntire(line) ?: return@mapNotNull null
            TestResult(
                className = match.groups["container"]?.value ?: error("Missing 'container'"),
                testName = match.groups["test"]?.value ?: error("Missing 'test'"),
                status = match.groups["status"]?.value ?: error("Missing 'status'")
            )
        }.toList()
    }

    private fun checkShardDistribution(all: List<TestResult>, vararg shards: List<TestResult>) {
        /* Test is suspicious if a shard has no tests */
        shards.forEachIndexed { index, shard ->
            if (shard.isEmpty()) fail("Shard $index does not contain any tests")
        }

        /* Test shards should not have overlapping tests */
        shards.withIndex().zipWithNext().forEach { [a, b] ->
            val aTests = a.value
            val bTests = b.value

            val intersection = aTests.intersect(bTests.toSet())
            if (intersection.isNotEmpty()) {
                fail(buildString {
                    appendLine("Shard ${a.index} and ${b.index} have ${intersection.size} tests in common")
                    appendLine("    Common tests: ${intersection.joinToString(", ") { it.testName }}")
                })
            }
        }

        /* Test shards should execute all tests */
        assertEquals(
            all.toSet(), shards.toList().flatten().toSet(),
            "Expected all tests to be distributed across shards without duplicates"
        )
    }

    /**
     * Checks that all tests of a class are executed on exactly one shard
     */
    private fun checkClassesAreNotSplit(all: List<TestResult>, vararg shards: List<TestResult>) {
        all.groupBy { it.className }.forEach { entry ->
            val className = entry.key
            val tests = entry.value
            val shardsContainingClass = shards.filter { shard -> shard.any { it.className == className } }
            assertEquals(1, shardsContainingClass.size, "Expected $className to run on exactly one shard")
            assertEquals(tests.toSet(), shardsContainingClass.single().filter { it.className == className }.toSet())
        }
    }

    private fun createGradleRunner(
        environment: Map<String, String> = defaultEnv(),
    ): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(Path("").toAbsolutePath().toFile())
            .withEnvironment(environment)
            .forwardOutput()
            .withTestKitDir(File(System.getProperty("gradle.user.home") ?: error("Missing 'gradle.user.home'")))
    }

    private fun defaultEnv(): Map<String, String> {
        return System.getenv().toMutableMap().apply {
            remove(TEST_FEDERATION_ENABLED_ENV_KEY)
            remove(TEST_FEDERATION_MODE_ENV_KEY)
            remove(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)
        }
    }
}
