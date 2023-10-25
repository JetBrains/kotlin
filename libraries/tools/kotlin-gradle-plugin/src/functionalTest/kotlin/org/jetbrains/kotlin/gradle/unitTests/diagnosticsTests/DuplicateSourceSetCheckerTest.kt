/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import kotlin.test.Test
import kotlin.test.assertFails

class DuplicateSourceSetCheckerTest {

    @Test
    fun `target with custom name duplicates defualt name failes build`() {
        val project = buildProjectWithMPP {
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64("linUX")
        }
        assertFails { project.evaluate() }
        project.checkDiagnostics("DuplicateSourceSetChecker")
    }

    @Test
    fun `target with custom name does not produce any diagnostics`() {
        val project = buildProjectWithMPP {
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64("custom")
        }
        project.evaluate()
        project.assertNoDiagnostics()
    }

    @Test
    fun `several targets without custom name don't produce any diagnostics`() {
        val project = buildProjectWithMPP {
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64()
            project.multiplatformExtension.mingwX64()
        }
        project.evaluate()
        project.assertNoDiagnostics()
    }
}