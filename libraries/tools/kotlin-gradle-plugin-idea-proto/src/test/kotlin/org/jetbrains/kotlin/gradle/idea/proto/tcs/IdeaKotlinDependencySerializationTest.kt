/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationLogger
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class IdeaKotlinDependencySerializationTest : AbstractSerializationTest<IdeaKotlinDependency>() {
    override fun serialize(value: IdeaKotlinDependency): ByteArray = value.toByteArray(this)

    override fun deserialize(data: ByteArray): IdeaKotlinDependency =
        IdeaKotlinDependency(data) ?: fail("Failed to deserialize ${IdeaKotlinDependency::class.java.name}")

    @Test
    fun `sample - unresolved binary dependency`() = testSerialization(
        TestIdeaKotlinInstances.simpleUnresolvedBinaryDependency
    )

    @Test
    fun `sample - resolved binary dependency`() = testSerialization(
        TestIdeaKotlinInstances.simpleResolvedBinaryDependency
    )

    @Test
    fun `sample - source dependency`() = testSerialization(
        TestIdeaKotlinInstances.simpleSourceDependency
    )

    @Test
    fun `sample - project artifact dependency`() = testSerialization(
        TestIdeaKotlinInstances.simpleProjectArtifactDependency
    )

    @Test
    fun `bad data - returns null`() {
        assertNull(IdeaKotlinDependency(byteArrayOf()))
        if (logger.reports.size != 1) fail("Expected exactly one report in logger. Found ${logger.reports}")
        val report = logger.reports.first()
        assertEquals(IdeaKotlinSerializationLogger.Severity.ERROR, report.severity)
        assertEquals("Dependency not set", report.message)
    }
}
