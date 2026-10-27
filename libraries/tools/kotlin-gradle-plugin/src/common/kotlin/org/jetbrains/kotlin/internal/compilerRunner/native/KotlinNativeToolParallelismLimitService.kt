/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

private const val PARALLELISM_LIMIT_SERVICE_NAME = "KotlinNativeToolParallelismLimitService"

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
    gradle.sharedServices.registerIfAbsent(
        "$PARALLELISM_LIMIT_SERVICE_NAME$name",
        KotlinNativeToolParallelismLimitService::class.java
    ) { spec ->
        spec.maxParallelUsages.set(maxParallelUsages)
    }
