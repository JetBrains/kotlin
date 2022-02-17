/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import buildIdeaKotlinProjectModel
import createKpmProject
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class DeepCopyTest {
    @Test
    fun `test - deep copy for model build from project`() {
        val (project, kotlin) = createKpmProject()

        kotlin.mainAndTest {
            jvm
            fragments.create("linux", KotlinLinuxX64Variant::class.java)
        }

        val model = project.buildIdeaKotlinProjectModel()
        val copy = model.deepCopy()
        assertNotSame(model, copy)
        assertEquals(model, copy)
    }
}
