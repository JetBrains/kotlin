/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("FunctionName")
package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinSourceSetTreeDependsOnMismatch
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import kotlin.test.*

class KotlinSourceSetTreeDependsOnMismatchTest {
    private fun checkDiagnostics(configure: KotlinMultiplatformExtension.() -> Unit): List<ToolingDiagnostic> {
        val project = buildProjectWithMPP {
            kotlin {
                targetHierarchy.default()
                project.androidApplication { compileSdk = 32 }
                iosX64(); iosArm64(); iosSimulatorArm64()
                watchosArm64(); watchosX64(); watchosSimulatorArm64()
                macosX64(); macosArm64()
                linuxX64(); linuxArm64()

                configure()
            }
        }
        project.evaluate()
        return project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project).toList()
    }

    private fun checkBadSourceSetDependency(
        correctDependent: String,
        incorrectDependent: String,
        dependency: String
    ) = checkDiagnostics {
        sourceSets.getByName(incorrectDependent).dependsOn(sourceSets.getByName(dependency))
    }.assertContainsDiagnostic(
        KotlinSourceSetTreeDependsOnMismatch(correctDependent, incorrectDependent, dependency)
    )

    private fun checkBadSourceSetDependency(
        incorrectDependent: String,
        dependency: String
    ) = checkDiagnostics {
        sourceSets.getByName(incorrectDependent).dependsOn(sourceSets.getByName(dependency))
    }.assertContainsDiagnostic(
        KotlinSourceSetTreeDependsOnMismatch(incorrectDependent, dependency)
    )

    @Test
    fun `no diagnostics should be reported for correctly configured project`() {
        checkDiagnostics {
            // no extra configurations
        }.assertNoDiagnostics(KotlinSourceSetTreeDependsOnMismatch.id)
    }

    @Test
    fun `commonTest cant depend on commonMain`() = checkBadSourceSetDependency(
        correctDependent = "nativeMain",
        incorrectDependent = "commonTest",
        dependency = "commonMain"
    )

    @Test
    fun `commonMain cant depend on commonTest`() = checkBadSourceSetDependency(
        correctDependent = "nativeTest",
        incorrectDependent = "commonMain",
        dependency = "commonTest"
    )

    @Test
    fun `appleTest cant depend on appleMain`() = checkBadSourceSetDependency(
        correctDependent = "iosMain",
        incorrectDependent = "appleTest",
        dependency = "appleMain"
    )

    @Test
    fun `iosX64Test cant depend on iosX64Main`() = checkBadSourceSetDependency(
        incorrectDependent = "iosX64Test",
        dependency = "iosX64Main",
    )

    @Test
    fun `iosX64Test cant depend on commonMain`() = checkBadSourceSetDependency(
        correctDependent = "nativeMain",
        incorrectDependent = "iosX64Test",
        dependency = "commonMain",
    )
}