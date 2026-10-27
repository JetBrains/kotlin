/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

private const val PARALLELISM_LIMIT_SERVICE_NAME = "KotlinNativeToolParallelismLimitService"

/**
 * A fraction of the Gradle daemon's max heap that [maxParallelUsagesFromAvailableRam] is allowed to assume is
 * usable for running tool instances, leaving the rest as a buffer for everything else happening in the daemon.
 */
private const val AVAILABLE_RAM_USAGE_BUFFER_FACTOR = 0.7

private const val BYTES_IN_MEGABYTE = 1024 * 1024

/**
 * Build service used to cap how many instances of a given task type (e.g. [KotlinNativeLink]) are allowed
 * to execute at the same time, regardless of how much parallelism (Workers API, Configuration Cache) would
 * otherwise be available.
 *
 * Users should register the service using the [registerKotlinNativeParallelismLimitService] helper, providing
 * a unique name. There's no limitation on multiple task types using the same service to limit parallelism, but
 * the canonical use case is one service instance per task type.
 */
private abstract class KotlinNativeToolParallelismLimitService : BuildService<BuildServiceParameters.None>

/**
 * Registers (or reuses) the build service that caps how many instances of the calling task (identified by
 * [name], used as a suffix of the shared service name) are allowed to execute in parallel. Callers
 * (e.g. [KotlinNativeLink]) are meant to call this once at task *configuration* time, for their own task
 * type/[name], and declare [org.gradle.api.Task.usesService] with the returned provider - Gradle's own
 * task scheduling (driven by [org.gradle.api.services.BuildServiceSpec.getMaxParallelUsages]) then takes
 * care of capping parallelism, so every task type ends up with its own, independently-limited instance
 * of the service.
 *
 * The concrete service type is a private implementation detail of this file, so it's exposed here only
 * as a [BuildService], which is enough for callers to declare [org.gradle.api.Task.usesService].
 */
internal fun Project.registerKotlinNativeParallelismLimitService(
    name: String,
    maxParallelUsages: Int,
): Provider<out BuildService<*>> =
    registerKotlinNativeParallelismLimitService(name, maxParallelUsagesCalculatorStrategy = { maxParallelUsages })

/**
 * Same as the overload accepting `maxParallelUsages` directly, but instead the value is obtained by calling
 * [maxParallelUsagesCalculatorStrategy] and [maxParallelUsagesCalculatorFallbackStrategies], which allows it
 * to be calculated lazily, based on a chosen strategy (see [maxParallelUsagesFromGradleProperty] and
 * [maxParallelUsagesFromAvailableRam]). If neither strategy returns a value, no limit is set on the service.
 */
internal fun Project.registerKotlinNativeParallelismLimitService(
    name: String,
    maxParallelUsagesCalculatorStrategy: MaxParallelUsagesCalculatorStrategy,
    vararg maxParallelUsagesCalculatorFallbackStrategies: MaxParallelUsagesCalculatorStrategy,
): Provider<out BuildService<*>> =
    gradle.sharedServices.registerIfAbsent(
        "$PARALLELISM_LIMIT_SERVICE_NAME$name",
        KotlinNativeToolParallelismLimitService::class.java
    ) { spec ->
        val maxParallelUsages = maxParallelUsagesCalculatorStrategy.calculateMaxParallelUsages()
            ?: maxParallelUsagesCalculatorFallbackStrategies.firstNotNullOfOrNull { it.calculateMaxParallelUsages() }
        if (maxParallelUsages != null) {
            spec.maxParallelUsages.set(maxParallelUsages)
        }
    }

/**
 * A strategy for calculating the value to use as [org.gradle.api.services.BuildServiceSpec.getMaxParallelUsages]
 * when registering [KotlinNativeToolParallelismLimitService]. Returning `null` means "no limit should be set".
 *
 * Use helpers like [maxParallelUsagesFromGradleProperty] or [maxParallelUsagesFromAvailableRam] to obtain an instance,
 * instead of implementing this interface directly.
 */
internal fun interface MaxParallelUsagesCalculatorStrategy {
    fun calculateMaxParallelUsages(): Int?
}

/**
 * A [MaxParallelUsagesCalculatorStrategy] that reads the value from the Gradle property [propertyName], returning
 * `null` if the property isn't set, or its value isn't a valid integer.
 */
internal fun maxParallelUsagesFromGradleProperty(
    providers: ProviderFactory,
    propertyName: String,
): MaxParallelUsagesCalculatorStrategy =
    MaxParallelUsagesCalculatorStrategy {
        providers.gradleProperty(propertyName).orNull?.toIntOrNull()
    }

/**
 * A [MaxParallelUsagesCalculatorStrategy] that calculates how many parallel tool instances can run without
 * exhausting the Gradle daemon's available memory, assuming each instance uses approximately
 * [approximateTaskInstanceRamUsageMb] megabytes, and leaving a buffer so the whole heap isn't used up.
 */
internal fun maxParallelUsagesFromAvailableRam(
    approximateTaskInstanceRamUsageMb: Long,
): MaxParallelUsagesCalculatorStrategy =
    MaxParallelUsagesCalculatorStrategy {
        val daemonMaxMemoryMb = Runtime.getRuntime().maxMemory() / BYTES_IN_MEGABYTE
        val usableMemoryMb = (daemonMaxMemoryMb * AVAILABLE_RAM_USAGE_BUFFER_FACTOR).toLong()
        (usableMemoryMb / approximateTaskInstanceRamUsageMb).toInt().coerceAtLeast(1)
    }
