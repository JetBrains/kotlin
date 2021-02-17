/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KotlinAndroidExtensionIT : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(
            androidGradlePluginVersion = AGPVersion.v3_4_1,
            androidHome = KtTestUtil.findAndroidSdk()
        )
    }

    @Test
    fun testAndroidExtensionsProject() {
        val project = Project("AndroidExtensionsProject")
        val androidExtensionModel = project.getModels(KotlinAndroidExtension::class.java).getModel(":app")!!

        assertEquals(1L, androidExtensionModel.modelVersion)
        assertEquals("app", androidExtensionModel.name)
        assertFalse(androidExtensionModel.isExperimental)
        assertEquals("hashMap", androidExtensionModel.defaultCacheImplementation)
    }

    @Test
    fun testAndroidExtensionsManyVariants() {
        val project = Project("AndroidExtensionsManyVariants")
        val androidExtensionModel = project.getModels(KotlinAndroidExtension::class.java).getModel(":app")!!

        assertEquals(1L, androidExtensionModel.modelVersion)
        assertEquals("app", androidExtensionModel.name)
        assertTrue(androidExtensionModel.isExperimental)
        assertEquals("hashMap", androidExtensionModel.defaultCacheImplementation)
    }

    @Test
    fun testNonAndroidExtensionsProjects() {
        val project = Project("kotlinProject")
        val model = project.getModels(KotlinAndroidExtension::class.java).getModel(":")

        assertNull(model)
    }
}
