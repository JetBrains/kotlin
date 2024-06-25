/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import kotlin.test.Test

class KmpWithJavaPluginsDeprecationDiagnosticTest {

    @Test
    fun testKmpWithJavaPluginApplied() {
        val project = buildProjectWithMPP {
            plugins.apply("java")
            multiplatformExtension.jvm()
        }

        project.evaluate()
        project.checkDiagnostics("withJavaPlugin".diagnosticLocation)
    }

    @Test
    fun testKmpWithJavaLibraryPluginApplied() {
        val project = buildProjectWithMPP {
            plugins.apply("java-library")
            multiplatformExtension.jvm()
        }

        project.evaluate()
        project.checkDiagnostics("withJavaLibraryPlugin".diagnosticLocation)
    }

    @Test
    fun testKmpWithApplicationPluginApplied() {
        val project = buildProjectWithMPP {
            plugins.apply("application")
            multiplatformExtension.jvm()
        }

        project.evaluate()
        project.checkDiagnostics("withApplicationPlugin".diagnosticLocation)
    }

    private val String.diagnosticLocation get() = "KmpWithJavaPlugins/$this"
}