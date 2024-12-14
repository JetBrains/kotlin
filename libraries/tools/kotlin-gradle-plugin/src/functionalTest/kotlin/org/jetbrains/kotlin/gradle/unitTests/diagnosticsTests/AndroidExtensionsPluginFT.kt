/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidExtensionsPluginFT {

    @Test
    fun producesErrorSeverityWarningByDefault() {
        val project = buildProject()
        project.plugins.apply("kotlin-android-extensions")

        project.evaluate()

        project.assertContainsDiagnostic(
            KotlinToolingDiagnostics.AndroidExtensionPluginRemoval()
        )

        assertTrue(project.extensions.findByType(AndroidExtensionsExtension::class.java) == null)
    }

    @Test
    fun producesWarningSeverityWhenConfigured() {
        val project = buildProject()
        project.propertiesExtension.set("kotlin.androidExtensionsPlugin.enabled", true)
        project.plugins.apply("kotlin-android-extensions")

        project.evaluate()

        project.assertContainsDiagnostic(
            KotlinToolingDiagnostics.AndroidExtensionPluginRemoval(ToolingDiagnostic.Severity.WARNING)
        )

        assertTrue(project.extensions.findByType(AndroidExtensionsExtension::class.java) != null)
    }
}