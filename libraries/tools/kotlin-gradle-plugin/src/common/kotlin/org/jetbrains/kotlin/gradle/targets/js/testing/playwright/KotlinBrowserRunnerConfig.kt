/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.withBuilder
import java.net.URI
import java.time.Duration
import javax.inject.Inject


/**
 * Configuration representation for KotlinTestBrowserRunnerConfig
 * https://github.com/Kotlin/kotlin-web-helpers/blob/76102e1c7b0d36aaf94d96174598dc277f573c2f/window.d.ts#L48
 *
 * TODO: There is no reason right now to use Gradle Properties API in this class, as it will be instantiated at execution time
 */
internal abstract class KotlinBrowserRunnerConfig
@Inject internal constructor(objects: ObjectFactory) {
    @get:Input
    val flowId: Property<String> = objects.property<String>().convention("default")

    @get:Input
    val timeout: Property<Duration> = objects.property<Duration>().convention(Duration.ofMinutes(2))

    @get:Input
    val kotlinTestCliArguments: ListProperty<String> = objects.listProperty()

    @get:Input
    val testsFinishedMarker: Property<String> = objects.property<String>().convention("KOTLIN_TEST_FINISHED")

    fun buildUrlWithConfigState(base: URI, extraKotlinTestCliArguments: List<String>? = null): URI {
        val json = toKotlinTestRunnerConfigJsonString(extraKotlinTestCliArguments)
        return base.withBuilder { addQueryParam("kotlinTestConfig", json) }
    }

    fun toKotlinTestRunnerConfigJsonString(
        extraKotlinTestCliArguments: List<String>? = null
    ): String {
        val config = KotlinTestRunnerConfig(
            reporterOptions = ReporterOptions(flowId = flowId.get()),
            mochaSetupOptions = MochaSetupOptions(timeout = timeout.get().toMillis().toString()),
            kotlinTestCliArguments = kotlinTestCliArguments.get() + extraKotlinTestCliArguments.orEmpty(),
            testsFinishedMarker = testsFinishedMarker.get(),
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
