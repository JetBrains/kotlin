/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import createProxyInstance
import deserialize
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelKey
import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelSerializer.Companion.serializable
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.external
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import org.junit.Test
import serialize
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

    @Test
    fun `test - simple project`() {
        val project = ProjectBuilder.builder().build() as ProjectInternal
        project.plugins.apply(KotlinPm20PluginWrapper::class.java)

        /* Setup example project */
        val kotlinExtension = project.extensions.getByType(KotlinPm20ProjectExtension::class.java)
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

        val model = buildModel(project)
        val deserializedModel = deserializeModelWithBackwardsCompatibleClasses(model)

        /* Use proxy instances to assert the deserialized model */
        run {
            val deserializedModelProxy = createProxyInstance<IdeaKotlinProjectModel>(deserializedModel)

            val deserializedMainModuleProxy = deserializedModelProxy.modules.firstOrNull { it.moduleIdentifier.moduleClassifier == null }
                ?: fail("Missing main module")

            val deserializedTestModuleProxy = deserializedModelProxy.modules.firstOrNull { it.moduleIdentifier.moduleClassifier == "test" }
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

    @OptIn(ExternalVariantApi::class)
    @Test
    fun `test - attaching serializable models`() {
        data class RetainedModel(val id: Int) : Serializable
        data class UnretainedModel(val id: Int)

        val retainedModelKey = KotlinExternalModelKey<RetainedModel>(serializable())
        val unretainedModelKey = KotlinExternalModelKey<UnretainedModel>()

        val project = ProjectBuilder.builder().build() as ProjectInternal
        project.plugins.apply(KotlinPm20PluginWrapper::class.java)

        /* Setup example project */
        val kotlinExtension = project.extensions.getByType(KotlinPm20ProjectExtension::class.java)
        kotlinExtension.main.common.external[retainedModelKey] = RetainedModel(2411)
        kotlinExtension.main.common.external[unretainedModelKey] = UnretainedModel(510)

        val model = buildModel(project)
        val deserializedModel = deserializeModelWithBackwardsCompatibleClasses(model)
        val deserializedModelProxy = createProxyInstance<IdeaKotlinProjectModel>(deserializedModel)

        val deserializedMainModuleProxy = deserializedModelProxy.modules.find { it.moduleIdentifier.moduleClassifier == null }
            ?: fail("Missing main module")

        val deserializedCommonFragmentProxy = deserializedMainModuleProxy.fragments.find { it.name == "common" }
            ?: fail("Missing common fragment")

        run {
            val deserializedCommonFragment = unwrapProxyInstance(deserializedCommonFragmentProxy)
            val external = deserializedCommonFragment.serialize().deserialize<IdeaKotlinFragment>().external
            assertEquals(1, external.ids.size)
            assertEquals(RetainedModel(2411), external[retainedModelKey])
            assertNull(external[unretainedModelKey])
        }
    }
}

private fun getClassLoaderForBackwardsCompatibilityTest(): ClassLoader {
    val uris = getClasspathForBackwardsCompatibilityTest().map { file -> file.toURI().toURL() }.toTypedArray()
    return URLClassLoader.newInstance(uris, null)
}

private fun getClasspathForBackwardsCompatibilityTest(): List<File> {
    val backwardsCompatibilityClasspathString = System.getProperty("backwardsCompatibilityClasspath")
        ?: error("Missing backwardsCompatibilityClasspath system property")

    return backwardsCompatibilityClasspathString.split(";").map { path -> File(path) }
        .onEach { file -> if (!file.exists()) println("[WARNING] Missing $file") }
}

private fun buildModel(project: Project): IdeaKotlinProjectModel {
    return project.serviceOf<ToolingModelBuilderRegistry>().getBuilder(IdeaKotlinProjectModel::class.java.name)
        .buildAll(IdeaKotlinProjectModel::class.java.name, project) as IdeaKotlinProjectModel
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
