/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalTestFederationApi::class, ExperimentalPathApi::class)

package org.jetbrains.kotlin.code


import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.code.TestBatchesFunctionalTest.JunitTarget.JUnit4
import org.jetbrains.kotlin.code.TestBatchesFunctionalTest.JunitTarget.Junit5
import org.jetbrains.kotlin.code.utils.ideaDebuggerDispatchPort
import org.jetbrains.kotlin.code.utils.issueNewDebugSessionJvmArguments
import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.testFederation.InternalTestFederationApi
import org.jetbrains.kotlin.testFederation.TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY
import org.jetbrains.kotlin.testFederation.TEST_FEDERATION_ENABLED_ENV_KEY
import org.jetbrains.kotlin.testFederation.TEST_FEDERATION_MODE_ENV_KEY
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBatchesFunctionalTest {

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
        val allTestsResult = runner.runTests(Junit5).parseExecutedTests()
        val batch1Result = runner.runTests(Junit5, currentBatch = 1, totalBatches = 3).parseExecutedTests()
        val batch2Result = runner.runTests(Junit5, currentBatch = 2, totalBatches = 3).parseExecutedTests()
        val batch3Result = runner.runTests(Junit5, currentBatch = 3, totalBatches = 3).parseExecutedTests()
        checkBatchDistribution(allTestsResult, batch1Result, batch2Result, batch3Result)
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

        val allTests = runner.runTests(Junit5).parseExecutedTests()
        val bucket1 = runner.runTests(Junit5, currentBatch = 1, totalBatches = 3).parseExecutedTests()
        val bucket2 = runner.runTests(Junit5, currentBatch = 2, totalBatches = 3).parseExecutedTests()
        val bucket3 = runner.runTests(Junit5, currentBatch = 3, totalBatches = 3).parseExecutedTests()
        checkBatchDistribution(allTests, bucket1, bucket2, bucket3)
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
        val allTests = runner.runTests(Junit5).parseExecutedTests()
        val batch1 = runner.runTests(Junit5, currentBatch = 1, totalBatches = 3).parseExecutedTests()
        val batch2 = runner.runTests(Junit5, currentBatch = 2, totalBatches = 3).parseExecutedTests()
        val batch3 = runner.runTests(Junit5, currentBatch = 3, totalBatches = 3).parseExecutedTests()
        checkBatchDistribution(allTests, batch1, batch2, batch3)
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
        val allTests = runner.runTests(Junit5).parseExecutedTests()
        val batch1 = runner.runTests(Junit5, currentBatch = 1, totalBatches = 3).parseExecutedTests()
        val batch2 = runner.runTests(Junit5, currentBatch = 2, totalBatches = 3).parseExecutedTests()
        val batch3 = runner.runTests(Junit5, currentBatch = 3, totalBatches = 3).parseExecutedTests()
        checkBatchDistribution(allTests, batch1, batch2, batch3)
    }

    @Test
    fun `junit4 - plain old Tests`() {
        junit4SourcesDirectory.resolve("MyTests.kt").writeCode("""
            import kotlin.test.Test
            
            class MyTestA {
                @Test fun a() = Unit
                @Test fun b() = Unit
            }

            class MyTestB {
                @Test fun a() = Unit
                @Test fun b() = Unit
            }

            class MyTestC {
                @Test fun a() = Unit
                @Test fun b() = Unit
            }

            class MyTestD {
                @Test fun a() = Unit
                @Test fun b() = Unit
            }

            class MyTestE {
                @Test fun a() = Unit
                @Test fun b() = Unit
            }

            class MyTestF {
                @Test fun a() = Unit
                @Test fun b() = Unit
            }
        """.trimIndent()
        )

        val runner = createGradleRunner()
        val allTests = runner.runTests(JUnit4).parseExecutedTests()
        val batch1 = runner.runTests(JUnit4, currentBatch = 1, totalBatches = 3).parseExecutedTests()
        val batch2 = runner.runTests(JUnit4, currentBatch = 2, totalBatches = 3).parseExecutedTests()
        val batch3 = runner.runTests(JUnit4, currentBatch = 3, totalBatches = 3).parseExecutedTests()
        checkBatchDistribution(allTests, batch1, batch2, batch3)
    }


    /* Test Framework Code */

    private val targetProjectPath = ":repo:codebase-tests"
    private val junit5ResultsDirectory = Path("repo/codebase-tests/build/test-results/junit5Tests")
    private val junit4ResultsDirectory = Path("repo/codebase-tests/build/test-results/junit4Tests")
    private val junit5SourcesDirectory = Path("repo/codebase-tests/src/junit5Tests/kotlin")
    private val junit4SourcesDirectory = Path("repo/codebase-tests/src/junit4Tests/kotlin")

    @BeforeEach
    @AfterEach
    fun cleanup() {
        junit4ResultsDirectory.deleteRecursively()
        junit5ResultsDirectory.deleteRecursively()
        junit4SourcesDirectory.deleteRecursively()
        junit5SourcesDirectory.deleteRecursively()
    }

    data class TestResult(
        val containerName: String, val testName: String, val status: String,
    )

    enum class JunitTarget {
        JUnit4, Junit5
    }

    private fun GradleRunner.runTests(
        target: JunitTarget, currentBatch: Int? = null, totalBatches: Int? = null,
    ): BuildResult {
        val testTask = when (target) {
            JUnit4 -> "junit4Tests"
            Junit5 -> "junit5Tests"
        }

        return withArguments(
            *listOfNotNull(
                "$targetProjectPath:$testTask",
                if (currentBatch != null) "-Ptests.currentBatch=$currentBatch" else null,
                if (totalBatches != null) "-Ptests.totalBatches=$totalBatches" else null,

                /* Allow debugging of test Gradle process */
                "-Dorg.gradle.jvmargs=-Xmx1G -XX:+UseParallelGC " +
                        issueNewDebugSessionJvmArguments("Gradle Test Build").joinToString(" "),

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

    private fun checkBatchDistribution(all: List<TestResult>, vararg buckets: List<TestResult>) {
        /* Test is suspicious if a batch has no tests */
        buckets.forEachIndexed { index, batch ->
            if (batch.isEmpty()) fail("Batch ${index.plus(1)} does not contain any tests")
        }

        /* Test batches should not have overlapping tests */
        buckets.withIndex().zipWithNext { a, b ->
            val aTests = a.value
            val bTests = b.value

            val intersection = aTests.intersect(bTests.toSet())
            if (intersection.isNotEmpty()) {
                fail(buildString {
                    appendLine("Batch ${a.index.plus(1)} and ${b.index.plus(1)} have ${intersection.size} tests in common")
                    appendLine("    Common tests: ${intersection.joinToString(", ") { it.testName }}")
                })
            }
        }

        /* Test batches should execute all tests */
        assertEquals(
            all.toSet(), buckets.toList().flatten().toSet(),
            "Expected all tests to be distributed across batches without duplicates"
        )
    }

    private fun createGradleRunner(
        environment: Map<String, String> = defaultEnv(),
    ): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(Path("").toAbsolutePath().toFile())
            .withEnvironment(System.getenv() + environment)
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
