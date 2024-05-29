/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test

class JsLikeDiagnosticsFunctionalTest {

    @Test
    fun testJsReportWarning() {
        val project = buildProjectWithMPP {
            kotlin {
                js()
            }
        }

        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.JsEnvironmentNotChosenExplicitly)
    }

    @Test
    @OptIn(ExperimentalWasmDsl::class)
    fun testWasmJsLikeReportWarning() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmJs()
            }
        }

        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.WasmJsEnvironmentNotChosenExplicitly)
    }

    @Test
    @OptIn(ExperimentalWasmDsl::class)
    fun testWasmWasiLikeReportWarning() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi()
            }
        }

        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.WasmWasiEnvironmentNotChosenExplicitly)
    }

    @Test
    fun testJsNotReportWarning() {
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    nodejs()
                }
            }
        }

        project.evaluate()

        project.assertNoDiagnostics(KotlinToolingDiagnostics.JsEnvironmentNotChosenExplicitly)
    }

    @Test
    @OptIn(ExperimentalWasmDsl::class)
    fun testWasmJsNotReportWarning() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmJs {
                    nodejs()
                }
            }
        }

        project.evaluate()

        project.assertNoDiagnostics(KotlinToolingDiagnostics.WasmJsEnvironmentNotChosenExplicitly)
    }

    @Test
    @OptIn(ExperimentalWasmDsl::class)
    fun testWasmWasiNotReportWarning() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    nodejs()
                }
            }
        }

        project.evaluate()

        project.assertNoDiagnostics(KotlinToolingDiagnostics.WasmWasiEnvironmentNotChosenExplicitly)
    }
}
