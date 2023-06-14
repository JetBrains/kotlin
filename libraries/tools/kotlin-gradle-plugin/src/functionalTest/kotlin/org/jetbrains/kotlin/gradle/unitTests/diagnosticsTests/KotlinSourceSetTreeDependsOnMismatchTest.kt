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
                androidTarget()
                project.androidApplication { compileSdk = 32 }
                iosX64(); iosArm64(); iosSimulatorArm64()
                macosX64(); macosArm64()

                configure()
            }
        }
        project.evaluate()
        return project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project).toList()
    }

    private fun checkSingleBadSourceSetDependency(
        dependent: String,
        dependency: String,
        expectedDiagnostic: ToolingDiagnostic = KotlinSourceSetTreeDependsOnMismatch(dependent, dependency)
    ) = checkDiagnostics {
        sourceSets.getByName(dependent).dependsOn(sourceSets.getByName(dependency))
    }.assertContainsDiagnostic(expectedDiagnostic)

    @Test
    fun `no diagnostics should be reported for correctly configured project`() {
        checkDiagnostics {
            // no extra configurations
        }.assertNoDiagnostics(KotlinSourceSetTreeDependsOnMismatch.id)
    }

    @Test
    fun `commonTest cant depend on commonMain`() = checkSingleBadSourceSetDependency(
        dependent = "commonTest",
        dependency = "commonMain",
        // Since for given test project commonMain have only one dependent (nativeMain) it is
        // impossible to figure out wrong dependency via "white crow" heuristic
        expectedDiagnostic = KotlinSourceSetTreeDependsOnMismatch(
            dependents = mapOf(
                "test" to listOf("commonTest"),
                "main" to listOf("nativeMain"),
            ),
            dependencyName = "commonMain"
        )
    )

    @Test
    fun `commonMain cant depend on commonTest`() = checkSingleBadSourceSetDependency(
        dependent = "commonMain",
        dependency = "commonTest",
        expectedDiagnostic = KotlinSourceSetTreeDependsOnMismatch(
            dependents = mapOf(
                "main" to listOf("commonMain"),
                "test" to listOf("nativeTest"),
            ),
            dependencyName = "commonTest"
        )
    )

    @Test
    fun `appleTest cant depend on appleMain`() = checkSingleBadSourceSetDependency(
        dependent = "appleTest",
        dependency = "appleMain"
    )

    @Test
    fun `appleMain cant depend on appleTest`() = checkSingleBadSourceSetDependency(
        dependent = "appleMain",
        dependency = "appleTest"
    )

    @Test
    fun `iosTest cant depend on iosMain`() = checkSingleBadSourceSetDependency(
        dependent = "iosTest",
        dependency = "iosMain"
    )

    @Test
    fun `iosMain cant depend on iosTest`() = checkSingleBadSourceSetDependency(
        dependent = "iosMain",
        dependency = "iosTest"
    )

    @Test
    fun `iosX64Test cant depend on iosX64Main`() = checkSingleBadSourceSetDependency(
        dependent = "iosX64Test",
        dependency = "iosX64Main"
    )

    @Test
    fun `iosX64Main cant depend on iosX64Test`() = checkSingleBadSourceSetDependency(
        dependent = "iosX64Main",
        dependency = "iosX64Test"
    )

    @Test
    fun `iosX64Test and iosArm64Test cant depend on iosMain`() = checkDiagnostics {
        sourceSets.getByName("iosX64Test").dependsOn(sourceSets.getByName("iosMain"))
        sourceSets.getByName("iosArm64Test").dependsOn(sourceSets.getByName("iosMain"))
    }.assertContainsDiagnostic(
        KotlinSourceSetTreeDependsOnMismatch(
            dependents = mapOf(
                "main" to listOf("iosArm64Main", "iosSimulatorArm64Main", "iosX64Main"),
                "test" to listOf("iosArm64Test", "iosX64Test")
            ),
            dependencyName = "iosMain"
        )
    )

    @Test
    fun `test that only lowest source set edges are reported`() = checkDiagnostics {
        sourceSets.getByName("iosX64Test").dependsOn(sourceSets.getByName("iosMain"))
        sourceSets.getByName("iosArm64Test").dependsOn(sourceSets.getByName("nativeMain"))
        sourceSets.getByName("iosSimulatorArm64Main").dependsOn(sourceSets.getByName("commonMain"))
    }.assertDiagnostics(
        // Expected only one diagnostic since "nativeMain" and "commonMain" is a "bad source sets" because
        // iosMain depends on all of them transitively and thus coloring them as "bad source sets" as well.
        // Thus, only the lowest "bad source set" should be reported
        KotlinSourceSetTreeDependsOnMismatch(dependeeName = "iosX64Test", dependencyName = "iosMain")
    )

    @Test
    fun `test that few incorrect source set dependencies can be reported`() = checkDiagnostics {
        sourceSets.getByName("iosX64Test").dependsOn(sourceSets.getByName("iosMain"))
        sourceSets.getByName("macosX64Test").dependsOn(sourceSets.getByName("macosMain"))
        sourceSets.getByName("macosArm64Test").dependsOn(sourceSets.getByName("macosMain"))
    }.assertDiagnostics(
        KotlinSourceSetTreeDependsOnMismatch(dependeeName = "iosX64Test", dependencyName = "iosMain"),
        KotlinSourceSetTreeDependsOnMismatch(
            dependents = mapOf(
                "main" to listOf("macosArm64Main", "macosX64Main"),
                "test" to listOf("macosArm64Test", "macosX64Test")
            ),
            dependencyName = "macosMain"
        ),
    )
}