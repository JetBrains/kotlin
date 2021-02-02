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
    fun `sample2 missing build pluginVersion`() {
        @Language("JSON") val json =
            """
            {
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

        assertFailsWith<IllegalArgumentException> { KotlinToolingMetadata.parseJsonOrThrow(json) }
    }
}
