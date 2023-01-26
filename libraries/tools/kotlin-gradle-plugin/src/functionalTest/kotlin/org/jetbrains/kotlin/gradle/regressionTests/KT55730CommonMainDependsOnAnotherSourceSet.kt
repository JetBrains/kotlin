/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableCompatibilityMetadataVariant
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class KT55730CommonMainDependsOnAnotherSourceSet {
    @Test
    fun `legacy metadata compilation should have commonMain with its depends on closure`() {
        val project = buildProject {
            enableCompatibilityMetadataVariant()
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