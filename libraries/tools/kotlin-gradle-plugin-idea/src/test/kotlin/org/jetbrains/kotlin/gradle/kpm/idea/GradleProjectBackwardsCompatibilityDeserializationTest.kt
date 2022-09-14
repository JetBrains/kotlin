/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmExtra
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmExtrasSerializationExtension.anySerializableKey
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.copy
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.createProxyInstance
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.unwrapProxyInstance
import org.jetbrains.kotlin.gradle.kpm.idea.testUtils.buildIdeaKpmProject
import org.jetbrains.kotlin.gradle.kpm.idea.testUtils.createKpmProject
import org.jetbrains.kotlin.gradle.kpm.idea.testUtils.deserializeIdeaKpmProjectWithBackwardsCompatibleClasses
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * This test is designed to test if serialized models can still be deserialized with a older (minimal supported)
 * version of this module.
 *
 * The test will
 * - Build a representative 'sample' model
 * - Serialize this model
 * - Deserialize this model with the older version of the classes (using a custom class loader)
 * - Assert the deserialized model is healthy
 */
class GradleProjectBackwardsCompatibilityDeserializationTest {

    data class UnretainedModel(val id: Int)

    @Test
    fun `test - simple project`() {
        val (project, kotlinExtension) = createKpmProject()
        kotlinExtension.mainAndTest {
            jvm
            val native = fragments.create("native")
            val linux = fragments.create<GradleKpmLinuxX64Variant>("linuxX64")
            val ios = fragments.create<GradleKpmIosX64Variant>("iosX64")
            native.refines(common)
            linux.refines(native)
            ios.refines(native)
        }
        project.evaluate()

        val model = project.buildIdeaKpmProject()
        val deserializedModel = deserializeIdeaKpmProjectWithBackwardsCompatibleClasses(model)

        /* Use proxy instances to assert the deserialized model */
        run {
            val deserializedModelProxy = createProxyInstance<IdeaKpmProject>(deserializedModel)

            val deserializedMainModuleProxy = deserializedModelProxy.modules.firstOrNull { it.coordinates.moduleClassifier == null }
                ?: fail("Missing main module")

            val deserializedTestModuleProxy = deserializedModelProxy.modules.firstOrNull { it.coordinates.moduleClassifier == "test" }
                ?: fail("Missing test module")

            listOf(deserializedMainModuleProxy, deserializedTestModuleProxy).forEach { module ->
                assertEquals(
                    model.modules.flatMap { it.fragments }.map { it.name }.toSet(),
                    module.fragments.map { it.name }.toSet(),
                    "Expected all fragment names to be present"
                )
            }
        }
    }

    @Test
    fun `test - attaching serializable extras`() {
        val unretainedModelKey = extrasKeyOf<UnretainedModel>()

        val project = ProjectBuilder.builder().build() as ProjectInternal
        project.plugins.apply(KotlinPm20PluginWrapper::class.java)

        /* Setup example project */
        val kotlinExtension = project.extensions.getByType(KotlinPm20ProjectExtension::class.java)
        kotlinExtension.main.common.extras[anySerializableKey] = TestIdeaKpmExtra(2411)
        kotlinExtension.main.common.extras[unretainedModelKey] = UnretainedModel(510)

        val model = project.buildIdeaKpmProject()
        val deserializedModel = deserializeIdeaKpmProjectWithBackwardsCompatibleClasses(model)
        val deserializedModelProxy = createProxyInstance<IdeaKpmProject>(deserializedModel)

        val deserializedMainModuleProxy = deserializedModelProxy.modules.find { it.coordinates.moduleClassifier == null }
            ?: fail("Missing main module")

        val deserializedCommonFragmentProxy = deserializedMainModuleProxy.fragments.find { it.name == "common" }
            ?: fail("Missing common fragment")

        run {
            val deserializedCommonFragment = unwrapProxyInstance(deserializedCommonFragmentProxy)
            val extras = deserializedCommonFragment.copy<IdeaKpmFragment>().extras
            assertEquals(1, extras.keys.size)
            assertEquals(TestIdeaKpmExtra(2411), extras[anySerializableKey])
            assertNull(extras[unretainedModelKey])
        }
    }
}
