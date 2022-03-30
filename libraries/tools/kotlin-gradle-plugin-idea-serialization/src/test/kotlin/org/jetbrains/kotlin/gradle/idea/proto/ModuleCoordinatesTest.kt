/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinModuleCoordinatesImpl
import org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.kpm.idea.proto.IdeaKotlinModuleCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.proto.serialize
import kotlin.test.Test
import kotlin.test.assertEquals

class ModuleCoordinatesTest {
    @Test
    fun `test - sample 0`() {
        val coordinates = IdeaKotlinModuleCoordinatesImpl(
            "buildId", "someProjectPath", "someProjectName", "someModuleName", null
        )

        val deserialized = IdeaKotlinModuleCoordinates(coordinates.serialize())
        assertEquals(coordinates, deserialized)
    }
}
