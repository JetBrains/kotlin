/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.computeEnvironment
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.processLaunchOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ProcessLaunchOptionsTest {

    @Test
    fun `verify includeSystemEnvironment defaults to false`() {
        val opts = createProcessLaunchOptions()

        assertNotNull(
            opts.includeSystemEnvironment.orNull,
            "Expected includeSystemEnvironment is not null"
        )
        assertFalse(
            opts.includeSystemEnvironment.get(),
            "Expected includeSystemEnvironment default is false"
        )
    }


    @Test
    fun `when includeSystemEnvironment is true - expect computed environment contains system environment`() {
        val opts = createProcessLaunchOptions {
            includeSystemEnvironment.set(true)
        }

        val expectedEnv = System.getenv()

        assertEquals(
            expected = expectedEnv.keys,
            actual = opts.computeEnvironment().keys,
            message = "Expected environment contains system environment",
        )
    }

    @Test
    fun `when includeSystemEnvironment is default (false) - expect computed environment does not contain system environment`() {
        val opts = createProcessLaunchOptions()

        assertEquals(
            expected = emptySet(),
            actual = opts.computeEnvironment().keys,
            message = "Expected computed environment is empty",
        )
    }

    @Test
    fun `when customEnvironment has values - and includeSystemEnvironment is enabled - expect computed environment is overridden`() {
        val systemPath = System.getenv("PATH")
        val pathOverride = "foo"

        require(systemPath != pathOverride) { "Cannot run test if PATH is already set to pathOverride:$pathOverride" }

        val opts = createProcessLaunchOptions {
            includeSystemEnvironment.set(true)
            customEnvironment.put("PATH", pathOverride)
        }

        val actualPath = opts.computeEnvironment().getValue("PATH")

        assertEquals(
            expected = pathOverride,
            actual = actualPath,
            message = "Expected system env PATH is overridden with custom PATH",
        )
    }

    companion object {
        private fun createProcessLaunchOptions(
            configure: ProcessLaunchOptions.() -> Unit = {},
        ): ProcessLaunchOptions {
            return ProjectBuilder.builder()
                .build()
                .objects
                .processLaunchOptions(configure)
        }
    }
}
