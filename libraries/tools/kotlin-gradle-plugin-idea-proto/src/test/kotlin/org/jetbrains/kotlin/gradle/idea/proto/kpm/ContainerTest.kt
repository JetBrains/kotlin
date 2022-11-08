/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.IdeaKpmSchemaInfoProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmContainerProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmSchemaInfoProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import org.jetbrains.kotlin.gradle.idea.testFixtures.kpm.TestIdeaKpmInstances
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaSerializationLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContainerTest : IdeaSerializationContext {
    override val logger = TestIdeaSerializationLogger()
    override val extrasSerializationExtension = TestIdeaExtrasSerializationExtension

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
