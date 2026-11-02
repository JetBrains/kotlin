/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.logging.Logging
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.internal.logging.events.LogEvent
import org.gradle.internal.logging.events.OutputEvent
import org.gradle.internal.operations.logging.LogEventLevel
import org.gradle.process.ExecOperations
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.jetbrains.kotlin.gradle.util.withGradleLogCapture
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

/**
 * Checks how [KotlinWebpackRunner] logs the output of the webpack process.
 *
 * Instead of webpack, the runner launches [KotlinWebpackRunnerTest.Companion.main] in a new JVM.
 */
class KotlinWebpackRunnerTest {

    @TempDir
    lateinit var tempDir: Path

    @TestFactory
    fun `expect stdout is logged at info level and stderr at error level`(): List<DynamicTest> {
        return listOf(
            "execute()" to { KotlinWebpackRunner().execute() },
            "start().waitForResult()" to { KotlinWebpackRunner().start().waitForResult() },
        ).map { (desc, exec) ->
            dynamicTest(desc) {
                val logs = withGradleLogCapture {
                    exec()
                }

                assertHasLogLines(
                    logs,
                    STDOUT_LINE to LogEventLevel.INFO,
                    STDERR_LINE to LogEventLevel.ERROR,
                )
            }
        }
    }

    private fun KotlinWebpackRunner(): KotlinWebpackRunner {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.resolve("project").toFile()).build()

        val webpackToolLocation = "webpack/bin/webpack.js"

        val entry = tempDir.resolve("entry.js").apply { writeText("") }
        val npmToolingEnvDir = tempDir.resolve("tooling")
        npmToolingEnvDir.resolve("node_modules/$webpackToolLocation").createParentDirectories().writeText("")

        // Write Java args to a file, otherwise the length can exceed the Windows command line limit.
        // Replace backslashes with forward slashes, because backslashes are escape characters in quoted argument file values.
        val javaArgsFile = tempDir.resolve("java-args.txt").apply {
            writeText(
                """
                -cp "${currentClasspath.replace('\\', '/')}"
                $APP_FQN
                """.trimIndent()
            )
        }

        return KotlinWebpackRunner(
            name = "test",
            npmProjectDir = tempDir.toFile(),
            nodeExecutable = currentJavaExecutable,
            logger = Logging.getLogger(KotlinWebpackRunnerTest::class.java),
            configFile = tempDir.resolve("webpack.config.js").toFile(),
            tool = webpackToolLocation,
            args = emptyList(),
            nodeArgs = listOf("@$javaArgsFile"),
            config = KotlinWebpackConfig(
                npmProjectDir = project.provider { tempDir.toFile() },
                entry = entry.toFile(),
                rules = project.objects.webpackRulesContainer(),
                defineNonBrowserEnvironmentProperties = project.objects.property(Boolean::class.java).value(false),
            ),
            objects = project.objects,
            execOps = project.serviceOf<ExecOperations>(),
            npmToolingEnvDir = npmToolingEnvDir.toFile(),
            resolveModulesFromKotlinToolingDir = false,
        )
    }

    companion object {
        private const val STDOUT_LINE = "KotlinWebpackRunnerTest: good ol' trusty reliable stdout"
        private const val STDERR_LINE = "KotlinWebpackRunnerTest: whoop whoop! stderr time!"

        /** The current FQN of the test class. Used to launch [main] as a Java application. */
        private val APP_FQN: String = KotlinWebpackRunnerTest::class.qualifiedName!!

        @JvmStatic
        fun main(@Suppress("unused") args: Array<String>) {
            println(STDOUT_LINE)
            System.err.println(STDERR_LINE)
        }

        private val currentJavaExecutable: String by lazy {
            ProcessHandle.current().info().command().orElseThrow()
        }

        private val currentClasspath: String by lazy {
            listOfNotNull(
                System.getenv("CLASSPATH"),
                System.getProperty("java.class.path"),
            ).joinToString(File.pathSeparator)
        }

        private fun assertHasLogLines(output: List<OutputEvent>, vararg expectedLogs: Pair<String, LogEventLevel>) {
            val expectedLogs = expectedLogs.toMap()
            val actualMessages = output.filterIsInstance<LogEvent>()
                .filter { it.message in expectedLogs.keys }
                .associate { it.message to it.level }
            assertEquals(expectedLogs, actualMessages)
        }
    }
}
