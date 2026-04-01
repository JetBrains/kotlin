/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

// TODO https://youtrack.jetbrains.com/issue/KT-76062/Maven-remove-Kotlin-script-execution-support: script execution support is deprecated and planned for removal. Remove this class when done.
class ScriptExecutionIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Script can access Maven build info")
    fun testExecuteKotlinScriptBuildAccess(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptBuildAccess", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "[INFO] kotlin build script accessing build info of test-executeKotlinScriptBuildAccess project",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Script compilation error is reported")
    fun testExecuteKotlinScriptCompileError(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptCompileError", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Unresolved reference: compileErrorHere",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Script from external .kts file executes")
    fun testExecuteKotlinScriptFile(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptFile", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from Kotlin script file!",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Inline script executes")
    fun testExecuteKotlinScriptInline(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptInline", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from inline Kotlin script!",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Script runtime exception is reported")
    fun testExecuteKotlinScriptScriptException(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptScriptException", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "[ERROR] InvocationTargetException: exception from script",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Script with external dependencies resolves them")
    fun testExecuteKotlinScriptWithDependencies(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptWithDependencies", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Dependency jar is: junit-4.13.1.jar",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Script with custom template executes")
    fun testExecuteKotlinScriptWithTemplate(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptWithTemplate", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from Kotlin script file!"
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Inline script using JDK 17 APIs")
    fun testJava17ExecuteKotlinScriptInlineJdkDep(mavenVersion: TestVersions.Maven) {
        testProject("java17/test-executeKotlinScriptInlineJdkDep", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_17)
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from inline Kotlin script using java!",
                )
            }
        }
    }
}
