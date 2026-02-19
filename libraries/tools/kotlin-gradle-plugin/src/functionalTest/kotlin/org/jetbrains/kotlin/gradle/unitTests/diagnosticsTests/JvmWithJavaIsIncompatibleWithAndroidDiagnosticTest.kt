/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.Assumptions
import kotlin.test.Test
import kotlin.test.assertFails

class JvmWithJavaIsIncompatibleWithAndroidDiagnosticTest {

    @Test
    fun `test - withJava and android library`() {
        Assumptions.assumeTrue(GradleVersion.current() < GradleVersion.version("9.0"), ".withJava() is not supported with Gradle 9")
        val project = buildProjectWithMPP()
        project.androidLibrary { compileSdk = 33 }
        @Suppress("DEPRECATION")
        project.multiplatformExtension.jvm().withJava()
        @Suppress("DEPRECATION")
        project.multiplatformExtension.androidTarget()

        assertFails { project.evaluate() }
        project.checkDiagnostics("JvmWithJavaIsIncompatibleWithAndroid-withJava-withAndroidLibrary")
    }

    @Test
    fun `test - withJava and android application`() {
        Assumptions.assumeTrue(GradleVersion.current() < GradleVersion.version("9.0"), ".withJava() is not supported with Gradle 9")
        val project = buildProjectWithMPP()
        project.androidApplication { compileSdk = 33 }
        @Suppress("DEPRECATION")
        project.multiplatformExtension.jvm().withJava()
        @Suppress("DEPRECATION")
        project.multiplatformExtension.androidTarget()
        assertFails { project.evaluate() }

        project.checkDiagnostics("JvmWithJavaIsIncompatibleWithAndroid-withJava-withAndroidApplication")
    }

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
