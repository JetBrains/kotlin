/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.property
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `webpack.config.js` is executed as JavaScript and validated against webpack's own schema, so these assert the
 * shape of the emitted JSON rather than just that serialization succeeded.
 */
class KotlinWebpackConfigSerializationTest {

    private val project: Project = buildProject()

    private fun webpackConfig(configure: KotlinWebpackConfig.() -> Unit): String {
        val config = KotlinWebpackConfig(
            rules = project.objects.webpackRulesContainer(),
            defineNonBrowserEnvironmentProperties = project.objects.property<Boolean>().convention(false),
        ).apply(configure)
        return StringBuilder().also { config.appendTo(it) }.toString()
    }

    /**
     * Extracts the pretty-printed JSON object assigned by `config.<something> = … { … };` by matching braces from
     * the assignment onwards.
     */
    private fun String.assignedJsonObject(marker: String): JsonObject {
        val start = indexOf('{', startIndex = indexOf(marker).also { check(it >= 0) { "no `$marker` in:\n$this" } })
        var depth = 0
        for (i in start until length) {
            when (this[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return Json.parseToJsonElement(substring(start, i + 1)).jsonObject
            }
        }
        error("unbalanced braces after `$marker` in:\n$this")
    }

    @Test
    fun `devServer client overlay is emitted as an object`() {
        val generated = webpackConfig {
            devServer = KotlinWebpackConfig.DevServer(
                client = KotlinWebpackConfig.DevServer.Client(
                    KotlinWebpackConfig.DevServer.Client.Overlay(errors = true, warnings = false)
                )
            )
        }

        val overlay = generated.assignedJsonObject("config.devServer =")["client"]!!.jsonObject["overlay"]!!.jsonObject
        assertEquals(true, overlay["errors"]!!.jsonPrimitive.boolean)
        assertEquals(false, overlay["warnings"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `optimization omits null fields`() {
        // this is exactly what KotlinKarma configures; webpack rejects an explicit null for runtimeChunk
        val generated = webpackConfig {
            optimization = KotlinWebpackConfig.Optimization(runtimeChunk = null, splitChunks = false)
        }

        val optimization = generated.assignedJsonObject("config.optimization =")
        assertFalse("runtimeChunk" in optimization, "expected no runtimeChunk key, got $optimization")
        assertEquals(false, optimization["splitChunks"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `generated config is indented with two spaces`() {
        val generated = webpackConfig {
            optimization = KotlinWebpackConfig.Optimization(runtimeChunk = "single", splitChunks = false)
        }

        assertTrue(
            generated.lineSequence().any { it == "  \"runtimeChunk\": \"single\"," },
            "expected a two-space indented key in:\n$generated"
        )
    }
}
