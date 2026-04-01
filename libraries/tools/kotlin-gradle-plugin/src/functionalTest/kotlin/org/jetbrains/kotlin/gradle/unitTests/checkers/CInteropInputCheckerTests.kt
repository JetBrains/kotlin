/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

class CInteropInputCheckerTests {

    @Test
    fun `missing def file - emits diagnostic`() {
        buildProjectWithMPP {
            tasks.withType(CInteropProcess::class.java).all {  }
            kotlin {
                linuxArm64().compilations.getByName("main").cinterops.create("foo")
            }
        }.evaluate().assertContainsDiagnostic(
            KotlinToolingDiagnostics.CInteropRequiredParametersNotSpecifiedError
        )
    }

    @Test
    fun `missing def file - skips diagnostic in generated cinterop`() {
        buildProjectWithMPP {
            kotlin {
                linuxArm64().compilations.getByName("main").cinterops.create("foo") {
                    it.isGeneratedCinterop = true
                }
                tasks.withType(CInteropProcess::class.java).all {  }
            }
        }.evaluate().assertNoDiagnostics()
    }

}