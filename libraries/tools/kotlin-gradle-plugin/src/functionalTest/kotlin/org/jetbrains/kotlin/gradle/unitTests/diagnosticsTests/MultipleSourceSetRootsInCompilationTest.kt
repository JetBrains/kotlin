/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.checkDiagnosticsWithMppProject
import kotlin.test.Test

class MultipleSourceSetRootsInCompilationTest {
    private fun checkDiagnostics(name: String, projectConfiguration: Project.() -> Unit) =
        checkDiagnosticsWithMppProject("MultipleSourceSetRootsInCompilationTest/${name}", projectConfiguration)

    @Test
    fun extraSourceSetInEachTarget() {
        // It is expected to see diagnostic reported for each extra source set
        checkDiagnostics("extraSourceSetInEachTarget") {
            kotlin {
                fun KotlinTarget.addExtraSourceSets() {
                    val extraMain = project.multiplatformExtension.sourceSets.create(disambiguateName("extraMain"))
                    val extraTest = project.multiplatformExtension.sourceSets.create(disambiguateName("extraTest"))
                    compilations.getByName("main").defaultSourceSet.dependsOn(extraMain)
                    compilations.getByName("test").defaultSourceSet.dependsOn(extraTest)
                }

                jvm {
                    addExtraSourceSets()
                }

                js {
                    nodejs()
                    addExtraSourceSets()
                }

                linuxX64 {
                    addExtraSourceSets()
                }
            }
        }
    }

    @Test
    fun secondCommonMain() {
        // commonMain2 is included to multiple targets, but it is expected to see only 1 diagnostic reported
        checkDiagnostics("secondCommonMain") {
            kotlin {
                val commonMain2 = sourceSets.create("commonMain2")
                listOf(jvm(), js { nodejs() }, linuxX64()).forEach {
                    it.compilations.getByName("main").defaultSourceSet.dependsOn(commonMain2)
                }
            }
        }
    }

    @Test
    fun customCompilations() {
        checkDiagnostics("customCompilations") {
            // Diagnostic should be reported only for JVM target.
            // Because of ambiguity between commonIntegrationTest and jvmIntegrationTest2.
            kotlin {
                val commonIntegrationTest = sourceSets.create("commonIntegrationTest")
                val jvmIntegrationTest2 = sourceSets.create("jvmIntegrationTest2")

                jvm {
                    val integrationTest = compilations.create("integrationTest")
                    integrationTest.defaultSourceSet.dependsOn(commonIntegrationTest)
                    integrationTest.defaultSourceSet.dependsOn(jvmIntegrationTest2)
                }

                linuxX64 {
                    compilations.create("integrationTest").defaultSourceSet.dependsOn(commonIntegrationTest)
                }

                js { nodejs() }
            }
        }
    }

    @Test
    fun androidProjectWithMultipleVariants() {
        val project = buildProjectWithMPP {
            androidApplication {
                compileSdk = 33

                flavorDimensions.add("country")
                productFlavors {
                    create("arstozka") { it.dimension = "country" }
                    create("kolechia") { it.dimension = "country" }
                }
            }

            kotlin {
                linuxX64()
                androidTarget {

                }
            }
        }

        project.evaluate()
        project.assertNoDiagnostics()
    }
}