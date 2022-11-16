/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaSerializationContext
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import org.junit.Test
import java.io.Serializable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdeaJavaIoSerializableExtrasSerializerTest {

    @Test
    fun `serialize - deserialize - String`() {
        val context = TestIdeaSerializationContext()
        val serializer = IdeaExtrasSerializer.javaIoSerializable<String>()
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

        val context = TestIdeaSerializationContext()
        val serializer = IdeaExtrasSerializer.javaIoSerializable<ThisIsNotSerializable>()
        assertNull(serializer.serialize(context, ThisIsNotSerializable()), "Expected null return value for non serializable object")

        val serializationFailureReportsCount = context.logger.reports.count { report ->
            report.message.startsWith(IdeaJavaIoSerializableExtrasSerializer.ErrorMessages.SERIALIZATION_FAILURE)
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

        val context = TestIdeaSerializationContext()
        val serializer = IdeaExtrasSerializer.javaIoSerializable<NotDeserializable>()
        val binary = assertNotNull(serializer.serialize(context, NotDeserializable()))
        assertNull(serializer.deserialize(context, binary))

        assertEquals(
            1, context.logger.reports.size,
            "Expected exactly one report. Found ${context.logger.reports}"
        )

        val report = context.logger.reports.single()
        val reportMessage = assertNotNull(report.message, "Expected message in issue report")

        assertTrue(
            reportMessage.startsWith(IdeaJavaIoSerializableExtrasSerializer.ErrorMessages.DESERIALIZATION_FAILURE),
            "Expected issue report to start with ${IdeaJavaIoSerializableExtrasSerializer.ErrorMessages.DESERIALIZATION_FAILURE}"
        )


        val causes = report.cause?.withLinearClosure { it.cause }.orEmpty()

        assertTrue(
            exception in causes,
            "Expected exception in report. Exceptions: $causes"
        )
    }
}
