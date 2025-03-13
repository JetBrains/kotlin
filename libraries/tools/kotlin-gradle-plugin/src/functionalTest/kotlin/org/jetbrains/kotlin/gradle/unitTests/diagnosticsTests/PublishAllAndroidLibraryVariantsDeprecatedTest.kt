/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

@Suppress("FunctionName")
class PublishAllAndroidLibraryVariantsDeprecatedTest {

    private val project = buildProjectWithMPP {
        androidLibrary {
            compileSdk = 30
        }

        kotlin {
            androidTarget()
            jvm()
            linuxX64("linux")
        }
    }

    @Test
    fun `default kmp + android should not report publishAllLibraryVariants deprecation warning`() {
        project.evaluate()
        project.assertNoDiagnostics(KotlinToolingDiagnostics.PublishAllAndroidLibraryVariantsDeprecated)
    }

    @Test
    fun `call to publishAllLibraryVariants should report deprecation warning`() {
        project.kotlin {
            androidTarget {
                @Suppress("DEPRECATION")
                publishAllLibraryVariants()
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.PublishAllAndroidLibraryVariantsDeprecated)
    }

    @Test
    fun `call to publishLibraryVariants should not report deprecation warning`() {
        project.kotlin {
            androidTarget {
                publishLibraryVariants("release", "debug")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(KotlinToolingDiagnostics.PublishAllAndroidLibraryVariantsDeprecated)
    }
}