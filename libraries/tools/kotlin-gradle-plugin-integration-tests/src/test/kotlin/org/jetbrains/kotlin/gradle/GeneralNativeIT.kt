/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class GeneralNativeIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun testParallelExecutionDetection(): Unit = with(transformProjectWithPluginsDsl("native-parallel")) {
        val compileTasks = arrayOf(":one:compileKotlinLinux", ":two:compileKotlinLinux")
        // Check that parallel in-process execution fails with the corresponding message.
        build(*compileTasks) {
            assertFailed()
            assertContains("Parallel in-process execution of the Kotlin/Native compiler detected.")
        }

        // Parallel execution without daemon => ok.
        build("clean", *compileTasks, "-Pkotlin.native.disableCompilerDaemon=true") {
            assertSuccessful()
            assertTasksExecuted(*compileTasks)
        }

        // org.gradle.parallel must be set in the properties file (not in command line).
        projectDir.resolve("gradle.properties").modify {
            it.replace("org.gradle.parallel=true", "org.gradle.parallel=false")
        }

        // Sequential execution => ok.
        build("clean", *compileTasks) {
            assertSuccessful()
            assertTasksExecuted(*compileTasks)
        }

        // Check for KT-37696.
        // Add an incorrect code to trigger compilation failure.
        projectDir.resolve("one/src/linuxMain/kotlin/main.kt").appendText("\nfun incorrect")
        gradleBuildScript("two").appendText("""
            
            val compileKotlinLinux by tasks.getting
            compileKotlinLinux.mustRunAfter(":one:compileKotlinLinux")            
        """.trimIndent())

        build("clean", *compileTasks, "--continue") {
            assertFailed()
            assertTasksFailed(":one:compileKotlinLinux")
            assertTasksExecuted(":two:compileKotlinLinux")
            assertNotContains("Parallel in-process execution of the Kotlin/Native compiler detected.")
        }
    }

    // TODO: Move native specific tests from NewMultiplatformIT here.
}