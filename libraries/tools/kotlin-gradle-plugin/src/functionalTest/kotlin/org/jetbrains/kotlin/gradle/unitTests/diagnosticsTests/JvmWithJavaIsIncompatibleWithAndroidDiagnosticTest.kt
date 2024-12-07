/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.androidApplication
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import kotlin.test.Test
import kotlin.test.assertFails

class JvmWithJavaIsIncompatibleWithAndroidDiagnosticTest {

    @Test
    fun `test - withJava and android library`() {
        val project = buildProjectWithMPP()
        project.androidLibrary { compileSdk = 33 }
        project.multiplatformExtension.jvm().withJava()
        project.multiplatformExtension.androidTarget()

        assertFails { project.evaluate() }
        project.checkDiagnostics("JvmWithJavaIsIncompatibleWithAndroid-withJava-withAndroidLibrary")
    }

    @Test
    fun `test - withJava and android application`() {
        val project = buildProjectWithMPP()
        project.androidApplication { compileSdk = 33 }
        project.multiplatformExtension.jvm().withJava()
        project.multiplatformExtension.androidTarget()
        assertFails { project.evaluate() }

        project.checkDiagnostics("JvmWithJavaIsIncompatibleWithAndroid-withJava-withAndroidApplication")
    }

    @Test
    fun `test - withJava - without android plugin`() {
        val project = buildProjectWithMPP()
        project.androidLibrary { compileSdk = 33 }
        project.multiplatformExtension.jvm()
        project.multiplatformExtension.androidTarget()
        project.evaluate()

        project.checkDiagnostics("JvmWithJavaIsIncompatibleWithAndroid-withJava-withoutAndroidPlugin")
    }
}