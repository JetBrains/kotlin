/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.kpm

import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmProject
import org.jetbrains.kotlin.gradle.unitTests.kpm.AbstractKpmExtensionTest
import org.jetbrains.kotlin.gradle.unitTests.kpm.buildIdeaKpmProjectModel
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.deserialize
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.serialize
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmMacosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeaKpmProjectModelSerializableTest : AbstractKpmExtensionTest() {

    @Test
    fun `test - serialize and deserialize - empty project`() {
        project.evaluate()
        project.repositories.mavenLocal()
        assertSerializeAndDeserializeEquals(kotlin.buildIdeaKpmProjectModel())
    }

    @Test
    fun `test - serialize and deserialize - project with variants and fragments`() {
        project.evaluate()
        project.repositories.mavenLocal()
        kotlin.mainAndTest {
            val native = fragments.create("native")
            val apple = fragments.create("apple")
            val ios = fragments.create<GradleKpmIosX64Variant>("ios")
            val macos = fragments.create<GradleKpmMacosX64Variant>("macos")
            val linux = fragments.create<GradleKpmLinuxX64Variant>("linux")
            val jvm = jvm

            apple.refines(native)
            ios.refines(apple)
            macos.refines(apple)
            linux.refines(native)
            jvm.refines(common)
        }

        project.evaluate()
        assertSerializeAndDeserializeEquals(kotlin.buildIdeaKpmProjectModel())
    }

    private fun assertSerializeAndDeserializeEquals(model: IdeaKpmProject) {
        val deserializedModel = model.serialize().deserialize<IdeaKpmProject>()

        assertEquals(
            model.toString(), deserializedModel.toString(),
            "Expected deserializedModel string representation to match source model"
        )
    }
}
