/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_DEPRECATED_TEST_PROPERTY
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GradleDeprecatedPropertyChecker {


    @Test
    fun `KT-83254 - diagnostic with filtering - emits only when filter passes the property`() {
        val propertiesWithDeprecatedFalseValue = listOf(
            "kotlin.mpp.import.enableKgpDependencyResolution" to "EnableKgpDependencyResolutionDeprecation",
            "kotlin.publishJvmEnvironmentAttribute" to "EnablePublishJvmEnvironmentAttributeDeprecation",
        )
        propertiesWithDeprecatedFalseValue.forEach {
            buildProjectWithMPP(
                preApplyCode = { project.propertiesExtension.set(it.first, false.toString()) },
            ) {
                kotlin { jvm() }
            }.evaluate().checkDiagnostics(it.second)
            buildProjectWithMPP(
                preApplyCode = { project.propertiesExtension.set(it.first, true.toString()) },
            ) {
                kotlin { jvm() }
            }.evaluate().assertNoDiagnostics()
        }
    }

    @Test
    fun `KT-83678 - checker doesn't null out properties set during build script evaluation`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
            }
        }
        val nonExistentProperty = "nonExistentProperty"
        val propertiesService = PropertiesBuildService.registerIfAbsent(project).get()
        assertNull(propertiesService.get(nonExistentProperty, project))
        // the value of the property is memoized by PropertiesBuildService and setting it has no effect
        project.propertiesExtension.set(nonExistentProperty, true)
        assertNull(propertiesService.get(nonExistentProperty, project))

        // check that the checker executes after build script evaluation and doesn't null out used properties if they are set during build script evaluation
        project.propertiesExtension.set(KOTLIN_DEPRECATED_TEST_PROPERTY, "foo")
        assertEquals("foo", propertiesService.get(KOTLIN_DEPRECATED_TEST_PROPERTY, project))
        project.evaluate()
        assertEquals("foo", propertiesService.get(KOTLIN_DEPRECATED_TEST_PROPERTY, project))
    }

    @Test
    fun `KT-85433 non-BTA compilation mode reports deprecation warning`() {
        val project = buildProjectWithMPP(
            preApplyCode = { enableBtaJvm(enabled = false) },
        ) {
            kotlin { jvm() }
        }.evaluate()
        project.checkDiagnostics("NonBtaCompilationModeDeprecated")
    }

    @Test
    fun `KT-85433 explicit BTA compilation mode reports deprecation warning`() {
        val project = buildProjectWithMPP(
            preApplyCode = { enableBtaJvm(enabled = true) },
        ) {
            kotlin { jvm() }
        }.evaluate()
        project.checkDiagnostics("NonBtaCompilationModeDeprecated")
    }
}
