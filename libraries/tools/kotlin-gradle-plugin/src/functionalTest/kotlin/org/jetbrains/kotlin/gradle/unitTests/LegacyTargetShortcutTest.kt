/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

@Suppress("DEPRECATION")
class LegacyTargetShortcutTest {

    @Test
    fun `test error diagnostic with ios() legacy target shortcut`() {
        val project = setupProject {
            ios {
                binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.UnsupportedTargetShortcutError)
    }

    @Test
    fun `test error diagnostic with tvos() legacy target shortcut`() {
        val project = setupProject {
            tvos {
                binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.UnsupportedTargetShortcutError)
    }

    @Test
    fun `test error diagnostic with watchos() legacy target shortcut`() {
        val project = setupProject {
            watchos {
                binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.UnsupportedTargetShortcutError)
    }

    private fun setupProject(code: KotlinMultiplatformExtension.() -> Unit) = buildProjectWithMPP {
        kotlin {
            code()
        }
    }
}