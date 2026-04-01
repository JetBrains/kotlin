/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import NonKgpSourceSetsInAndroidExtensionAccessor
import org.jetbrains.kotlin.gradle.dsl.kotlinAndroidExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.applyKotlinAndroidPlugin
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.Test

class SourceSetsAccessInAndroidExtensionTest {
    @Test
    fun `diagnostic is not reported when sourceSets is not accessed`() {
        val project = buildProject() {
            applyKotlinAndroidPlugin()
            androidLibrary {}
        }
        project.evaluate()
        project.assertNoDiagnostics()
    }

    @Test
    fun `diagnostic is reported when sourceSets is accessed outside`() {
        val project = buildProject() {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            with(NonKgpSourceSetsInAndroidExtensionAccessor) {
                kotlinAndroidExtension.accessSourceSets()
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SourceSetsAccessInAndroidExtension)
    }
}