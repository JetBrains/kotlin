/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.external.externalKotlinTargetApiUtils
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.*

class ExternalKotlinTargetApiUtilsTest {
    @Test
    fun `test - enableKgpBasedDependencyResolution`() {
        val project = buildProjectWithMPP()
        assertNull(project.kotlinPropertiesProvider.enableKgpDependencyResolution)

        project.externalKotlinTargetApiUtils.enableKgpBasedDependencyResolution(true)
        assertEquals(true, project.kotlinPropertiesProvider.enableKgpDependencyResolution)

        project.externalKotlinTargetApiUtils.enableKgpBasedDependencyResolution(false)
        assertEquals(false, project.kotlinPropertiesProvider.enableKgpDependencyResolution)

        project.externalKotlinTargetApiUtils.enableKgpBasedDependencyResolution()
        assertEquals(true, project.kotlinPropertiesProvider.enableKgpDependencyResolution)
    }

    @Test
    fun `test - publishJvmEnvironmentAttribute`() {
        val project = buildProjectWithMPP()
        project.externalKotlinTargetApiUtils.publishJvmEnvironmentAttribute(true)
        assertTrue(project.kotlinPropertiesProvider.publishJvmEnvironmentAttribute)

        project.externalKotlinTargetApiUtils.publishJvmEnvironmentAttribute(false)
        assertFalse(project.kotlinPropertiesProvider.publishJvmEnvironmentAttribute)

        project.externalKotlinTargetApiUtils.publishJvmEnvironmentAttribute()
        assertTrue(project.kotlinPropertiesProvider.publishJvmEnvironmentAttribute)
    }
}