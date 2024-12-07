/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.junit.Test
import kotlin.test.assertEquals

class JpsKotlinCompilerRunnerTest {
    @Test
    fun filterDuplicatedCompilerPluginOptionsTest() {
        val actualToExpectedArgsMap = mapOf(
            listOf("-version", "-P", "plugin:<pluginId>:<optionName>=<value>") to listOf(
                "-version",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>"
            ),

            listOf(
                "-version",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-no-stdlib",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-no-sdk"
            ) to listOf("-version", "-P", "plugin:<pluginId>:<optionName>=<value>", "-no-stdlib", "-no-sdk"),

            listOf(
                "-version",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-no-stdlib",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-no-sdk"
            ) to listOf("-version", "-P", "plugin:<pluginId>:<optionName>=<value>", "-no-stdlib", "-no-sdk"),
        )
        JpsKotlinCompilerRunner().apply {
            actualToExpectedArgsMap.forEach {
                assertEquals(filterDuplicatedCompilerPluginOptionsForTest(it.key), it.value)
            }
        }
    }
}