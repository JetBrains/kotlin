/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.util.checkDiagnosticsWithMppProject
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

class RedundantDependsOnEdgesTest {
    private fun checkDiagnostics(name: String, projectConfiguration: Project.() -> Unit) =
        checkDiagnosticsWithMppProject("RedundantDependsOnEdgesTest/${name}", projectConfiguration)

    @Test
    fun testRedundantCommonMainDependsOnEdges() = checkDiagnostics("redundantDependsOnEdges") {
        kotlin {
            // declaring of any custom (even redundant) dependsOn edge should prevent
            // from implicit applying of default hierarchy
            // so request its application explicitly
            applyDefaultHierarchyTemplate()

            jvm()
            linuxX64()

            val commonMain = sourceSets.commonMain.get()

            // two different styles of accessing source sets
            sourceSets.jvmMain { dependsOn(commonMain) }
            sourceSets.getByName("linuxX64Main").dependsOn(commonMain)
        }
    }

    @Test
    fun testNonRedundantDependsOnEdgeIsNotReported() = checkDiagnostics("nonRedundantDependsOnEdgeIsNotReported") {
        kotlin {
            applyDefaultHierarchyTemplate()

            jvm()
            linuxX64()
            mingwX64()

            val x64Main = sourceSets.create("x64Main")
            x64Main.dependsOn(sourceSets.getByName("commonMain"))
            sourceSets.getByName("linuxX64Main").dependsOn(x64Main)
            sourceSets.getByName("mingwX64Main").dependsOn(x64Main)
        }
    }

    @Test
    fun testRedundantDependsOnEdgeReportedWithCustomHierarchy() = checkDiagnostics("redundantDependsOnEdgeReportedWithCustomHierarchy") {
        kotlin {
            hierarchy.applyHierarchyTemplate {
                common {
                    withJvm()
                    group("x64") {
                        withLinuxX64()
                        withMingwX64()
                    }
                }
            }

            jvm()
            linuxX64()
            mingwX64()

            val x64Main = sourceSets.getByName("x64Main")
            x64Main.dependsOn(sourceSets.getByName("commonMain"))
            sourceSets.getByName("linuxX64Main").dependsOn(x64Main)
            sourceSets.getByName("mingwX64Main").dependsOn(x64Main)
        }
    }
}