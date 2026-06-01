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
            ["-version", "-P", "plugin:<pluginId>:<optionName>=<value>"] to [
                "-version",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>"
            ],

            [
                "-version",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-no-stdlib",
                "-P",
                "plugin:<pluginId>:<optionName>=<value>",
                "-no-sdk"
            ] to ["-version", "-P", "plugin:<pluginId>:<optionName>=<value>", "-no-stdlib", "-no-sdk"],

            [
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
            ] to ["-version", "-P", "plugin:<pluginId>:<optionName>=<value>", "-no-stdlib", "-no-sdk"],
        )
        JpsKotlinCompilerRunner().apply {
            actualToExpectedArgsMap.forEach { [input, expected] ->
                assertEquals(expected, input.filterDuplicatedCompilerPluginOptionsForTest())
            }
        }
    }

    @Test
    fun filterDuplicatedWarningLevelTest() {
        val actualToExpectedArgsMap = mapOf(
            ["-version", "-language-version", "2.1"] to
                    ["-version", "-language-version", "2.1"],

            ["-Xwarning-level=INVISIBLE_REFERENCE:error"] to
                    ["-Xwarning-level=INVISIBLE_REFERENCE:error"],

            [
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-language-version", "2.1",
                "-Xwarning-level=INVISIBLE_REFERENCE:error"
            ] to ["-Xwarning-level=INVISIBLE_REFERENCE:error", "-language-version", "2.1"],

            [
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-Xwarning-level=NOTHING_TO_INLINE:warning"
            ] to ["-Xwarning-level=INVISIBLE_REFERENCE:error", "-Xwarning-level=NOTHING_TO_INLINE:warning"],

            [
                "-version",
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-no-stdlib",
                "-Xwarning-level=NOTHING_TO_INLINE:warning",
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-Xwarning-level=NOTHING_TO_INLINE:warning",
                "-no-sdk"
            ] to [
                "-version",
                "-Xwarning-level=INVISIBLE_REFERENCE:error",
                "-no-stdlib",
                "-Xwarning-level=NOTHING_TO_INLINE:warning",
                "-no-sdk"
            ],
        )
        JpsKotlinCompilerRunner().apply {
            actualToExpectedArgsMap.forEach { [input, expected] ->
                assertEquals(expected, input.filterDuplicatedWarningLevelForTest())
            }
        }
    }
}
