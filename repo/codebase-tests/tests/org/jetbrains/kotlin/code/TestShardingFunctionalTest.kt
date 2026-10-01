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
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * End-to-end check of the test sharding with nested Gradle builds:
 * the `-Ptests.*` properties reach the test JVM, the sharding filter is registered by the service loader,
 * and `tests.shardByMethod` is passed through.
 * The detailed sharding behaviour is tested by the fast tests in `repo/test-runtime/src/test`.
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
        val singleShardResult = runner.runTests(currentShard = 1, totalShards = 1, shardByMethod = true).parseExecutedTests()
        checkShardDistribution(allTestsResult, singleShardResult)
        val shard1Result = runner.runTests(currentShard = 1, totalShards = 3, shardByMethod = true).parseExecutedTests()
        val shard2Result = runner.runTests(currentShard = 2, totalShards = 3, shardByMethod = true).parseExecutedTests()
        val shard3Result = runner.runTests(currentShard = 3, totalShards = 3, shardByMethod = true).parseExecutedTests()
        checkShardDistribution(allTestsResult, shard1Result, shard2Result, shard3Result)
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
        val singleShard = runner.runTests(currentShard = 1, totalShards = 1).parseExecutedTests()
        checkShardDistribution(allTests, singleShard)

        val shards = (1..3).map { shard ->
            runner.runTests(currentShard = shard, totalShards = 3).parseExecutedTests()
        }

        checkShardDistribution(allTests, *shards.toTypedArray())
        assertEquals(allTests.size, shards.flatten().size)
        checkClassesAreNotSplit(allTests, *shards.toTypedArray())
    }

    /* Test Framework Code */

    private val targetProjectPath = ":repo:test-runtime"
    private val junit5ResultsDirectory = Path("repo/test-runtime/build/test-results/junit5Tests")
    private val junit5SourcesDirectory = Path("repo/test-runtime/src/junit5Tests/kotlin")

    @BeforeEach
    @AfterEach
    fun cleanup() {
        junit5ResultsDirectory.deleteRecursively()
        junit5SourcesDirectory.deleteRecursively()
    }

    data class TestResult(
        val className: String, val testName: String, val status: String,
    )

    /**
     * @param shardByMethod shards all test classes by their methods instead of keeping classes together
     */
    private fun GradleRunner.runTests(
        currentShard: Int? = null, totalShards: Int? = null, shardByMethod: Boolean = false,
    ): BuildResult {
        return withArguments(
            *listOfNotNull(
                "$targetProjectPath:junit5Tests",
                "--no-build-cache",
                if (currentShard != null) "-Ptests.currentShard=$currentShard" else null,
                if (totalShards != null) "-Ptests.totalShards=$totalShards" else null,
                if (shardByMethod) "-Ptests.shardByMethod=true" else null,

                /* Allow debugging of test Gradle process */
                *defaultGradleArguments(xmx = "1g").toTypedArray(),

                /* Allow debugging the nested test execution */
                if (ideaDebuggerDispatchPort != null) "-Ptests.additionalJvmArgument=" +
                        issueNewDebugSessionJvmArguments("Nested Test Execution").joinToString(" ") else null
            ).toTypedArray()
        ).build()
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
            if (shard.isEmpty()) fail("Shard ${index.plus(1)} does not contain any tests")
        }

        /* Test shards should not have overlapping tests */
        shards.withIndex().zipWithNext { a, b ->
            val aTests = a.value
            val bTests = b.value

            val intersection = aTests.intersect(bTests.toSet())
            if (intersection.isNotEmpty()) {
                fail(buildString {
                    appendLine("Shard ${a.index.plus(1)} and ${b.index.plus(1)} have ${intersection.size} tests in common")
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
