/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.junit.Test
import kotlin.test.assertTrue

class GradleDaemonMemoryIT : BaseGradleIT() {
    // For corresponding documentation, see https://docs.gradle.org/current/userguide/gradle_daemon.html
    // Setting user.variant to different value implies a new daemon process will be created.
    // In order to stop daemon process, special exit task is used ( System.exit(0) ).
    @Test
    fun testGradleDaemonMemory() {
        val project = Project("kotlinProject")
        val VARIANT_CONSTANT = "ForTest"
        val userVariantArg = "-Duser.variant=$VARIANT_CONSTANT"
        val MEMORY_MAX_GROWTH_LIMIT_KB = 500
        val BUILD_COUNT = 15
        val reportMemoryUsage = "-Dkotlin.gradle.test.report.memory.usage=true"
        val options = BaseGradleIT.BuildOptions(withDaemon = true)

        fun exitTestDaemon() {
            project.build(userVariantArg, reportMemoryUsage, "exit", options = options) {
                assertFailed()
                assertContains("The daemon has exited normally or was terminated in response to a user interrupt.")
            }
        }

        fun buildAndGetMemoryAfterBuild(): Int {
            var reportedMemory: Int? = null

            project.build(userVariantArg, reportMemoryUsage, "clean", "build", options = options) {
                assertSuccessful()
                val matches = "\\[KOTLIN\\]\\[PERF\\] Used memory after build: (\\d+) kb \\(difference since build start: ([+-]?\\d+) kb\\)"
                    .toRegex().find(output)
                assert(matches != null && matches.groups.size == 3) { "Used memory after build is not reported by plugin" }
                reportedMemory = matches!!.groupValues[1].toInt()
            }

            return reportedMemory!!
        }

        exitTestDaemon()

        try {
            val usedMemory = (1..BUILD_COUNT).map { buildAndGetMemoryAfterBuild() }

            // ensure that the maximum of the used memory established after several first builds doesn't raise significantly in the subsequent builds
            val establishedMaximum = usedMemory.take(5).max()!!
            val totalMaximum = usedMemory.max()!!

            val maxGrowth = totalMaximum - establishedMaximum
            assertTrue(
                maxGrowth <= MEMORY_MAX_GROWTH_LIMIT_KB,
                "Maximum used memory over series of builds growth $maxGrowth (from $establishedMaximum to $totalMaximum) kb > $MEMORY_MAX_GROWTH_LIMIT_KB kb"
            )

            // testing that nothing remains locked by daemon, see KT-9440
            project.build(userVariantArg, "clean", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertSuccessful()
            }
        } finally {
            exitTestDaemon()
        }
    }
}