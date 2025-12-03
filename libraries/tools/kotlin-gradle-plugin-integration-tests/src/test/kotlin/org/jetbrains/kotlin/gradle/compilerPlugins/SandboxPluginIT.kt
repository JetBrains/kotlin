/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.compilerPlugins

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.test.Ignore

@DisplayName("Sandbox plugin tests")
@OtherGradlePluginTests
class SandboxPluginIT : KGPBaseTest() {
    private val pluginClasspath: String get() = System.getProperty("sandboxPluginClasspath")
    private val pluginArgument: String get() = "-Xplugin=$pluginClasspath"

    @DisplayName("Plugin generates top-level function")
    @GradleTest
    @Ignore
    fun testTopLevelFunctionGeneration(gradleVersion: GradleVersion) {
        project("sandboxPluginGeneratingTopLevelFunction", gradleVersion) {
            buildGradleKts.appendText(
                """
                tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
                    compilerOptions {
                        freeCompilerArgs.add("$pluginArgument")
                    }
                }
                """.trimIndent()
            )
            build("check")
        }
    }
}
