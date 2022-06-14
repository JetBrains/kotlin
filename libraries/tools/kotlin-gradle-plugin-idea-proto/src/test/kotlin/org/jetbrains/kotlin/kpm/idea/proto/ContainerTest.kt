/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmInstances
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmSerializationLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContainerTest : IdeaKpmSerializationContext {
    override val logger = TestIdeaKpmSerializationLogger()
    override val extrasSerializationExtension = TestIdeaKpmExtrasSerializationExtension

    @Test
    fun `deserialize - with error future message - returns null`() {
        val data = ideaKpmContainerProto {
            schemaVersionMajor = IdeaKpmProtoSchema.versionMajor + 1
            schemaVersionMinor = IdeaKpmProtoSchema.versionMinor
            schemaVersionPatch = IdeaKpmProtoSchema.versionPatch
            schemaInfos.add(ideaKpmSchemaInfoProto {
                sinceSchemaVersionMajor = IdeaKpmProtoSchema.versionMajor + 1
                sinceSchemaVersionMinor = 0
                sinceSchemaVersionPatch = 0
                message = "Incompatible for this test!"
                severity = IdeaKpmSchemaInfoProto.Severity.ERROR
            })
        }.toByteArray()

        assertTrue(logger.reports.isEmpty(), "Expected no reports in logger")
        assertNull(IdeaKpmProject(data))
        assertTrue(logger.reports.isNotEmpty(), "Expected at least one report in logger")
    }

    @Test
    fun `deserialize - with lower major version - returns object`() {
        val data = ideaKpmContainerProto {
            schemaVersionMajor = IdeaKpmProtoSchema.versionMajor - 1
            schemaVersionMinor = IdeaKpmProtoSchema.versionMinor
            schemaVersionPatch = IdeaKpmProtoSchema.versionPatch
            project = IdeaKpmProjectProto(TestIdeaKpmInstances.simpleProject)
        }.toByteArray()

        assertEquals(TestIdeaKpmInstances.simpleProject, IdeaKpmProject(data))
    }
}
