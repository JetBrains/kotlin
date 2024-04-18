/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.*

class PublishJvmEnvironmentAttributeTest :
    BaseNamedAttributeTest<TargetJvmEnvironment>(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, TargetJvmEnvironment::class.java) {
    @Test
    fun `test - default value`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()
        configurationResult.await()
        assertTrue(kotlinPropertiesProvider.publishJvmEnvironmentAttribute)
        assertAttributeEquals(kotlin.jvm(), STANDARD_JVM)
    }

    @Test
    fun `test - publishJvmEnvironmentAttribute disabled`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()

        project.propertiesExtension.set(KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE, "false")

        configurationResult.await()
        assertFalse(kotlinPropertiesProvider.publishJvmEnvironmentAttribute)
        assertNoAttribute(kotlin.jvm())
    }

    @Test
    fun `test - publishJvmEnvironmentAttribute enabled`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()

        project.propertiesExtension.set(KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE, "true")

        configurationResult.await()
        assertTrue(kotlinPropertiesProvider.publishJvmEnvironmentAttribute)
        assertAttributeEquals(kotlin.jvm(), STANDARD_JVM)
    }
}
