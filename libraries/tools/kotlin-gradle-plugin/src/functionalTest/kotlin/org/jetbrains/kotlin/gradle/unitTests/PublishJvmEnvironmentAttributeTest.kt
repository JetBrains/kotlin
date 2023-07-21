/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinVariantWithMetadataVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.containsMultiplatformAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.*

class PublishJvmEnvironmentAttributeTest {
    @Test
    fun `test - default value`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()
        configurationResult.await()
        assertFalse(kotlinPropertiesProvider.publishJvmEnvironmentAttribute)
        assertNoJvmEnvironmentAttribute(kotlin.jvm())
    }

    @Test
    fun `test - publishJvmEnvironmentAttribute disabled`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()

        project.propertiesExtension.set(KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE, "false")

        configurationResult.await()
        assertFalse(kotlinPropertiesProvider.publishJvmEnvironmentAttribute)
        assertNoJvmEnvironmentAttribute(kotlin.jvm())
    }

    @Test
    fun `test - publishJvmEnvironmentAttribute enabled`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()

        project.propertiesExtension.set(KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE, "true")

        configurationResult.await()
        assertTrue(kotlinPropertiesProvider.publishJvmEnvironmentAttribute)
        assertJvmEnvironmentAttributeEquals(kotlin.jvm(), project.objects.named<TargetJvmEnvironment>(STANDARD_JVM))
    }

    private fun assertNoJvmEnvironmentAttribute(target: KotlinTarget) {
        target.forEachUsage { usage ->
            usage.attributes.containsMultiplatformAttributes
            assertNull(
                usage.attributes.getAttribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE),
                "Expected no jvm environment attribute to be set on usage '$usage"
            )
        }
    }

    private fun assertJvmEnvironmentAttributeEquals(target: KotlinTarget, value: TargetJvmEnvironment) {
        target.forEachUsage { usage ->
            usage.attributes.containsMultiplatformAttributes
            assertEquals(
                value, usage.attributes.getAttribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE),
            )
        }
    }

    private fun KotlinTarget.forEachUsage(action: (usage: KotlinUsageContext) -> Unit) {
        val component = internal.kotlinComponents.singleOrNull() ?: fail("Expected a single component. Found: ${components}")
        component as KotlinVariantWithMetadataVariant
        component.usages.ifEmpty { fail("Expected at least one 'usage'") }
        component.usages.forEach(action)
    }
}
