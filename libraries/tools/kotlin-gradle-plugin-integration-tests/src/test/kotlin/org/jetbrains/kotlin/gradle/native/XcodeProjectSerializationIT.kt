/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.*
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import org.jetbrains.kotlin.gradle.testing.XcodeProjectSerializationFixtures
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

@NativeGradlePluginTests
class XcodeProjectSerializationIT : KGPBaseTest() {

    @GradleTest
    fun `smoke test kotlinx serialization - works to deserialize a pbxproj json in different Gradle versions`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }

            val sampleXcodeProjectJson = XcodeProjectSerializationFixtures.sampleXcodeProjectWithEmbedAndSignIntegration
            val reserializedProjectString = buildScriptReturn {
                ByteArrayOutputStream().use {
                    deserializeXcodeProject(sampleXcodeProjectJson.toByteArray())
                        .serializeXcodeProject(it)
                    it.toString()
                }
            }.buildAndReturn()
            val reserializedProject = Json.decodeFromString<JsonElement>(reserializedProjectString)
            assertEquals(
                Json.decodeFromString<JsonElement>(sampleXcodeProjectJson),
                reserializedProject,
            )
        }
    }

}