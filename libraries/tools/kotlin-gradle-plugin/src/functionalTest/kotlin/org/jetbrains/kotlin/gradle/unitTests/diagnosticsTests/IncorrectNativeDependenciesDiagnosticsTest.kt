/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import kotlin.test.Test

class IncorrectNativeDependenciesDiagnosticsTest {

    @Test
    fun `test - dependencies in compileOnly scope declared`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.linuxArm64()
        kotlin.linuxX64()
        kotlin.jvm()

        kotlin.sourceSets.commonMain.dependencies {
            compileOnly("my:library:1.0.0")
        }

        project.evaluate()
        project.checkDiagnostics("IncorrectNativeDependencies")
    }
}