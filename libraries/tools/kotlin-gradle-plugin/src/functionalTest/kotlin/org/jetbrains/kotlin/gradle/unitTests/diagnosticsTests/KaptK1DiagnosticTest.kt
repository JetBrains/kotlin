/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.named
import org.junit.Test

class KaptK1DiagnosticTest {

    @Test
    fun testNoDiagnosticByDefault() {
        val project = buildProjectWithJvm {
            plugins.apply("org.jetbrains.kotlin.kapt")
        }

        project.evaluate()

        project.tasks.named<KaptGenerateStubsTask>("kaptGenerateStubsKotlin").get().useK2Kapt.get()
        project.assertNoDiagnostics()
    }

    @Test
    fun testNoDiagnosticWhenKaptK2IsEnabledExplicitly() {
        val project = buildProjectWithJvm(
            preApplyCode = {
                project.extraProperties.set("kapt.use.k2", true)
            }
        ) {
            plugins.apply("org.jetbrains.kotlin.kapt")
        }

        project.evaluate()

        project.tasks.named<KaptGenerateStubsTask>("kaptGenerateStubsKotlin").get().useK2Kapt.get()
        project.assertNoDiagnostics()
    }

    @Test
    fun testDiagnosticIsPresentWithKaptK1ModeEnabled() {
        val project = buildProjectWithJvm(
            preApplyCode = {
                project.extraProperties.set("kapt.use.k2", false)
            }
        ) {
            plugins.apply("org.jetbrains.kotlin.kapt")
        }

        project.evaluate()

        project.tasks.named<KaptGenerateStubsTask>("kaptGenerateStubsKotlin").get().useK2Kapt.get()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.WarnKaptK1IsDeprecated())
    }
}