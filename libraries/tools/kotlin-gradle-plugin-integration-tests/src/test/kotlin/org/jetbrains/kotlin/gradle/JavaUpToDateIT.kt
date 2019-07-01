/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class JavaUpToDateIT : BaseGradleIT() {
    @Test
    fun testKotlinMethodBodyIsChanged() {
        val project = Project("javaUpToDate", GradleVersionRequired.AtLeast("4.3"))

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava", ":compileTestKotlin", ":compileTestJava")
        }

        project.projectFile("MainKotlinClass.kt").modify {
            it.replace(
                "fun number(): Int = 0",
                "fun number(): Int = 1"
            )
        }
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            assertTasksUpToDate(":compileJava", ":compileTestJava")
        }
    }

    @Test
    fun testKotlinNewLineAdded() {
        val project = Project("javaUpToDate", GradleVersionRequired.AtLeast("4.3"))

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava", ":compileTestKotlin", ":compileTestJava")
        }

        project.projectFile("MainKotlinClass.kt").modify { "\n$it" }
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            assertTasksUpToDate(":compileJava", ":compileTestJava")
        }
    }

    @Test
    fun testPrivateMethodSignatureChanged() {
        val project = Project("javaUpToDate", GradleVersionRequired.AtLeast("4.3"))

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava", ":compileTestKotlin", ":compileTestJava")
        }

        project.projectFile("MainKotlinClass.kt").modify {
            it.replace(
                "private fun privateMethod() = 0",
                "private fun privateMethod() = \"0\""
            )
        }
        project.build("build") {
            assertSuccessful()
            // see https://github.com/gradle/gradle/issues/5013
            assertTasksExecuted(":compileKotlin", ":compileJava", ":compileTestKotlin", ":compileTestJava")
        }
    }
}