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
import kotlin.test.assertTrue
import kotlin.test.fail

class TestShardingFunctionalTest {

    @Test
    fun `junit5 - plain old Tests`() {
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
        val allTestsResult = runner.runTests().parseExecutedTests()
        assertEquals(8, allTestsResult.size)
        val singleShardResult = runner.runTests(currentShard = 1, totalShards = 1).parseExecutedTests()
        checkShardDistribution(allTestsResult, singleShardResult)
        val shard1Result = runner.runTests(currentShard = 1, totalShards = 3).parseExecutedTests()
        val shard2Result = runner.runTests(currentShard = 2, totalShards = 3).parseExecutedTests()
        val shard3Result = runner.runTests(currentShard = 3, totalShards = 3).parseExecutedTests()
        checkShardDistribution(allTestsResult, shard1Result, shard2Result, shard3Result)
    }


    @Test
    fun `junit5 - removing a shard preserves remaining test shard indices`() {
        junit5SourcesDirectory.resolve("Test.kt").writeCode(
            """
            import kotlin.test.Test

            class MyTest {
                ${(0 until 64).joinToString("\n") { "@Test fun test$it() = Unit" }}
            }
            """.trimIndent()
        )

        val runner = createGradleRunner()
        val allTests = runner.runTests().parseExecutedTests()
        assertEquals(64, allTests.size)

        var shards = (1..3).map { shard ->
            runner.runTests(currentShard = shard, totalShards = 3).parseExecutedTests()
        }
        checkShardDistribution(allTests, *shards.toTypedArray())
        assertEquals(allTests.size, shards.flatten().size)

        for (totalShards in 2 downTo 1) {
            val remainingShards = (1..totalShards).map { shard ->
                runner.runTests(currentShard = shard, totalShards = totalShards).parseExecutedTests()
            }
            checkShardDistribution(allTests, *remainingShards.toTypedArray())
            assertEquals(allTests.size, remainingShards.flatten().size)

            shards.dropLast(1).forEachIndexed { index, tests ->
                tests.forEach { test ->
                    assertTrue(
                        test in remainingShards[index],
                        "Expected $test to remain on shard ${index + 1} after removing shard ${totalShards + 1}"
                    )
                }
            }
            shards = remainingShards
        }
    }

    @Test
    fun `junit5 - TestFactory`() {
        junit5SourcesDirectory.resolve("TestFactoryTest.kt").writeCode(
            """
            import org.junit.jupiter.api.DynamicTest
            import org.junit.jupiter.api.TestFactory
            import java.util.stream.Stream
            import kotlin.streams.asStream

            class MyTestFactoryTest {

                private fun generateTests(): Stream<DynamicTest> = listOf("1", "2", "3")
                    .map { name -> DynamicTest.dynamicTest(name, { }) }
                    .asSequence().asStream()

                @TestFactory fun a() = generateTests()
                @TestFactory fun b() = generateTests()
                @TestFactory fun c() = generateTests()
                @TestFactory fun d() = generateTests()
                @TestFactory fun e() = generateTests()
                @TestFactory fun f() = generateTests()
                @TestFactory fun g() = generateTests()

            }
        """.trimIndent()
        )

        val runner = createGradleRunner()

        val allTests = runner.runTests().parseExecutedTests()
        assertEquals(21, allTests.size)
        val shard1 = runner.runTests(currentShard = 1, totalShards = 3).parseExecutedTests()
        val shard2 = runner.runTests(currentShard = 2, totalShards = 3).parseExecutedTests()
        val shard3 = runner.runTests(currentShard = 3, totalShards = 3).parseExecutedTests()
        checkShardDistribution(allTests, shard1, shard2, shard3)
    }

    @Test
    fun `junit5 - TestTemplate`() {
        junit5SourcesDirectory.resolve("TestTemplate.kt").writeCode(
            """
            import org.junit.jupiter.api.TestTemplate
            import org.junit.jupiter.api.extension.*
            import java.util.stream.Stream
            import kotlin.streams.asStream

            class MyContextProvider: TestTemplateInvocationContextProvider {
                override fun supportsTestTemplate(context: ExtensionContext): Boolean {
                    return true
                }

                override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
                      return listOf("a", "b", "c").map { name ->
                          object : TestTemplateInvocationContext {
                              override fun getDisplayName(invocationIndex: Int): String = name
                              override fun getAdditionalExtensions(): List<Extension> = emptyList()
                          }
                      }.asSequence().asStream()
                  }
            }

            class MyTestTemplateTest {
                  @TestTemplate @ExtendWith(MyContextProvider::class) fun a() = Unit
                  @TestTemplate @ExtendWith(MyContextProvider::class) fun b() = Unit
                  @TestTemplate @ExtendWith(MyContextProvider::class) fun c() = Unit
                  @TestTemplate @ExtendWith(MyContextProvider::class) fun d() = Unit
                  @TestTemplate @ExtendWith(MyContextProvider::class) fun e() = Unit
                  @TestTemplate @ExtendWith(MyContextProvider::class) fun f() = Unit
                  @TestTemplate @ExtendWith(MyContextProvider::class) fun g() = Unit
            }
           """.trimIndent()
        )

        val runner = createGradleRunner()
        val allTests = runner.runTests().parseExecutedTests()
        assertEquals(21, allTests.size)
        val shard1 = runner.runTests(currentShard = 1, totalShards = 3).parseExecutedTests()
        val shard2 = runner.runTests(currentShard = 2, totalShards = 3).parseExecutedTests()
        val shard3 = runner.runTests(currentShard = 3, totalShards = 3).parseExecutedTests()
        checkShardDistribution(allTests, shard1, shard2, shard3)
    }

