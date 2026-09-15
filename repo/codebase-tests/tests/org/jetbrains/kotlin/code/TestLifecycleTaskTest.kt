/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import TestLifecycleTasksModel
import TestLifecycleTasksModel.TestLifecycleTask
import TestLifecycleTask.QualityGate
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.gradle.GradleBuild
import org.jetbrains.kotlin.code.TestLifecycleTaskTest.Companion.sanitize
import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.test.isTeamCityBuild
import org.jetbrains.kotlin.testFederation.Domain
import org.jetbrains.kotlin.testFederation.fromArgumentStringOrThrow
import org.jetbrains.kotlin.testFederation.toArgumentString
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.Test

/**
 * Verifies the dump of test lifecycle tasks, their test task dependencies, and domains.
 * Update it from IntelliJ's `Tools` run configurations with `Update testLifecycleTask.dump.txt`,
 * or use `Update all project dumps` to refresh every dump.
 */
@GradleLock
class TestLifecycleTaskTest {

    private val qualityGateMasterTasks: Set<String>
        get() = (System.getProperty("quality.gate.master.tasks") ?: error("Missing 'quality.gate.master.tasks' property"))
            .split(";").toSet()

    private val qualityGateNightlyTasks: Set<String>
        get() = (System.getProperty("quality.gate.nightly.tasks") ?: error("Missing 'quality.gate.nightly.tasks' property"))
            .split(";").toSet()

    @Test
    fun `testLifecycleTask dump`() {
        val actualText = dump.renderedText

        if (!expectFile.isRegularFile()) expectFile.createFile()
        val expectText = expectFile.readText().sanitize()
        if (expectText != actualText) {
            if (isTeamCityBuild) {
                println(buildString {
                    appendLine("==== Actual ${expectFile.name} ====")
                    appendLine(actualText)
                })

                expectFile.resolveSibling(expectFile.nameWithoutExtension + "-actual.txt").writeText(actualText)
            }

            throw AssertionFailedError(
                "${expectFile.name} is out-of-date",
                FileInfo(expectFile.absolutePathString(), expectText.encodeToByteArray()),
                actualText,
            )
        }
    }

    /**
     * Identifies if the test is currently running on TeamCity.
     * Used, as this test will also verify if the TeamCity config (passed in as [qualityGateMasterTasks] and [qualityGateNightlyTasks])
     * is in sync with the definition within kotlin.git (provided by the dump)
     */
    @EnabledIfSystemProperty(named = "teamcity", matches = "true")
    @EnabledIfSystemProperty(named = "quality.gate.master.tasks", matches = ".+") // Remove after a 'phase-in' period of this test
    @Test
    fun `quality gates are in sync with TeamCity`() {
        /*
        Test if defined quality gates contain all expected tasks. kotlin.git is the source of truth:
        expected: tasks defined in kotlin.git
        actual: tasks executed by our TeamCity builds
         */
        val allTestLifecycleTasks = dump.buildModel.flatMap { [_, value] ->
            value?.testLifecycleTasks.orEmpty()
        }.toSet()

        val testLifecycleTasksIndex = allTestLifecycleTasks.associateBy { it.path }

        val expandedQualityGateMasterTasks = qualityGateMasterTasks.flatMap { task ->
            buildList {
                add(task)
                addAll(testLifecycleTasksIndex[task]?.allDependencies.orEmpty())
            }
        }.toSet()

        val expandedQualityGateNightlyTasks = qualityGateNightlyTasks.flatMap { task ->
            buildList {
                add(task)
                addAll(testLifecycleTasksIndex[task]?.allDependencies.orEmpty())
            }
        }.toSet()


        val issues = mutableListOf<String>()
        fun reportIssue(builder: StringBuilder.() -> Unit) {
            issues.add(buildString(builder))
        }

        allTestLifecycleTasks.forEach { lifecycleTask ->
            val expectedQualityGate = QualityGate.valueOf(lifecycleTask.qualityGate)

            val actualQualityGate = when (lifecycleTask.path) {
                in expandedQualityGateMasterTasks -> QualityGate.Master
                in expandedQualityGateNightlyTasks -> QualityGate.Nightly
                else -> run {
                    // If all direct dependencies are covered by the master quality gate,
                    // then it is considered to be covered as well by 'master'
                    if (lifecycleTask.dependencies.all { dependency ->
                            dependency in expandedQualityGateMasterTasks
                        }) return@run QualityGate.Master

                    // If all direct dependencies are covered by the nightly quality gate,
                    // then it is considered to be covered as well by 'nightly'
                    if (lifecycleTask.dependencies.all { dependency ->
                            dependency in expandedQualityGateNightlyTasks
                        }) return@run QualityGate.Nightly

                    QualityGate.None
                }
            }

            if (expectedQualityGate != actualQualityGate && expectedQualityGate != QualityGate.Undefined) {
                reportIssue {
                    appendLine("'${lifecycleTask.path}': Quality Gate is not in-sync with TeamCity")
                    appendLine("  - expected (kotlin.git): '$expectedQualityGate', actual (TeamCity): '$actualQualityGate'")
                    appendLine(
                        """
                        Please adjust the TeamCity configuration to reflect the expected Quality Gates.
                        If the expected Quality Gate (defined in kotlin.git) is wrong, adjust the quality gate

                        `build.gradle.kts`
                        ```
                          testLifecycleTask("${lifecycleTask.path.removePrefix(":")}", QualityGate.$actualQualityGate) {
                          }
                        ```
                    """.trimIndent().prependIndent("    ")
                    )
                }
            }
        }

        if (issues.isNotEmpty()) {
            fail(buildString {
                appendLine("kotlin.git and the current TeamCity configuration are not in sync. ${issues.size} issues found:")
                issues.sorted().forEach { issue ->
                    appendLine(issue.prependIndent("  "))
                }
            })
        }
    }

