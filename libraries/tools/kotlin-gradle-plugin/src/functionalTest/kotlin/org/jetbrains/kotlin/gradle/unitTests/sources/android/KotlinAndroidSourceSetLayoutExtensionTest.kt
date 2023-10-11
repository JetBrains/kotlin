/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.sources.android.kotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV1
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.gradle.plugin.sources.android.singleTargetAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.setMultiplatformAndroidSourceSetLayoutVersion
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KotlinAndroidSourceSetLayoutExtensionTest {

    @Test
    fun `single platform plugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(KotlinAndroidPluginWrapper::class.java)

        assertEquals(singleTargetAndroidSourceSetLayout, project.kotlinAndroidSourceSetLayout)

        project.setMultiplatformAndroidSourceSetLayoutVersion(1)
        assertEquals(singleTargetAndroidSourceSetLayout, project.kotlinAndroidSourceSetLayout)

        project.setMultiplatformAndroidSourceSetLayoutVersion(2)
        assertEquals(singleTargetAndroidSourceSetLayout, project.kotlinAndroidSourceSetLayout)
    }

    @Test
    fun `test multiplatform plugin`() {
        val project = buildProjectWithMPP()
        assertEquals(
            multiplatformAndroidSourceSetLayoutV2, project.kotlinAndroidSourceSetLayout,
            "Expected v2 being set as default"
        )

        project.setMultiplatformAndroidSourceSetLayoutVersion(2)
        assertEquals(multiplatformAndroidSourceSetLayoutV2, project.kotlinAndroidSourceSetLayout)

        project.setMultiplatformAndroidSourceSetLayoutVersion(1)
        assertEquals(multiplatformAndroidSourceSetLayoutV1, project.kotlinAndroidSourceSetLayout)

        /* Test unhappy path: Layout version 0 is unknown/unsupported */
        project.setMultiplatformAndroidSourceSetLayoutVersion(0)
        assertFailsWith<IllegalArgumentException> {
            project.kotlinAndroidSourceSetLayout
        }
    }
}
