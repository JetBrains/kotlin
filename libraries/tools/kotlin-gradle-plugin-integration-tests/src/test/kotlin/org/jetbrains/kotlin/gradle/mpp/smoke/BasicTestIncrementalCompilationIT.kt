/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.mpp.KmpIncrementalITBase
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Basic incremental scenarios with tests in KMP - K2")
@MppGradlePluginTests
open class BasicTestIncrementalCompilationIT : KmpIncrementalITBase() {
    override val mainCompileTasks: Set<String>
        get() = setOf(
            ":app:compileTestKotlinJvm",
            ":lib:compileTestKotlinJvm",

            ":app:compileTestKotlinNative",
            ":lib:compileTestKotlinNative",

            ":app:compileTestKotlinJs",
            ":lib:compileTestKotlinJs",

            ":app:jsTest",
            ":lib:jsTest",

            ":app:jvmTest",
            ":lib:jvmTest",

            ":app:nativeTest",
            ":lib:nativeTest",
        )
    override val gradleTask: String
        get() = "build"

    @DisplayName("KMP tests are rebuilt when affected")
    @GradleTest
    fun testAffectingTestDependencies(gradleVersion: GradleVersion): Unit = withProject(gradleVersion) {
        build("build")

        /**
         * Step 1 - touch lib/common, affect all tests in app and lib
         */

        testCase(
            incrementalPath = touchAndGet("lib", "commonMain", "CommonUtil.kt"),
            executedTasks = mainCompileTasks,
        )

        /**
         * Step 2 - touch app/common, affect all tests in app
         */

        testCase(
            incrementalPath = touchAndGet("app", "commonMain", "PlainPublicClass.kt"),
            executedTasks = setOf(
                ":app:compileTestKotlinJvm",
                ":app:compileTestKotlinNative",
                ":app:compileTestKotlinJs",
                ":app:jsTest",
                ":app:jvmTest",
                ":app:nativeTest",
            ),
        )

        /**
         * Step 3 - touch app/jvm, affect jvm tests in app
         */

        val touchedAppJvm = touchAndGet("app", "jvmMain", "PlainPublicClassJvm.kt")
        testCase(
            incrementalPath = null,
            executedTasks = setOf(
                ":app:compileTestKotlinJvm",
                ":app:jvmTest",
            ),
        ) {
            assertCompiledKotlinSources(listOf(touchedAppJvm).relativizeTo(projectPath), output) //KT-63476
        }

        /**
         * Step 4 - touch app/js, affect js tests in app
         */

        testCase(
            incrementalPath = touchAndGet("app", "jsMain", "PlainPublicClassJs.kt"),
            executedTasks = setOf(
                ":app:compileTestKotlinJs",
                ":app:jsTest",
            ),
        )

        /**
         * Step 5 - touch app/native, affect native tests in app
         */

        touchAndGet("app", "nativeMain", "PlainPublicClassNative.kt")
        testCase(
            incrementalPath = null,
            executedTasks = setOf(
                ":app:compileTestKotlinNative",
                ":app:nativeTest",
            ),
        )
    }
}

@DisplayName("Basic incremental scenarios with tests in KMP - K1")
class BasicTestIncrementalCompilationK1IT : BasicTestIncrementalCompilationIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}