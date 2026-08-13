/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

class NoBrowserSpecifiedForJsBrowserTestFrameworkDiagnosticTest {
    @Test
    fun `diagnostic reported if non browser is configured with DSL`() {
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
}
