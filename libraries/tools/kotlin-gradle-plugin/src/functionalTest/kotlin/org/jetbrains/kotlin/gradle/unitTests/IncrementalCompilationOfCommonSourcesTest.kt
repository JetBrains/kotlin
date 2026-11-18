/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_WASM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildKMPWithAllBackends
import org.jetbrains.kotlin.gradle.utils.withType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IncrementalCompilationOfCommonSourcesTest {

    @Test
    fun disabledByDefaultForEveryTarget() {
        assertEnabledTargets(
            properties = emptyMap(),
            expectedJvm = false, expectedJs = false, expectedWasm = false,
        )
    }

    @Test
    fun jvmPropertyAffectsOnlyJvmTasks() {
        assertEnabledTargets(
            properties = mapOf(KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to true),
            expectedJvm = true, expectedJs = false, expectedWasm = false,
        )
    }

    @Test
    fun jsPropertyAffectsOnlyJsTasks() {
        assertEnabledTargets(
            properties = mapOf(KOTLIN_JS_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to true),
            expectedJvm = false, expectedJs = true, expectedWasm = false,
        )
    }

    @Test
    fun wasmPropertyAffectsOnlyWasmTasks() {
        assertEnabledTargets(
            properties = mapOf(KOTLIN_WASM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to true),
            expectedJvm = false, expectedJs = false, expectedWasm = true,
        )
    }

    private fun assertEnabledTargets(
        properties: Map<String, Boolean>,
        expectedJvm: Boolean,
        expectedJs: Boolean,
        expectedWasm: Boolean,
    ) {
        val project = buildKMPWithAllBackends(preApplyCode = {
            properties.forEach { (name, value) -> extraProperties.set(name, value.toString()) }
        })

        project.evaluate()

        val jvmTasks = project.tasks.withType<KotlinCompile>().toList()
        val (wasmTasks, jsTasks) = project.tasks.withType<Kotlin2JsCompile>().partition { it.getIsWasmPlatform.get() }
        assertTrue(jvmTasks.isNotEmpty() && jsTasks.isNotEmpty() && wasmTasks.isNotEmpty(), "Missing compile tasks to check")

        val expected = jvmTasks.associate { it.path to expectedJvm } +
                jsTasks.associate { it.path to expectedJs } +
                wasmTasks.associate { it.path to expectedWasm }
        val actual = jvmTasks.associate { it.path to it.enableUnsafeIncrementalCompilationForMultiplatform.get() } +
                (jsTasks + wasmTasks).associate { it.path to it.enableUnsafeIncrementalCompilationForMultiplatform.get() }

        assertEquals(expected, actual)
    }
}
