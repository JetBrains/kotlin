/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmJavaIoSerializableExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmSerializationContext
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import org.junit.Test
import java.io.Serializable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdeaKpmJavaIoSerializableExtrasSerializerTest {

    @Test
    fun `serialize - deserialize - String`() {
        val context = TestIdeaKpmSerializationContext()
        val serializer = IdeaKpmExtrasSerializer.javaIoSerializable<String>()
        val binary = assertNotNull(
            serializer.serialize(context, "Sunny Cash"),
            "Failed to serialize: ${context.logger.reports}"
        )


        assertEquals("Sunny Cash", serializer.deserialize(context, binary))
        assertEquals(0, context.logger.reports.size, "Expected no reports")
    }

    @Test
    fun `serialize - deserialize - non serializable object`() {
        class ThisIsNotSerializable

        val context = TestIdeaKpmSerializationContext()
        val serializer = IdeaKpmExtrasSerializer.javaIoSerializable<ThisIsNotSerializable>()
        assertNull(serializer.serialize(context, ThisIsNotSerializable()), "Expected null return value for non serializable object")

        val serializationFailureReportsCount = context.logger.reports.count { report ->
            report.message.startsWith(IdeaKpmJavaIoSerializableExtrasSerializer.ErrorMessages.SERIALIZATION_FAILURE)
        }

        assertEquals(
            1, serializationFailureReportsCount,
            "Expected exactly one serialization failure report. Found reports: ${context.logger.reports}"
        )
    }

    @Test
    fun `serialize - deserialize - exception during deserialization`() {
        data class TestException(override val message: String) : Exception(message)

        val exception = TestException("Failed in 'readResolve()'")

        class NotDeserializable : Serializable {
            fun readResolve(): Any = throw exception
        }

        val context = TestIdeaKpmSerializationContext()
        val serializer = IdeaKpmExtrasSerializer.javaIoSerializable<NotDeserializable>()
        val binary = assertNotNull(serializer.serialize(context, NotDeserializable()))
        assertNull(serializer.deserialize(context, binary))

        assertEquals(
            1, context.logger.reports.size,
            "Expected exactly one report. Found ${context.logger.reports}"
        )

        val report = context.logger.reports.single()
        val reportMessage = assertNotNull(report.message, "Expected message in issue report")

        assertTrue(
            reportMessage.startsWith(IdeaKpmJavaIoSerializableExtrasSerializer.ErrorMessages.DESERIALIZATION_FAILURE),
            "Expected issue report to start with ${IdeaKpmJavaIoSerializableExtrasSerializer.ErrorMessages.DESERIALIZATION_FAILURE}"
        )


        val causes = report.cause?.withLinearClosure { it.cause }.orEmpty()

        assertTrue(
            exception in causes,
            "Expected exception in report. Exceptions: $causes"
        )
    }
}
