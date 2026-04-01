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
            actualToExpectedArgsMap.forEach { (input, expected) ->
                assertEquals(expected, input.filterDuplicatedCompilerPluginOptionsForTest())
            }
        }
    }

    @Test
    fun filterDuplicatedWarningLevelTest() {
        val actualToExpectedArgsMap = mapOf(
            listOf("-version", "-language-version", "2.1") to
                    listOf("-version", "-language-version", "2.1"),

            listOf("-Xwarning-level=INVISIBLE_REFERENCE:error") to
                    listOf("-Xwarning-level=INVISIBLE_REFERENCE:error"),

            listOf(
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-language-version", "2.1",
                "-Xwarning-level=INVISIBLE_REFERENCE:error"
            ) to listOf("-Xwarning-level=INVISIBLE_REFERENCE:error", "-language-version", "2.1"),

            listOf(
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-Xwarning-level=NOTHING_TO_INLINE:warning"
            ) to listOf("-Xwarning-level=INVISIBLE_REFERENCE:error", "-Xwarning-level=NOTHING_TO_INLINE:warning"),

            listOf(
                "-version",
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-no-stdlib",
                "-Xwarning-level=NOTHING_TO_INLINE:warning",
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-Xwarning-level=NOTHING_TO_INLINE:warning",
                "-no-sdk"
            ) to listOf(
                "-version",
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-no-stdlib",
                "-Xwarning-level=NOTHING_TO_INLINE:warning",
                "-no-sdk"
            ),
        )
        JpsKotlinCompilerRunner().apply {
            actualToExpectedArgsMap.forEach { (input, expected) ->
                assertEquals(expected, input.filterDuplicatedWarningLevelForTest())
            }
        }
    }
}