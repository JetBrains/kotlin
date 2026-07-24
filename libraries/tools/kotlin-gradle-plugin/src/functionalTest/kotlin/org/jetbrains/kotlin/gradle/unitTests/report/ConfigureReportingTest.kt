/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.report

import org.gradle.api.internal.plugins.PluginApplicationException
import org.jetbrains.kotlin.gradle.report.reportingSettings
import org.jetbrains.kotlin.gradle.util.applyKotlinJvmPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.util.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigureReportingTest {

    @Test
    fun validateMandatoryJsonDirectory() {
        val project = buildProject {
            propertiesExtension.set("kotlin.build.report.output", "json")
            applyKotlinJvmPlugin()
        }

        val reportSettings = reportingSettings(project)
        assertNotNull(reportSettings.jsonOutputDir)
    }

    @Test
    fun validateInvalidJsonDirectory() {
        val exception =
            assertThrows<PluginApplicationException>("The property 'kotlin.build.report.json.directory' should not be empty. Validation should fail.") {
                buildProject {
                    propertiesExtension.set("kotlin.build.report.output", "json")
                    propertiesExtension.set("kotlin.build.report.json.directory", "")
                    applyKotlinJvmPlugin()
                }
            }

        assertEquals(
            "The property 'kotlin.build.report.json.directory' must not be empty. Please provide a valid value.",
            exception.cause?.message
        )
    }
}
