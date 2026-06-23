/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
class BrowserBundlerAlreadyDefinedDiagnosticTest {

    @Test
    fun `diagnostic not reported if the bundler is defined only once`() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmJs {
                    browser(KotlinBrowserBundler.NONE) {}
                }
            }
        }.evaluate()

        project.assertNoDiagnostics(KotlinToolingDiagnostics.BrowserBundlerAlreadyDefined)
    }

    @Test
    fun `diagnostic not reported if the same bundler is defined twice`() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmJs {
                    browser(KotlinBrowserBundler.NONE) {}
                    browser(KotlinBrowserBundler.NONE) {}
                }
            }
        }.evaluate()

        project.assertNoDiagnostics(KotlinToolingDiagnostics.BrowserBundlerAlreadyDefined)
    }

    @Test
    fun `diagnostic reported if another bundler is defined`() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmJs {
                    browser(KotlinBrowserBundler.NONE) {}
                    browser(KotlinBrowserBundler.WEBPACK) {}
                }
            }
        }.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.BrowserBundlerAlreadyDefined)
    }
}
