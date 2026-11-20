/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import NonKgpSourceSetsInAndroidExtensionAccessor
import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.dsl.kotlinAndroidExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.applyKotlinAndroidPlugin
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.defaultFilteredDiagnostics
import kotlin.test.Test
import kotlin.test.assertTrue

class SourceSetsAccessInAndroidExtensionTest {
    @Test
    fun `diagnostic is not reported when sourceSets is not accessed`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}
        }
        project.evaluate()
        project.assertNoDiagnostics(
            filterDiagnosticIds = defaultFilteredDiagnostics + KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin
        )
    }

    @Test
    fun `diagnostic is not reported when sourceSets is accessed inside`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            @Suppress("DEPRECATION_ERROR")
            kotlinAndroidExtension.sourceSets
        }
        project.evaluate()
        project.assertNoDiagnostics(
            filterDiagnosticIds = defaultFilteredDiagnostics + KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin
        )
    }

    @Test
    fun `diagnostic is reported when sourceSets is accessed outside`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            with(NonKgpSourceSetsInAndroidExtensionAccessor) {
                kotlinAndroidExtension.accessSourceSets()
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SourceSetsAccessInAndroidExtension)
    }

    @Test
    fun `diagnostic is not reported when sourceSets Kotlin block is accessed inside`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            @Suppress("DEPRECATION_ERROR")
            kotlinAndroidExtension.sourceSets {}
        }
        project.evaluate()
        project.assertNoDiagnostics(
            filterDiagnosticIds = defaultFilteredDiagnostics + KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin
        )
    }

    @Test
    fun `diagnostic is reported when sourceSets Kotlin block is accessed outside`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            with(NonKgpSourceSetsInAndroidExtensionAccessor) {
                kotlinAndroidExtension.accessSourceSetsKotlinBlock()
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SourceSetsAccessInAndroidExtension)
    }

    @Test
    fun `diagnostic is not reported when sourceSets Groovy block is accessed inside`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            @Suppress("DEPRECATION_ERROR")
            kotlinAndroidExtension.sourceSets(Action {})
        }
        project.evaluate()
        project.assertNoDiagnostics(
            filterDiagnosticIds = defaultFilteredDiagnostics + KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin
        )
    }

    @Test
    fun `diagnostic is reported when sourceSets Groovy block is accessed outside`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            with(NonKgpSourceSetsInAndroidExtensionAccessor) {
                kotlinAndroidExtension.accessSourceSetsGroovyBlock()
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SourceSetsAccessInAndroidExtension)
    }

    @Test
    fun `diagnostic is reported and closure delegates to source set when sourceSets block is used from Groovy script`() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {}

            val script = file("sourceSets.gradle")
            script.writeText(
                """
                kotlin {
                    sourceSets {
                        main {
                            languageSettings { optIn('groovy.dsl.Marker') }
                        }
                    }
                }
                """.trimIndent()
            )
            apply(mapOf("from" to script))
        }
        project.evaluate()

        @Suppress("DEPRECATION_ERROR")
        val main = project.kotlinAndroidExtension.sourceSets.getByName("main")
        assertTrue(
            "groovy.dsl.Marker" in main.languageSettings.optInAnnotationsInUse,
            "Groovy closure did not configure the 'main' source set"
        )
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SourceSetsAccessInAndroidExtension)
    }
}
