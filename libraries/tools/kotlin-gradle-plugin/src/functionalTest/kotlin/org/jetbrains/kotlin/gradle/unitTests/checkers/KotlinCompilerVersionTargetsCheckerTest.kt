/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSeverity
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinCompilerVersionTargetsCheckerTest {

    @Test
    fun `custom compiler version with native targets - emits diagnostic`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64()
                linuxArm64()
                compilerVersion.set(COMPILER_VERSION)
            }
        }.evaluate()

        val diagnostic = KotlinToolingDiagnostics.KotlinCompilerVersionNotAppliedToNativeTargets(
            COMPILER_VERSION,
            project.getKotlinPluginVersion(),
            listOf("linuxArm64", "linuxX64"),
        )
        project.assertContainsDiagnostic(diagnostic)
        // The mismatch must fail the build, not only warn
        val reported = project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project.path).single { it.id == diagnostic.id }
        assertEquals(KotlinToolingDiagnosticsSeverity.ERROR, reported.severity)
    }

    @Test
    fun `custom compiler version without native targets - no diagnostic`() {
        buildProjectWithMPP {
            kotlin {
                jvm()
                js()
                compilerVersion.set(COMPILER_VERSION)
            }
        }.evaluate().assertNoDiagnostics(KotlinToolingDiagnostics.KotlinCompilerVersionNotAppliedToNativeTargets)
    }

    @Test
    fun `default compiler version with native targets - no diagnostic`() {
        buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64()
            }
        }.evaluate().assertNoDiagnostics(KotlinToolingDiagnostics.KotlinCompilerVersionNotAppliedToNativeTargets)
    }

    @Test
    fun `compiler version of the plugin with native targets - no diagnostic`() {
        buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64()
                compilerVersion.set(getKotlinPluginVersion())
            }
        }.evaluate().assertNoDiagnostics(KotlinToolingDiagnostics.KotlinCompilerVersionNotAppliedToNativeTargets)
    }

    private companion object {
        const val COMPILER_VERSION = "2.4.20"
    }
}
