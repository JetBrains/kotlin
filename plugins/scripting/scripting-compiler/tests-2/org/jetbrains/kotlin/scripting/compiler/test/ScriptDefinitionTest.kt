/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import junit.framework.TestCase
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.junit.Assert
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.fileNamePattern
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptDefinitionTest : TestCase() {

    fun testGradleLikeFileNamePatternMatching() {

        fun makeDefinition(pattern: String) =
            ScriptDefinition.FromConfigurations(
                defaultJvmScriptingHostConfiguration,
                ScriptCompilationConfiguration {
                    @Suppress("DEPRECATION_ERROR")
                    fileNamePattern(pattern)
                },
                ScriptEvaluationConfiguration()
            )

        val initScriptDefinition = makeDefinition("(?:.+\\.)?init\\.gradle\\.kts")
        val settingsScriptDefinition = makeDefinition("(?:.+\\.)?settings\\.gradle\\.kts")
        val buildScriptDefinition = makeDefinition(".+(?<!(^|\\.)(init|settings))\\.gradle\\.kts")

        fun ScriptDefinition.FromConfigurations.assertMatches(path: String) {
            Assert.assertTrue(
                "Script definition with filePathPattern '${filePathPattern}' should match '$path'",
                isScript(FileScriptSource(File(path)))
            )
        }

        fun ScriptDefinition.FromConfigurations.assertNotMatches(path: String) {
            Assert.assertFalse(
                "Script definition with filePathPattern '${filePathPattern}' should NOT match '$path'",
                isScript(FileScriptSource(File(path)))
            )
        }

        with (initScriptDefinition) {
            assertMatches("some/path/init.gradle.kts")
            assertMatches("init.gradle.kts")
            assertMatches("some/path/some.init.gradle.kts")
            assertMatches("some.init.gradle.kts")
            assertNotMatches("some/path/settings.gradle.kts")
            assertNotMatches("settings.gradle.kts")
            assertNotMatches("some/path/something.gradle.kts")
            assertNotMatches("something.gradle.kts")
        }

        with (settingsScriptDefinition) {
            assertMatches("some/path/settings.gradle.kts")
            assertMatches("settings.gradle.kts")
            assertMatches("some/path/some.settings.gradle.kts")
            assertMatches("some.settings.gradle.kts")
            assertNotMatches("some/path/init.gradle.kts")
            assertNotMatches("init.gradle.kts")
            assertNotMatches("some/path/something.gradle.kts")
            assertNotMatches("something.gradle.kts")
        }

        with (buildScriptDefinition) {
            assertNotMatches("some/path/settings.gradle.kts")
            assertNotMatches("settings.gradle.kts")
            assertNotMatches("some/path/init.gradle.kts")
            assertNotMatches("init.gradle.kts")
            assertMatches("some/path/build.gradle.kts")
            assertMatches("build.gradle.kts")
            assertMatches("some/path/some.build.gradle.kts")
            assertMatches("some.build.gradle.kts")
            assertMatches("some/path/something.gradle.kts")
            assertMatches("something.gradle.kts")
        }
    }
}