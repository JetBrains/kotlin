/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("JVM classpath metadata incremental compilation")
class JvmClasspathMetadataIncrementalIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            logLevel = LogLevel.DEBUG,
            languageVersion = "2.0",
            enableUnsafeIncrementalCompilationForMultiplatform = true,
        )

    @GradleTest
    @DisplayName("Verify that incremental compilation without JVM classpath metadata leads to incorrect resolution")
    fun testWithJvmClasspathMetadataDisabled(gradleVersion: GradleVersion) {
        project(
            projectName = "jvm-classpath-metadata-incremental",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(jvmClasspathMetadata = false)
        ) {
            build("jvmRun", "-DmainClass=BarKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertOutputContains("KMP output: Any")
            }

            projectPath.resolve("src/commonMain/kotlin/foo.kt").modify { content ->
                content.replace("fun foo() = bar(42)", "fun foo() = bar(41)")
            }

            /**
             * Without kotlin.internal.kmp.jvmClasspathMetadata, the compiler produces incorrect output (Int instead of the expected Any).
             * During incremental builds, commonMain incorrectly gains access to declarations from jvmMain through artifacts left in the build/classes/ directory.
             */
            build("jvmRun", "-DmainClass=BarKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertOutputContains("KMP output: Int") // wrong result
            }
        }
    }
}
