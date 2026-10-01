/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

class JvmWithJavaIsIncompatibleWithAndroidDiagnosticTest {

    @Test
    fun `test - withJava - without android plugin`() {
        val project = buildProjectWithMPP()
        project.androidLibrary { compileSdk = 33 }
        project.multiplatformExtension.jvm()
        @Suppress("DEPRECATION")
        project.multiplatformExtension.androidTarget()
        project.evaluate()

        project.assertNoDiagnostics()
    }
}
