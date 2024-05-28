/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test

class KmpDslSourceSetDiagnosticsTest {

    @Test
    fun `when project has unused single custom source set, expect diagnostic UnusedSourceSetsWarning`() {
        val project = kmpProject {
            sourceSets.apply {
                create("foo")
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
    }

    @Test
    fun `when project has unused custom source set branch, expect diagnostic UnusedSourceSetsWarning`() {
        val project = kmpProject {
            sourceSets.apply {
                val foo = create("foo")
                val bar = create("bar")
                bar.dependsOn(foo)
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
    }

    @Test
    fun `when project has custom source sets branch, expect no diagnostic UnusedSourceSetsWarning`() {
        val project = kmpProject {
            sourceSets.apply {
                val foo = create("foo")
                val bar = create("bar")
                bar.dependsOn(foo)
                named("jvmMain") {
                    it.dependsOn(bar)
                }
            }
        }

        project.evaluate()
        project.assertNoDiagnostics(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
    }

    companion object {
        private fun kmpProject(
            configureKmp: KotlinMultiplatformExtension.() -> Unit = {},
        ): ProjectInternal {
            return buildProjectWithMPP {
                with(multiplatformExtension) {
                    jvm()

                    js {
                        nodejs()
                    }
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        nodejs()
                    }

                    linuxX64()
                    mingwX64()
                    macosX64()
                    macosArm64()

                    applyDefaultHierarchyTemplate()

                    configureKmp()
                }
            }
        }
    }
}
