/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.mpp.KmpIncrementalITBase
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Basic incremental scenarios with KMP - K2")
@MppGradlePluginTests
open class BasicIncrementalCompilationIT : KmpIncrementalITBase() {

    @DisplayName("Base test case - local change, local recompilation")
    @GradleTest
    fun testStrictlyLocalChange(gradleVersion: GradleVersion): Unit = withProject(gradleVersion) {
        build("assemble")

        /**
         * Step 1: touch app:common, no abi change
         */

        testCase(
            incrementalPath = touchAndGet("app", "commonMain", "PlainPublicClass.kt"),
            executedTasks = setOf(
                ":app:compileKotlinJvm",
                ":app:compileKotlinJs",
                ":app:compileKotlinNative"
            )
        )

        /**
         * Step 2: touch app:jvm, no abi change
         */

        val appJvmClassKt = touchAndGet("app", "jvmMain", "PlainPublicClassJvm.kt")
        testCase(
            incrementalPath = null, //TODO: it just doesn't print "Incremental compilation completed", why? (KT-63476)
            executedTasks = setOf(":app:compileKotlinJvm")
        ) {
            assertCompiledKotlinSources(listOf(appJvmClassKt).relativizeTo(projectPath), output)
        }

        /**
         * Step 3: touch app:js, no abi change
         */

        testCase(
            incrementalPath = touchAndGet("app", "jsMain", "PlainPublicClassJs.kt"),
            executedTasks = setOf(":app:compileKotlinJs"),
        )

        /**
         * Step 4: touch app:native, no abi change
         */

        touchAndGet("app", "nativeMain", "PlainPublicClassNative.kt")
        testCase(
            incrementalPath = null, // no native incremental compilation - see KT-62824
            executedTasks = setOf(":app:compileKotlinNative"),
        )

        /**
         * Step 5: touch lib:common, no abi change
         */

        testCase(
            incrementalPath = touchAndGet("lib", "commonMain", "CommonUtil.kt"),
            executedTasks = mainCompileTasks // TODO: KT-62642 - bad compile avoidance here
        )

        /**
         * Step 6: touch lib:jvm, no abi change
         */

        val libJvmUtilKt = touchAndGet("lib", "jvmMain", "libJvmPlatformUtil.kt")
        testCase(
            incrementalPath = null, //TODO: it just doesn't print "Incremental compilation completed", why? (KT-63476)
            executedTasks = setOf(":app:compileKotlinJvm", ":lib:compileKotlinJvm"),
        ) {
            assertCompiledKotlinSources(listOf(libJvmUtilKt).relativizeTo(projectPath), output)
        }

        /**
         * Step 7: touch lib:js, no abi change
         */

        testCase(
            incrementalPath = touchAndGet("lib", "jsMain", "libJsPlatformUtil.kt"),
            executedTasks = setOf(":app:compileKotlinJs", ":lib:compileKotlinJs"),
        )

        /**
         * Step 8: touch lib:native, no abi change
         */

        touchAndGet("lib", "nativeMain", "libNativePlatformUtil.kt")
        testCase(
            incrementalPath = null,
            executedTasks = setOf(":app:compileKotlinNative", ":lib:compileKotlinNative"),
        )
    }
}

@DisplayName("Basic incremental scenarios with Kotlin Multiplatform - K1")
class BasicIncrementalCompilationK1IT : BasicIncrementalCompilationIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}