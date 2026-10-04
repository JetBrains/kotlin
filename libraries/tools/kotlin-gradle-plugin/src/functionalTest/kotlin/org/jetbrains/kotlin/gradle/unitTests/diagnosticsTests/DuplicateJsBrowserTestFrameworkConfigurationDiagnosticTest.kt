/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DuplicateJsBrowserTestFrameworkConfigurationDiagnosticTest {

    @Test
    fun `diagnostic not reported if test framework not configured with either DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {}
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        project.assertNoDiagnostics()
    }

    @Test
    fun `diagnostic not reported if test framework is only configured via new DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {
                        test {
                            it.chromium()
                        }
                    }
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        project.assertNoDiagnostics()
    }

    @Test
    fun `diagnostic not reported if test framework is only configured via old DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {
                        testTask {
                            it.useKarma()
                        }
                    }
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        project.assertNoDiagnostics()
    }

    @Test
    fun `diagnostic reported if test framework is configured via old DSL after new DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {
                        test {
                            it.chromium()
                        }
                        testTask {
                            it.useKarma()
                        }
                    }
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        val diagnostic = project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project.path)
            .assertContainsDiagnostic(KotlinToolingDiagnostics.DuplicateJsBrowserTestFrameworkConfiguration)
        val throwable = assertNotNull(diagnostic.throwable)
        assertTrue(throwable.stackTrace.any { it.fileName == "ConfigureKotlinPlaywrightTestRunner.kt" })
    }

    @Test
    fun `diagnostic reported if test framework is configured via new DSL after old DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {
                        testTask {
                            it.useKarma()
                        }
                        test {
                            it.chromium()
                        }
                    }
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        val diagnostic = project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project.path)
            .assertContainsDiagnostic(KotlinToolingDiagnostics.DuplicateJsBrowserTestFrameworkConfiguration)
        val throwable = assertNotNull(diagnostic.throwable)
        assertTrue(throwable.stackTrace.any { it.fileName == "ConfigureKotlinPlaywrightTestRunner.kt" })
    }
}
