/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import TestLifecycleTask.QualityGate
import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.test.isTeamCityBuild
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

    companion object {

        internal lateinit var dump: TestLifecycleTasksDump

        @JvmStatic
        @BeforeAll
        fun resolveDump() {
            dump = generateTestLifecycleTasksDump(projectDir)
        }

        val expectFile: Path = projectDir.resolve("repo/testLifecycleTask.dump.txt")
    }
}