    @Test
    fun `junit5 - parameterized tests`() {
        junit5SourcesDirectory.resolve("ParameterizedTests.kt").writeCode(
            """
            import org.junit.jupiter.params.ParameterizedTest
            import org.junit.jupiter.params.provider.ValueSource

            class MyParameterizedTest {
                @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun a(value: String) = Unit
                @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun b(value: String) = Unit
                @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun c(value: String) = Unit
                @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun d(value: String) = Unit
                @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun e(value: String) = Unit
                @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun f(value: String) = Unit
                @ParameterizedTest @ValueSource(strings = ["1", "2", "3"]) fun g(value: String) = Unit
            }
        """.trimIndent()
        )

        val runner = createGradleRunner()
        val allTests = runner.runTests().parseExecutedTests()
        assertEquals(21, allTests.size)
        val shard1 = runner.runTests(currentShard = 1, totalShards = 3).parseExecutedTests()
        val shard2 = runner.runTests(currentShard = 2, totalShards = 3).parseExecutedTests()
        val shard3 = runner.runTests(currentShard = 3, totalShards = 3).parseExecutedTests()
        checkShardDistribution(allTests, shard1, shard2, shard3)
    }

    @Test
    fun `junit5 - BeforeAll`() {
        checkShardingWithStaticAnnotation("BeforeAll")
    }

    @Test
    fun `junit5 - AfterAll`() {
        checkShardingWithStaticAnnotation("AfterAll")
    }

    @Test
    fun `junit5 - inherited BeforeAll`() {
        checkShardingWithStaticAnnotation("BeforeAll", inherited = true)
    }

    @Test
    fun `junit5 - inherited AfterAll`() {
        checkShardingWithStaticAnnotation("AfterAll", inherited = true)
    }

    private fun checkShardingWithStaticAnnotation(annotation: String, inherited: Boolean = false) {
        val lifecycle = """
            companion object {
                @JvmStatic
                @$annotation
                fun lifecycle() = Unit
            }
        """.trimIndent()

        if (inherited) {
            junit5SourcesDirectory.resolve("BaseTest.kt").writeCode(
                """
                import org.junit.jupiter.api.$annotation

                open class BaseTest {
                    $lifecycle
                }
                """.trimIndent()
            )
        }

        for (index in 1..8) {
            junit5SourcesDirectory.resolve("MyTest$index.kt").writeCode(
                """
                import kotlin.test.Test
                import org.junit.jupiter.api.$annotation

                class MyTest$index ${if (inherited) ": BaseTest()" else ""} {
                    ${if (inherited) "" else lifecycle}

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
        assertEquals(64, allTests.size)
        val singleShard = runner.runTests(currentShard = 1, totalShards = 1).parseExecutedTests()
        checkShardDistribution(allTests, singleShard)

        val shards = (1..3).map { shard ->
            runner.runTests(currentShard = shard, totalShards = 3).parseExecutedTests()
        }

        checkShardDistribution(allTests, *shards.toTypedArray())
        assertEquals(allTests.size, shards.flatten().size)
        allTests.groupBy { it.containerName }.forEach { entry ->
            val className = entry.key
            val tests = entry.value
            val shardsContainingClass = shards.filter { shard -> shard.any { it.containerName == className } }
            assertEquals(1, shardsContainingClass.size, "Expected $className to run on exactly one shard")
            assertEquals(tests.toSet(), shardsContainingClass.single().filter { it.containerName == className }.toSet())
        }
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
        val containerName: String, val testName: String, val status: String,
    )

    private fun GradleRunner.runTests(
        currentShard: Int? = null, totalShards: Int? = null,
    ): BuildResult {
        return withArguments(
            *listOfNotNull(
                "$targetProjectPath:junit5Tests",
                "--no-build-cache",
                if (currentShard != null) "-Ptests.currentShard=$currentShard" else null,
                if (totalShards != null) "-Ptests.totalShards=$totalShards" else null,

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
                containerName = match.groups["container"]?.value ?: error("Missing 'container'"),
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
