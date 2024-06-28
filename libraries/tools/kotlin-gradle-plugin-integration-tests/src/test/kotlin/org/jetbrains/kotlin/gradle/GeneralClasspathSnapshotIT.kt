/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@JvmGradlePluginTests
@DisplayName("Classpath Snapshots transformation tests") // related mostly to the Gradle part
class GeneralClasspathSnapshotIT : KGPBaseTest() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(useGradleClasspathSnapshot = true) // ensure classpath snapshotting is enabled

    @DisplayName("Non-existent dependency files do not fail the build")
    @GradleTest
    fun nonExistentDependency(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.append(
                //language=Gradle
                """
                dependencies {
                    implementation(files("non-existent.jar"))
                }
                """.trimIndent()
            )
            build("compileKotlin") {
                assertHasDiagnostic(KotlinToolingDiagnostics.DependencyDoesNotPhysicallyExist)
            }
        }
    }
}