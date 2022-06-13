/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.*
import org.jetbrains.kotlin.gradle.kpm.idea.testUtils.deserializeIdeaKpmProjectWithBackwardsCompatibleClasses
import org.junit.Test
import kotlin.test.assertEquals

class IdeaKpmProjectBackwardsCompatibilityDeserializationTest {

    @Test
    fun `test - simpleInstance`() {
        val project = TestIdeaKpmInstances.simpleProject
        val deserialized = deserializeIdeaKpmProjectWithBackwardsCompatibleClasses(project)
        val deserializedProxy = createProxyInstance<IdeaKpmProject>(deserialized)

        assertEquals(project.coreLibrariesVersion, deserializedProxy.coreLibrariesVersion)
        assertEquals(project.modules.size, deserializedProxy.modules.size)
        project.modules.forEach { module ->
            val deserializedModule = deserializedProxy.assertContainsModule(module.name)
            assertEquals(module.coordinates, deserializedModule.coordinates.copy())

            module.fragments.forEach { fragment ->
                val deserializedFragment = deserializedModule.assertContainsFragment(fragment.name)
                assertEquals(fragment.coordinates, deserializedFragment.coordinates.copy())
            }
        }
    }
}
