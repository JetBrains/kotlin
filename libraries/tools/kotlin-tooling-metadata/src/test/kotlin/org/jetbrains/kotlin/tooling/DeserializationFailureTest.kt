/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


class DeserializationFailureTest {
    @Test
    fun sample1() {
        val result = KotlinToolingMetadata.parseJson("")
        assertTrue(
            result is KotlinToolingMetadataParsingResult.Failure,
            "Expected empty String to produce Failure. Actual: $result"
        )

        assertFailsWith<IllegalArgumentException> { KotlinToolingMetadata.parseJsonOrThrow("") }
    }

    @Test
    fun `sample2 missing buildPluginVersion`() {
        @Language("JSON") val json =
            """
            {
              "schemaVersion": ${SchemaVersion.current},
              "buildSystem": "Gradle",
              "buildSystemVersion": "6.7",
              "buildPlugin": "org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper",
              "projectSettings": {
                "isHmppEnabled": false,
                "isCompatibilityMetadataVariantEnabled": true
              },
              "projectTargets": []
            }
            """.trimIndent()

        val result = KotlinToolingMetadata.parseJson(json)
        assertTrue(
            result is KotlinToolingMetadataParsingResult.Failure,
            "Expected parsing failure, because of missing pluginVersion. Actual: $result"
        )

        val exception = assertFailsWith<IllegalArgumentException> { KotlinToolingMetadata.parseJsonOrThrow(json) }
        assertTrue(
            "buildPluginVersion" in exception.message.orEmpty(),
            "Expected 'buildPluginVersion' mentioned in error message\n${exception.message}"
        )
    }

    @Test
    fun `sample3 incompatible schemaVersion`() {
        val majorUpgradeVersion = SchemaVersion.current.copy(major = SchemaVersion.current.major + 1)
        val metadataString = KotlinToolingMetadata(
            schemaVersion = majorUpgradeVersion.toString(),
            buildSystem = "",
            buildSystemVersion = "",
            buildPlugin = "",
            buildPluginVersion = "",
            projectSettings = KotlinToolingMetadata.ProjectSettings(false, false, false),
            projectTargets = emptyList()
        ).toJsonString()

        val exception = assertFailsWith<IllegalArgumentException> { KotlinToolingMetadata.parseJsonOrThrow(metadataString) }
        assertTrue(
            majorUpgradeVersion.toString() in exception.message.orEmpty(),
            "Expected bad schemaVersion mentioned in error message\n${exception.message}"
        )
    }
}
