/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
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
    private val providers: ProviderFactory,
    private val gradle: Gradle,
) : KotlinNativeSettingsDsl {

    private val logger = Logging.getLogger(KotlinNativeSettingsDslImpl::class.java)

    override val compileMaxWorkers: Property<Int> = objects.maxWorkersProperty(KOTLIN_NATIVE_COMPILE_MAX_WORKERS)
    override val linkMaxWorkers: Property<Int> = objects.maxWorkersProperty(KOTLIN_NATIVE_LINK_MAX_WORKERS)
    override val commonizeCInteropMaxWorkers: Property<Int> = objects.maxWorkersProperty(KOTLIN_NATIVE_COMMONIZE_CINTEROP_MAX_WORKERS)

    init {
        gradle.settingsEvaluated { registerParallelismLimitServices() }
    }

    internal fun registerParallelismLimitServices() {
        registerIfSet(KotlinNativeCompile::class.java, compileMaxWorkers, KOTLIN_NATIVE_COMPILE_MAX_WORKERS)
        registerIfSet(KotlinNativeLink::class.java, linkMaxWorkers, KOTLIN_NATIVE_LINK_MAX_WORKERS)
        registerIfSet(CInteropCommonizerTask::class.java, commonizeCInteropMaxWorkers, KOTLIN_NATIVE_COMMONIZE_CINTEROP_MAX_WORKERS)
    }

    private fun registerIfSet(taskType: Class<out Task>, maxWorkers: Property<Int>, gradlePropertyName: String) {
        if (!maxWorkers.isPresent) return
        val value = maxWorkers.get()
        val overridden = gradlePropertyValue(gradlePropertyName)
        if (overridden.isPresent && overridden.get() != value) {
            logger.lifecycle("Kotlin/Native settings DSL value $value overrides Gradle property $gradlePropertyName=${overridden.get()}")
        }
        gradle.registerKotlinNativeParallelismLimitService(taskType, value)
    }

    private fun ObjectFactory.maxWorkersProperty(gradlePropertyName: String): Property<Int> =
        propertyWithConvention(gradlePropertyValue(gradlePropertyName))

    private fun gradlePropertyValue(gradlePropertyName: String): Provider<Int> =
        providers.gradleProperty(gradlePropertyName).map { it.toIntOrNull() }
}
