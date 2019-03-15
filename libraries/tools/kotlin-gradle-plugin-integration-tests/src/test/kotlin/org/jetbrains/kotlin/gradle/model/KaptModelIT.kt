/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KaptModelIT : BaseGradleIT() {
    @Test
    fun testKaptSimple() {
        val project = Project("simple", directoryPrefix = "kapt2")
        val kaptModel = project.getModels(Kapt::class.java).getModel(":")!!

        kaptModel.assertBasics("simple")

        assertEquals(2, kaptModel.kaptSourceSets.size)
        val mainSourceSet = kaptModel.kaptSourceSets.find { it.name == "main" }!!
        val testSourceSet = kaptModel.kaptSourceSets.find { it.name == "test" }!!

        assertEquals(KaptSourceSet.KaptSourceSetType.PRODUCTION, mainSourceSet.type)
        assertEquals(project.projectDir.resolve("build/generated/source/kapt/main"), mainSourceSet.generatedSourcesDirectory)
        assertEquals(project.projectDir.resolve("build/generated/source/kaptKotlin/main"), mainSourceSet.generatedKotlinSourcesDirectory)
        assertEquals(project.projectDir.resolve("build/tmp/kapt3/classes/main"), mainSourceSet.generatedClassesDirectory)

        assertEquals(KaptSourceSet.KaptSourceSetType.TEST, testSourceSet.type)
        assertEquals(project.projectDir.resolve("build/generated/source/kapt/test"), testSourceSet.generatedSourcesDirectory)
        assertEquals(project.projectDir.resolve("build/generated/source/kaptKotlin/test"), testSourceSet.generatedKotlinSourcesDirectory)
        assertEquals(project.projectDir.resolve("build/tmp/kapt3/classes/test"), testSourceSet.generatedClassesDirectory)
    }

    @Test
    fun testNonJvmProjects() {
        val project = Project("kotlin2JsProject")
        val models = project.getModels(Kapt::class.java)

        assertNull(models.getModel(":"))
        assertNull(models.getModel(":libraryProject"))
        assertNull(models.getModel(":mainProject"))
    }

    companion object {

        private fun Kapt.assertBasics(expectedName: String) {
            assertEquals(1L, modelVersion)
            assertEquals(expectedName, name)
        }
    }
}