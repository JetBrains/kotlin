/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinNativeArtifactDSL
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.ExperimentalArtifactsDslUsed
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume.assumeTrue
import kotlin.test.BeforeTest
import kotlin.test.Test

@Suppress("FunctionName")
@OptIn(KotlinNativeArtifactDSL.ExperimentalArtifactDsl::class)
class KotlinArtifactsDslTests {

    @Test
    fun `usage emits a warning`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosArm64()
                iosSimulatorArm64()
            }

            kotlinArtifacts {
                Native.Framework("frame") {
                    it.target = iosArm64
                }
                Native.XCFramework("xc") {
                    it.targets = setOf(iosArm64, iosSimulatorArm64)
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(ExperimentalArtifactsDslUsed)
    }

    @Test
    fun `property suppresses the warning`() {
        val project = buildProjectWithMPP {
            propertiesExtension.set(KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING, "true")

            kotlin {
                iosArm64()
                iosSimulatorArm64()
            }

            kotlinArtifacts {
                Native.Framework("frame") {
                    it.target = iosArm64
                }
                Native.XCFramework("xc") {
                    it.targets = setOf(iosArm64, iosSimulatorArm64)
                }
            }
        }

        project.evaluate()
        project.assertNoDiagnostics(ExperimentalArtifactsDslUsed)
    }

}