/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class KT55730CommonMainDependsOnAnotherSourceSet {
    @Test
    fun `legacy metadata compilation should have commonMain with its depends on closure`() {
        val project = buildProject {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT, "true")
            propertiesExtension.set(
                PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_ERRORS,
                KotlinToolingDiagnostics.PreHMPPFlagsError.id
            )
            applyMultiplatformPlugin()
            kotlin {
                val grandCommonMain = sourceSets.create("grandCommonMain")
                val commonMain = sourceSets.getByName("commonMain")
                commonMain.dependsOn(grandCommonMain)
            }
        }

        project.evaluate()

        val actualSourceSets = project
            .multiplatformExtension
            .metadata()
            .compilations
            .getByName("main")
            .kotlinSourceSets
            .map { it.name }
            .toSet()

        assertEquals(setOf("grandCommonMain", "commonMain"), actualSourceSets)
    }
}