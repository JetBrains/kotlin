/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test

class KotlinNativeBinaryConfigurationTest {

    @Test
    fun `test DEBUG binary with optimized`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64 {
                    binaries.staticLib(listOf(NativeBuildType.DEBUG)) {
                        optimized = true // This is not allowed for DEBUG build type
                    }
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.IncompatibleBinaryConfiguration)
    }

    @Test
    fun `test RELEASE binary without optimized`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64 {
                    binaries.executable(listOf(NativeBuildType.RELEASE)) {
                        optimized = false // This is not allowed for RELEASE build type
                    }
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.IncompatibleBinaryConfiguration)
    }

    @Test
    fun `test DEBUG binary without debuggable`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64 {
                    binaries.staticLib(listOf(NativeBuildType.DEBUG)) {
                        debuggable = false // This is not allowed for DEBUG build type
                    }
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.IncompatibleBinaryConfiguration)
    }

    @Test
    fun `test RELEASE binary with debuggable`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64 {
                    binaries.executable(listOf(NativeBuildType.RELEASE)) {
                        debuggable = true // This is not allowed for RELEASE build type
                    }
                }
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.IncompatibleBinaryConfiguration)
    }

    @Test
    fun `test RELEASE binary`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64 {
                    // Not debuggable and optimized is allowed for RELEASE build type
                    binaries.executable(listOf(NativeBuildType.RELEASE))
                }
            }
        }

        project.evaluate()
        project.assertNoDiagnostics(KotlinToolingDiagnostics.IncompatibleBinaryConfiguration)
    }

    @Test
    fun `test DEBUG binary`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64 {
                    // Debuggable and not optimized is allowed for DEBUG build type
                    binaries.executable(listOf(NativeBuildType.DEBUG))
                }
            }
        }

        project.evaluate()
        project.assertNoDiagnostics(KotlinToolingDiagnostics.IncompatibleBinaryConfiguration)
    }
}