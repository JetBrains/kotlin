/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.tasks.withType

private const val PARALLELISM_LIMIT_SERVICE_NAME = "KotlinNativeToolParallelismLimitService"

/**
 * Build service used to cap how many instances of a given task type (e.g. [KotlinNativeLink]) are allowed
 * to execute at the same time, regardless of how much parallelism (Workers API, Configuration Cache) would
 * otherwise be available.
 *
 * Register via [registerKotlinNativeParallelismLimitService], one service per task type.
 */
private abstract class KotlinNativeToolParallelismLimitService : BuildService<BuildServiceParameters.None>

/**
 * Registers (or reuses) the service that caps parallel tasks of [taskType] at [maxParallelUsages].
 */
internal fun Gradle.registerKotlinNativeParallelismLimitService(
    taskType: Class<out Task>,
    maxParallelUsages: Int,
): Provider<out BuildService<*>> =
    sharedServices.registerIfAbsent(
        parallelismLimitServiceName(taskType),
        KotlinNativeToolParallelismLimitService::class.java
    ) { spec ->
        spec.maxParallelUsages.set(maxParallelUsages)
    }

internal fun parallelismLimitServiceName(taskType: Class<out Task>): String =
    "$PARALLELISM_LIMIT_SERVICE_NAME${taskType.simpleName}"

/**
 * Makes [KotlinNativeCompile], [KotlinNativeLink] and [CInteropCommonizerTask] use their parallelism limit service,
 * when the matching `kotlin.native.*MaxWorkers` property or settings DSL value is set.
 */
internal val KotlinNativeToolParallelismLimitSetupAction = KotlinProjectSetupAction {
    limitParallelismOf<KotlinNativeCompile>(kotlinPropertiesProvider.nativeCompileMaxWorkers)
    limitParallelismOf<KotlinNativeLink>(kotlinPropertiesProvider.nativeLinkMaxWorkers)
    limitParallelismOf<CInteropCommonizerTask>(kotlinPropertiesProvider.nativeCommonizeCInteropMaxWorkers)
}

private inline fun <reified T : Task> Project.limitParallelismOf(maxParallelUsages: Int?) {
    val limitService = gradle.sharedServices.registrations.findByName(parallelismLimitServiceName(T::class.java))?.service
        ?: maxParallelUsages?.let { gradle.registerKotlinNativeParallelismLimitService(T::class.java, it) }
        ?: return
    tasks.withType<T>().configureEach { it.usesService(limitService) }
}
