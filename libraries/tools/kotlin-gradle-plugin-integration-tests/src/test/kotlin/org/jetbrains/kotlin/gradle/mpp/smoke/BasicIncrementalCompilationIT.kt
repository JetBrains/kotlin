/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.mpp.KmpIncrementalITBase
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * Basic smoke tests assert that the baseline IC is reasonable: if we do a local change, only a local rebuild is performed.
 */
@DisplayName("Basic incremental scenarios with KMP - K2")
@MppGradlePluginTests
open class BasicIncrementalCompilationIT : KmpIncrementalITBase() {

    @DisplayName("Base test case - local change, local recompilation")
    @GradleTest
    @TestMetadata("generic-kmp-app-plus-lib-with-tests")
    fun testStrictlyLocalChange(gradleVersion: GradleVersion): Unit = withProject(gradleVersion) {
        build("assemble")

        /**
         * Step 1: touch app:common, no abi change
         */

        val sourceInAppCommon = resolvePath("app", "commonMain", "Unused.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(
                ":app:compileCommonMainKotlinMetadata",
                ":app:compileKotlinJvm",
                ":app:compileKotlinJs",
                ":app:compileKotlinNative"
            )
        ) {
            assertIncrementalCompilation(listOf(sourceInAppCommon).relativizeTo(projectPath))
        }

        /**
         * Step 2: touch app:jvm, no abi change
         */

        val sourceInAppJvm = resolvePath("app", "jvmMain", "UnusedJvm.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(":app:compileKotlinJvm")
        ) {
            //TODO: it just doesn't print "Incremental compilation completed", why? (KT-63476)
            assertCompiledKotlinSources(listOf(sourceInAppJvm).relativizeTo(projectPath), output)
        }

        /**
         * Step 3: touch app:js, no abi change
         */

        val sourceInAppJs = resolvePath("app", "jsMain", "UnusedJs.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(":app:compileKotlinJs"),
        ) {
            assertIncrementalCompilation(listOf(sourceInAppJs).relativizeTo(projectPath))
        }

        /**
         * Step 4: touch app:native, no abi change
         */

        resolvePath("app", "nativeMain", "UnusedNative.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(":app:compileKotlinNative"),
        )

        /**
         * Step 5: touch lib:common, no abi change
         */

        val sourceInLibCommon = resolvePath("lib", "commonMain", "UsedInLibPlatformTests.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = mainCompileTasks // TODO: KT-62642 - bad compile avoidance here
        ) {
            assertIncrementalCompilation(listOf(sourceInLibCommon).relativizeTo(projectPath))
        }

        /**
         * Step 6: touch lib:jvm, no abi change
         */

        val sourceInLibJvm = resolvePath("lib", "jvmMain", "UsedInAppJvmAndLibTests.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(
                ":app:compileKotlinJvm",
                ":lib:compileKotlinJvm"
            ),
        ) {
            assertCompiledKotlinSources(listOf(sourceInLibJvm).relativizeTo(projectPath), output)
        }

        /**
         * Step 7: touch lib:js, no abi change
         */

        val sourceInLibJs = resolvePath("lib", "jsMain", "UsedInAppJsAndLibTests.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(
                ":app:compileKotlinJs",
                ":lib:compileKotlinJs"
            ),
        ) {
            assertIncrementalCompilation(listOf(sourceInLibJs).relativizeTo(projectPath))
        }

        /**
         * Step 8: touch lib:native, no abi change
         */

        resolvePath("lib", "nativeMain", "UsedInAppNativeAndLibTests.kt").addPrivateVal()
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(
                ":app:compileKotlinNative",
                ":lib:compileKotlinNative"
            ),
        )
    }
}

@DisplayName("Basic incremental scenarios with Kotlin Multiplatform - K1")
class BasicIncrementalCompilationK1IT : BasicIncrementalCompilationIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}