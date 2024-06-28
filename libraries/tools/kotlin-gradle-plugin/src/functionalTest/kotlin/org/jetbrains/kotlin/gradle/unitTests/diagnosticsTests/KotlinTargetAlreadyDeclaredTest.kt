/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import com.android.build.gradle.LibraryPlugin
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinTargetAlreadyDeclared
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

class KotlinTargetAlreadyDeclaredTest {

    @Test
    fun `no diagnostic reported when only single targets declared`() {
        val project = buildProjectWithMPP {
            plugins.apply(LibraryPlugin::class.java)
            androidLibrary { compileSdk = 33 }
            kotlin {
                jvm()
                js()
                androidTarget()
                linuxX64()
                linuxArm64()
            }
        }

        project.evaluate()
        project.assertNoDiagnostics(KotlinTargetAlreadyDeclared)
    }

    @Test
    fun `no diagnostic reported when custom name is used`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm("jvm2")
                linuxX64("linux")
            }
        }

        project.evaluate()
        project.assertNoDiagnostics(KotlinTargetAlreadyDeclared)
    }

    @Test
    fun `no diagnostic reported when target declaration dsl is used to get existing target`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm() // declare
                jvm() // get target declared above
            }
        }

        project.evaluate()
        project.assertNoDiagnostics(KotlinTargetAlreadyDeclared)
    }

    @Test
    fun `diagnostic reported when jvm target declared twice with different names`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                jvm("jvm2")
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinTargetAlreadyDeclared("jvm"))
    }

    @Test
    fun `diagnostic reported when native target declared twice with different names`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxArm64()
                linuxArm64("linux")
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinTargetAlreadyDeclared("linuxArm64"))
    }

    @Test
    fun `diagnostic reported when android target declared twice with different names`() {
        val project = buildProjectWithMPP {
            plugins.apply(LibraryPlugin::class.java)
            androidLibrary { compileSdk = 33 }
            kotlin {
                androidTarget()
                androidTarget("android2")
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinTargetAlreadyDeclared("androidTarget"))
    }

    @Test
    fun `diagnostic reported when js target declared twice with different names`() {
        val project = buildProjectWithMPP {
            kotlin {
                js("browser")
                js("nodejs")
            }
        }

        project.evaluate()
        project.assertContainsDiagnostic(KotlinTargetAlreadyDeclared("js"))
    }
}