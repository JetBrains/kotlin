/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.gradle.utils.withBuilder
import java.net.URI
import java.time.Duration


/**
 * Configuration representation for KotlinTestBrowserRunnerConfig
 * https://github.com/Kotlin/kotlin-web-helpers/blob/76102e1c7b0d36aaf94d96174598dc277f573c2f/window.d.ts#L48
 */
internal class KotlinBrowserRunnerConfig(
    val flowId: String = "default",
    val timeout: Duration = Duration.ofMinutes(2),
    val kotlinTestCliArguments: List<String> = emptyList(),
    val testsFinishedMarker: String = "KOTLIN_TEST_FINISHED",
) {
    fun buildUrlWithConfigState(base: URI): URI {
        val json = toKotlinTestRunnerConfigJsonString()
        return base.withBuilder { addQueryParam("kotlinTestConfig", json) }
    }

    fun toKotlinTestRunnerConfigJsonString(): String {
        val config = KotlinTestRunnerConfig(
            reporterOptions = ReporterOptions(flowId = flowId),
            mochaSetupOptions = MochaSetupOptions(timeout = timeout.toMillis().toString()),
            kotlinTestCliArguments = kotlinTestCliArguments,
            testsFinishedMarker = testsFinishedMarker,
        )
        return Json.encodeToString(KotlinTestRunnerConfig.serializer(), config)
    }

    @Serializable
    internal data class KotlinTestRunnerConfig(
        val reporterOptions: ReporterOptions,
        val mochaSetupOptions: MochaSetupOptions,
        val kotlinTestCliArguments: List<String>,
        val testsFinishedMarker: String,
    )

    @Serializable
    internal data class ReporterOptions(
        val flowId: String,
    )

    @Serializable
    internal data class MochaSetupOptions(
        val timeout: String,
    )
}