    @EnabledIfSystemProperty(named = "teamcity", matches = "true")
    @EnabledIfSystemProperty(named = "quality.gate.master.tasks", matches = ".+")
    @Test
    fun `TeamCity configurations invoke test lifecycle tasks`() {
        val testLifecycleTaskPaths = dump.buildModel.flatMap { [_, model] ->
            model?.testLifecycleTasks.orEmpty()
        }.map { it.path }.toSet()

        val invokedTasks = (qualityGateMasterTasks + qualityGateNightlyTasks).filter { it.isNotBlank() }

        val issues = invokedTasks
            .filterNot { task -> task in testLifecycleTaskPaths }
            .filterNot { task -> task in nonTestEntryPointTasks }
            .filterNot { task -> nonTestEntryPointTaskPrefixes.any { prefix -> task.startsWith(prefix) } }
            .distinct()

        if (issues.isNotEmpty()) {
            fail(buildString {
                appendLine(
                    "TeamCity invokes ${issues.size} Gradle task(s) that are neither a 'testLifecycleTask' " +
                            "nor an allowlisted exception:"
                )
                issues.sorted().forEach { task ->
                    appendLine("  - '$task'")
                }
                appendLine()
                appendLine(
                    """
                    Either register it as a 'testLifecycleTask' in kotlin.git and point the TeamCity build at it:

                    `build.gradle.kts`
                    ```
                      testLifecycleTask("someTest", QualityGate.Master) {
                          dependsOn(":some:module:someTest")
                      }
                    ```

                    ...or, if it is not a test entry point at all, add it to 'nonTestEntryPointTasks'
                    in this test with a short justification.
                    """.trimIndent().prependIndent("  ")
                )
            })
        }
    }

    object Update {
        @JvmStatic
        fun main(args: Array<String>) {
            val actualText = generateTestLifecycleTasksDump(projectDir).renderedText
            if (expectFile.readText().sanitize() != actualText) {
                expectFile.writeText(actualText + "\n")
                println("Updated: ${expectFile.toUri().toURL()}")
            } else {
                println("Up to date: ${expectFile.toUri().toURL()}")
            }
        }
    }

    companion object {

        lateinit var dump: TestLifecycleTasksDump

        @JvmStatic
        @BeforeAll
        fun resolveDump() {
            dump = generateTestLifecycleTasksDump(projectDir)
        }

        val expectFile: Path = projectDir.resolve("repo/testLifecycleTask.dump.txt")

        fun String.sanitize() = replace(Regex("""\R"""), "\n").trim()

        private val nonTestEntryPointTasks = setOf(
            "build",
            "check",
            "clean",
            "assemble",
            "install",
            "publish",
            "publishToMavenLocal",
            "publishPlugins",
            "help",
            "compileAll",
            "dist",
            "dexMethodCount",
            "checkBuild",
            "resolveDependencies",
            ":kotlin-native:publish",
            "buildTypes:publish",
            "generateConfigurationKeys",
            "generateTests",
            ":buildSamplesWithPlatformLibs",
            ":kotlin-native:copySamples",
            ":kotlin-compiler-native-image:kotlincNativeImageArtifacts",
            ":compiler:test-coverage:firCoverageReport",
            ":buildAnalyzer",
            ":konanRun",
            ":gradle-build-conventions:test-federation-convention:test",
            ":native:swift:swift-export-embeddable:validateSwiftExportEmbeddable",
        )

        private val nonTestEntryPointTaskPrefixes = setOf(
            ":kotlinx-coroutines-core:",
        )
    }
}

