/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import buildIdeaKotlinProjectModel
import createKpmProject
import createProxyInstance
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.deserialize
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.serialize
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import unwrapProxyInstance
import java.io.File
import java.io.Serializable
import java.net.URLClassLoader
import kotlin.test.*

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
class BackwardsCompatibilityDeserializationTest {

    data class RetainedModel(val id: Int) : Serializable
    data class UnretainedModel(val id: Int)

    @Test
    fun `test - simple project`() {
        val (project, kotlinExtension) = createKpmProject()
        kotlinExtension.mainAndTest {
            jvm
            val native = fragments.create("native")
            val linux = fragments.create<KotlinLinuxX64Variant>("linuxX64")
            val ios = fragments.create<KotlinIosX64Variant>("iosX64")
            native.refines(common)
            linux.refines(native)
            ios.refines(native)
        }
        project.evaluate()

        val model = project.buildIdeaKotlinProjectModel()
        val deserializedModel = deserializeModelWithBackwardsCompatibleClasses(model)

        /* Use proxy instances to assert the deserialized model */
        run {
            val deserializedModelProxy = createProxyInstance<IdeaKotlinProjectModel>(deserializedModel)

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
        val retainedModelKey = extrasKeyOf<RetainedModel>() + IdeaKotlinExtrasSerializer.serializable()
        val unretainedModelKey = extrasKeyOf<UnretainedModel>()

        val project = ProjectBuilder.builder().build() as ProjectInternal
        project.plugins.apply(KotlinPm20PluginWrapper::class.java)

        /* Setup example project */
        val kotlinExtension = project.extensions.getByType(KotlinPm20ProjectExtension::class.java)
        kotlinExtension.main.common.extras[retainedModelKey] = RetainedModel(2411)
        kotlinExtension.main.common.extras[unretainedModelKey] = UnretainedModel(510)

        val model = project.buildIdeaKotlinProjectModel()
        val deserializedModel = deserializeModelWithBackwardsCompatibleClasses(model)
        val deserializedModelProxy = createProxyInstance<IdeaKotlinProjectModel>(deserializedModel)

        val deserializedMainModuleProxy = deserializedModelProxy.modules.find { it.coordinates.moduleClassifier == null }
            ?: fail("Missing main module")

        val deserializedCommonFragmentProxy = deserializedMainModuleProxy.fragments.find { it.name == "common" }
            ?: fail("Missing common fragment")

        run {
            val deserializedCommonFragment = unwrapProxyInstance(deserializedCommonFragmentProxy)
            val extras = deserializedCommonFragment.serialize().deserialize<IdeaKotlinFragment>().extras
            assertEquals(1, extras.ids.size)
            assertEquals(RetainedModel(2411), extras[retainedModelKey])
            assertNull(extras[unretainedModelKey])
        }
    }
}

private fun getClassLoaderForBackwardsCompatibilityTest(): ClassLoader {
    val uris = getClasspathForBackwardsCompatibilityTest().map { file -> file.toURI().toURL() }.toTypedArray()
    return URLClassLoader.newInstance(uris, null)
}

private fun getClasspathForBackwardsCompatibilityTest(): List<File> {
    val compatibilityTestClasspath = System.getProperty("compatibilityTestClasspath")
        ?: error("Missing compatibilityTestClasspath system property")

    return compatibilityTestClasspath.split(";").map { path -> File(path) }
        .onEach { file -> if (!file.exists()) println("[WARNING] Missing $file") }
        .flatMap { file -> if(file.isDirectory) file.listFiles().orEmpty().toList() else listOf(file) }
}

private fun deserializeModelWithBackwardsCompatibleClasses(model: IdeaKotlinProjectModel): Any {
    val backwardsCompatibilityClassLoader = getClassLoaderForBackwardsCompatibilityTest()
    val backwardsCompatibilityModel = model.serialize().deserialize(backwardsCompatibilityClassLoader)

    assertSame(
        backwardsCompatibilityModel.javaClass.classLoader,
        backwardsCompatibilityClassLoader,
        "Expected deserialized model being loaded by 'backwardsCompatibilityTestClassLoader'"
    )

    assertNotEquals<Class<*>>(
        model.javaClass, backwardsCompatibilityModel.javaClass,
        "Expected deserialized model java class to be different from origin"
    )

    assertEquals(
        model.javaClass.name, backwardsCompatibilityModel.javaClass.name,
        "Expected deserialized model to be same java class as origin"
    )

    return backwardsCompatibilityModel
}
