/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

class NoBrowserSpecifiedForJsBrowserTestFrameworkDiagnosticTest {
    @Test
    fun `diagnostic reported if no browser is configured in the test DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {
                        test {}
                    }
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.NoBrowserSpecifiedForJsBrowserTestFramework)
    }

    @Test
    fun `diagnostic not reported if a browser is configured in the test DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {
                        test {
                            it.firefox()
                        }
                    }
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        project.assertNoDiagnostics(KotlinToolingDiagnostics.NoBrowserSpecifiedForJsBrowserTestFramework)
    }

    @Test
    fun `diagnostic not reported if the test DSL is not used at all`() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    browser {}
                }
            }
        }

        project.evaluate()
        project.tasks.getByName("jsBrowserTest")

        project.assertNoDiagnostics(KotlinToolingDiagnostics.NoBrowserSpecifiedForJsBrowserTestFramework)
    }
}
