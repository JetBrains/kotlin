/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeSettingsDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_COMMONIZE_CINTEROP_MAX_WORKERS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_COMPILE_MAX_WORKERS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_LINK_MAX_WORKERS
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention

/**
 * Backs `kotlin { native { ... } }` of the `org.jetbrains.kotlin.ecosystem` settings plugin.
 *
 * After settings are evaluated, each value that is set registers the parallelism limit service of its task type.
 * The service is shared by the whole build and the first registration wins, so these values override the ones
 * the tasks would read from Gradle properties.
 */
@ExperimentalKotlinGradlePluginApi
@InternalKotlinGradlePluginApi
class KotlinNativeSettingsDslImpl(
    objects: ObjectFactory,
    providers: ProviderFactory,
    private val gradle: Gradle,
) : KotlinNativeSettingsDsl {

    override val compileMaxWorkers: Property<Int> = objects.maxWorkersProperty(providers, KOTLIN_NATIVE_COMPILE_MAX_WORKERS)
    override val linkMaxWorkers: Property<Int> = objects.maxWorkersProperty(providers, KOTLIN_NATIVE_LINK_MAX_WORKERS)
    override val commonizeCInteropMaxWorkers: Property<Int> =
        objects.maxWorkersProperty(providers, KOTLIN_NATIVE_COMMONIZE_CINTEROP_MAX_WORKERS)

    init {
        gradle.settingsEvaluated { registerParallelismLimitServices() }
    }

    internal fun registerParallelismLimitServices() {
        registerIfSet(KotlinNativeCompile::class.java, compileMaxWorkers)
        registerIfSet(KotlinNativeLink::class.java, linkMaxWorkers)
        registerIfSet(CInteropCommonizerTask::class.java, commonizeCInteropMaxWorkers)
    }

    private fun registerIfSet(taskType: Class<out Task>, maxWorkers: Property<Int>) {
        maxWorkers.orNull?.let { gradle.registerKotlinNativeParallelismLimitService(taskType, it) }
    }

    private fun ObjectFactory.maxWorkersProperty(providers: ProviderFactory, gradlePropertyName: String): Property<Int> =
        propertyWithConvention(
            providers.gradleProperty(gradlePropertyName).map { it.toIntOrNull() }
        )
}