private fun generateTestLifecycleTasksDump(projectDir: Path): TestLifecycleTasksDump {
    val connector = GradleConnector.newConnector()
        .forProjectDirectory(projectDir.toFile())
    val models = try {
        connector.connect().use { connection ->
            connection.action(FetchTestLifecycleTaskModelBuildAction())
                .setStandardError(System.err)
                .setStandardOutput(System.out)
                .setJvmArguments(*defaultGradleJvmArguments().toTypedArray(), *issueNewDebugSessionJvmArguments("Build Action"))
                .withArguments(
                    "-Pteamcity=true",
                    "-Pkotlin.native.enabled=true",
                    *defaultGradleArguments().toTypedArray()
                ).run()
        }
    } finally {
        connector.disconnect()
    }

    val missingProjects = mutableListOf<String>()
    val testLifecycleTasks = mutableListOf<TestLifecycleTask>()
    val missingTestTasks = mutableSetOf<String>()
    val testTaskIndex = mutableMapOf<String, TestLifecycleTasksModel.TestTask>()
    val testLifecycleTaskIndex = mutableMapOf<String, TestLifecycleTask>()

    models.forEach { [projectPath, model] ->
        if (model == null) {
            missingProjects.add(projectPath)
            return@forEach
        }

        testLifecycleTasks += model.testLifecycleTasks
        missingTestTasks += model.testTasks.map { it.path }

        model.testLifecycleTasks.forEach { testLifecycleTask ->
            testLifecycleTaskIndex[testLifecycleTask.path] = testLifecycleTask
        }

        model.testTasks.forEach { testTask ->
            testTaskIndex[testTask.path] = testTask
        }
    }

    /* Find missing test tasks */
    testLifecycleTasks.forEach { testLifecycleTask ->
        missingTestTasks -= testLifecycleTask.allDependencies
    }

    fun TestLifecycleTask.allDomains(): Set<Domain> {
        return allDependencies.flatMap { dependency ->
            Domain.fromArgumentStringOrThrow(testTaskIndex[dependency]?.domains ?: return@flatMap emptyList())
        }.toSet()
    }

    val renderedText = buildString {
        appendLine("####################################################")
        appendLine("This dump is generated by ${TestLifecycleTaskTest::class.java.simpleName}.kt")
        appendLine("####################################################")
        appendLine()

        testLifecycleTasks.sortedBy { it.path }.forEach { lifecycleTask ->
            val allDomains = lifecycleTask.allDomains()

            appendLine()
            appendLine("${lifecycleTask.path}: [${allDomains.toArgumentString()}]")
            appendLine("  qualityGate: ${lifecycleTask.qualityGate}")

            appendLine("  allDependencies:")
            lifecycleTask.allDependencies.sorted().forEach { dependency ->
                val testTask = testTaskIndex[dependency]
                val testLifecycleTask = testLifecycleTaskIndex[dependency]
                val domains = testTask?.domains ?: testLifecycleTask?.allDomains()?.toArgumentString()
                appendLine("    - $dependency [${domains}]")
            }
        }

        appendLine()
        appendLine()
        appendLine("Not connected to any 'lifecycleTestTask':")
        missingTestTasks.sorted().forEach {
            val testTask = testTaskIndex[it]
            val testLifecycleTask = testLifecycleTaskIndex[it]
            val domains = testTask?.domains ?: testLifecycleTask?.allDomains()?.toArgumentString()
            appendLine("  - $it [${domains}]")
        }
    }.sanitize()

    return TestLifecycleTasksDump(
        buildModel = models,
        renderedText = renderedText
    )
}

data class TestLifecycleTasksDump(
    val buildModel: Map<String, TestLifecycleTasksModel?>,
    val renderedText: String,
)

class FetchTestLifecycleTaskModelBuildAction : BuildAction<Map<String, TestLifecycleTasksModel?>> {
    override fun execute(controller: BuildController): Map<String, TestLifecycleTasksModel?> {
        val build = controller.getModel(GradleBuild::class.java)
        return buildMap {
            build.projects.forEach { project ->
                val model = controller.findModel(project, TestLifecycleTasksModel::class.java)
                put(project.buildTreePath, model)
            }
        }
    }
}
