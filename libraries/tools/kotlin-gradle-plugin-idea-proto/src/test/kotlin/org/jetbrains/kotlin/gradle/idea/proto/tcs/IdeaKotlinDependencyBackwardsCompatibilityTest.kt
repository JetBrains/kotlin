/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.classLoaderForBackwardsCompatibleClasses
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationLogger
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinDependencySerializer
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.copy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class IdeaKotlinDependencyBackwardsCompatibilityTest {

    @Test
    fun `test - simple unresolved binary dependency`() {
        val dependency = TestIdeaKotlinInstances.simpleUnresolvedBinaryDependency
        val binary = TestIdeaKotlinDependencySerializer().serialize(dependency)
        val deserialized = deserializeIdeaKotlinDependencyWithBackwardsCompatibleClasses(binary)
        val deserializedCopied = deserialized.copy<IdeaKotlinUnresolvedBinaryDependency>()

        assertEquals(dependency.cause, deserializedCopied.cause)
        assertEquals(dependency.coordinates, deserializedCopied.coordinates)
        assertEquals(dependency.extras, deserializedCopied.extras)
    }

    @Test
    fun `test - simple resolved binary dependency`() {
        val dependency = TestIdeaKotlinInstances.simpleResolvedBinaryDependency
        val binary = TestIdeaKotlinDependencySerializer().serialize(dependency)
        val deserialized = deserializeIdeaKotlinDependencyWithBackwardsCompatibleClasses(binary)
        val deserializedCopied = deserialized.copy<IdeaKotlinResolvedBinaryDependency>()

        assertEquals(dependency.coordinates, deserializedCopied.coordinates)
        assertEquals(dependency.binaryType, deserializedCopied.binaryType)
        assertEquals(dependency.binaryFile, deserializedCopied.binaryFile)
        assertEquals(dependency.extras, deserializedCopied.extras)
    }

    @Test
    fun `test - simple source dependency`() {
        val dependency = TestIdeaKotlinInstances.simpleSourceDependency
        val binary = TestIdeaKotlinDependencySerializer().serialize(dependency)
        val deserialized = deserializeIdeaKotlinDependencyWithBackwardsCompatibleClasses(binary)
        val deserializedCopied = deserialized.copy<IdeaKotlinSourceDependency>()

        assertEquals(dependency.type, deserializedCopied.type)
        assertEquals(dependency.coordinates, deserializedCopied.coordinates)
        assertEquals(dependency.extras, deserializedCopied.extras)
    }
}

private fun deserializeIdeaKotlinDependencyWithBackwardsCompatibleClasses(project: ByteArray): Any {
    val classLoader = classLoaderForBackwardsCompatibleClasses()
    val serializer = TestIdeaKotlinDependencySerializer(classLoader)

    val deserialized = assertNotNull(
        serializer.deserialize(project),
        "Failed to deserialize dependency: ${serializer.reports}"
    )

    assertEquals(
        0, serializer.reports.count { it.severity > IdeaKotlinSerializationLogger.Severity.WARNING },
        "Expected no severe deserialization reports. Found ${serializer.reports}"
    )

    assertSame(
        classLoader, deserialized::class.java.classLoader,
        "Expected model do be deserialized in with old classes"
    )

    return deserialized
}
