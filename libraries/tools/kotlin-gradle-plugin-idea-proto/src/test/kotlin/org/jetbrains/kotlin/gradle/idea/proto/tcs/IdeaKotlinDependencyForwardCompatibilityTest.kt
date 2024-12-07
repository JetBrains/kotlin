/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.classLoaderForBackwardsCompatibleClasses
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinDependencySerializer
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import org.junit.Test
import kotlin.reflect.KProperty0
import kotlin.test.assertEquals
import kotlin.test.fail

class IdeaKotlinDependencyForwardCompatibilityTest {

    @Test
    fun `test - simple unresolved binary dependency`() {
        val binary = oldBinaryOf(TestIdeaKotlinInstances::simpleUnresolvedBinaryDependency)
        val deserialized = deserializeOrFail<IdeaKotlinUnresolvedBinaryDependency>(binary)

        assertEquals(TestIdeaKotlinInstances.simpleUnresolvedBinaryDependency.cause, deserialized.cause)
        assertEquals(TestIdeaKotlinInstances.simpleUnresolvedBinaryDependency.coordinates, deserialized.coordinates)
        assertEquals(TestIdeaKotlinInstances.simpleUnresolvedBinaryDependency.extras, deserialized.extras)
    }

    @Test
    fun `test - simple resolved binary dependency`() {
        val binary = oldBinaryOf(TestIdeaKotlinInstances::simpleResolvedBinaryDependency)
        val deserialized = deserializeOrFail<IdeaKotlinResolvedBinaryDependency>(binary)

        assertEquals(TestIdeaKotlinInstances.simpleResolvedBinaryDependency.coordinates, deserialized.coordinates)
        assertEquals(TestIdeaKotlinInstances.simpleResolvedBinaryDependency.binaryType, deserialized.binaryType)
        assertEquals(TestIdeaKotlinInstances.simpleResolvedBinaryDependency.classpath, deserialized.classpath)
        assertEquals(TestIdeaKotlinInstances.simpleResolvedBinaryDependency.extras, deserialized.extras)
    }

    @Test
    fun `test - simple source dependency`() {
        val binary = oldBinaryOf(TestIdeaKotlinInstances::simpleSourceDependency)
        val deserialized = deserializeOrFail<IdeaKotlinSourceDependency>(binary)

        assertEquals(TestIdeaKotlinInstances.simpleSourceDependency.type, deserialized.type)
        assertEquals(TestIdeaKotlinInstances.simpleSourceDependency.coordinates, deserialized.coordinates)
        assertEquals(TestIdeaKotlinInstances.simpleSourceDependency.extras, deserialized.extras)
    }

    @Test
    fun `test - simple project artifact dependency`() {
        val binary = oldBinaryOf(TestIdeaKotlinInstances::simpleProjectArtifactDependency)
        val deserialized = deserializeOrFail<IdeaKotlinProjectArtifactDependency>(binary)

        assertEquals(TestIdeaKotlinInstances.simpleProjectArtifactDependency.type, deserialized.type)
        assertEquals(TestIdeaKotlinInstances.simpleProjectArtifactDependency.coordinates, deserialized.coordinates)
        assertEquals(TestIdeaKotlinInstances.simpleProjectArtifactDependency.extras, deserialized.extras)
    }
}

private inline fun <reified T : IdeaKotlinDependency> deserializeOrFail(data: ByteArray): T {
    val context = TestIdeaKotlinSerializationContext()
    val deserialized = context.IdeaKotlinDependency(data) ?: fail(
        "Failed to deserialize ${T::class.java.name}. Reports:\n" + context.logger.reports.joinToString("\n")
    )

    return deserialized as T
}

private fun oldBinaryOf(property: KProperty0<IdeaKotlinDependency>): ByteArray {
    val classLoader = classLoaderForBackwardsCompatibleClasses()
    val testIdeaKotlinInstancesClazz = classLoader.loadClass(TestIdeaKotlinInstances::class.java.name).kotlin

    val testIdeaKotlinInstances = testIdeaKotlinInstancesClazz.objectInstance
        ?: error("Failed to get ${TestIdeaKotlinInstances::class.java.name} instance")

    val member = testIdeaKotlinInstancesClazz.members
        .firstOrNull { it.name == property.name }
        ?: error("Failed to get '${property.name}' member")

    val dependencyInstance = member.call(testIdeaKotlinInstances)
        ?: error("Failed to get '${property.name}'")

    return TestIdeaKotlinDependencySerializer(classLoader).serialize(dependencyInstance)
}
