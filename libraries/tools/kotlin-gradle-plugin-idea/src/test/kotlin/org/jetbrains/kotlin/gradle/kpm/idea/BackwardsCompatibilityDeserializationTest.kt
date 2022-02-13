/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import org.junit.Test
import java.io.*
import java.net.URLClassLoader
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

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

        testDeserializeCompatibility(buildModel(project))
    }

    private fun buildModel(project: Project): IdeaKotlinProjectModel {
        return project.serviceOf<ToolingModelBuilderRegistry>().getBuilder(IdeaKotlinProjectModel::class.java.name)
            .buildAll(IdeaKotlinProjectModel::class.java.name, project) as IdeaKotlinProjectModel
    }

    private fun testDeserializeCompatibility(model: IdeaKotlinProjectModel) {
        val serializedModel = ByteArrayOutputStream().run {
            ObjectOutputStream(this).use { stream -> stream.writeObject(model) }
            toByteArray()
        }

        val backwardsCompatibilityTestClassLoader = getClassLoaderForBackwardsCompatibilityTest()
        val backwardsCompatibilityObjectInputStream = object : ObjectInputStream(ByteArrayInputStream(serializedModel)) {
            override fun resolveClass(desc: ObjectStreamClass): Class<*> {
                return backwardsCompatibilityTestClassLoader.loadClass(desc.name)
            }
        }
        val backwardsCompatibilityModel = backwardsCompatibilityObjectInputStream.readObject()
        assertSame(
            backwardsCompatibilityModel.javaClass.classLoader,
            backwardsCompatibilityTestClassLoader,
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

