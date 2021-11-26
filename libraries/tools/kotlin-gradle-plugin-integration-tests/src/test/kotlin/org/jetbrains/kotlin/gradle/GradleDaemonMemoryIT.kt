/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.tooling.internal.consumer.ConnectorServices
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertTrue

@DisplayName("Gradle daemon memory leak")
class GradleDaemonMemoryIT : KGPDaemonsBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)

    // For corresponding documentation, see https://docs.gradle.org/current/userguide/gradle_daemon.html
    // Setting user.variant to different value implies a new daemon process will be created.
    // In order to stop daemon process, special exit task is used ( System.exit(0) ).
    @DisplayName("No small memory leak in plugin")
    @GradleTest
    fun testGradleDaemonMemory(gradleVersion: GradleVersion) {
        project("gradleDaemonMemory", gradleVersion) {
            val userVariantArg = "-Duser.variant=ForTest"
            val memoryMaxGrowthLimitKB = 5000
            val buildCount = 10
            val reportMemoryUsage = "-Dkotlin.gradle.test.report.memory.usage=true"
            val reportRegex = "\\[KOTLIN]\\[PERF] Used memory after build: (\\d+) kb \\(difference since build start: ([+-]?\\d+) kb\\)"
                .toRegex()

            val usedMemory: List<Int> = (1..buildCount).map {
                var reportedMemory = 0
                build(userVariantArg, reportMemoryUsage, "clean", "assemble") {
                    val matches = output
                        .lineSequence()
                        .filter { it.contains("[KOTLIN][PERF]") }
                        .joinToString(separator = "\n")
                        .run { reportRegex.find(this) }
                    assertTasksExecuted(":compileKotlin")
                    assert(matches != null && matches.groups.size == 3) {
                        printBuildOutput()
                        "Used memory after build is not reported by plugin"
                    }
                    reportedMemory = matches!!.groupValues[1].toInt()
                }
                reportedMemory
            }

            // ensure that the maximum of the used memory established after several first builds doesn't raise significantly in the subsequent builds
            val establishedMaximum = usedMemory.take(buildCount / 2).maxOrNull()!!
            val totalMaximum = usedMemory.maxOrNull()!!

            val maxGrowth = totalMaximum - establishedMaximum
            assertTrue(
                maxGrowth <= memoryMaxGrowthLimitKB,
                "Maximum used memory over series of builds growth $maxGrowth " +
                        "(from $establishedMaximum to $totalMaximum) kb > $memoryMaxGrowthLimitKB kb\n" +
                        "Usages: $usedMemory"
            )

            // testing that nothing remains locked by daemon, see KT-9440
            build(userVariantArg, "clean")
        }
    }
}