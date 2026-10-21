/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

private const val CONCURRENCY_LIMIT_SERVICE_NAME = "KotlinNativeToolConcurrencyLimitService"

/**
 * Demo build service used to cap how many instances of a given task type (e.g. [KotlinNativeLink]) are allowed
 * to execute at the same time, regardless of how much parallelism (Workers API, Configuration Cache) would
 * otherwise be available.
 *
 * Each [runTool] invocation can spawn a memory-hungry Kotlin/Native compiler/linker process. Normally, a project
 * with several targets/build types ends up with many link/compile tasks, and - as seen with the Workers API
 * migration or with the Configuration Cache enabled - Gradle can execute quite a few of them concurrently, which
 * risks exhausting the Gradle daemon's memory.
 *
 * Gradle's own recommended pattern for this, described in the docs on "accessing a build service concurrently"
 * (https://docs.gradle.org/current/userguide/build_services.html#accessing_the_build_service_concurrently), is to
 * register a shared [BuildService] with [org.gradle.api.services.BuildServiceSpec.getMaxParallelUsages] set to the
 * desired concurrency limit, and have every task that should be throttled declare [org.gradle.api.Task.usesService].
 * Callers follow that pattern directly: they register their own, task-specific instance of this service (via
 * [registerKotlinNativeConcurrencyLimitService]) and declare [org.gradle.api.Task.usesService] themselves at task
 * configuration time - Gradle's own scheduling then enforces the limit, so no manual throttling is needed here.
 *
 * This is a demo implementation: it isn't wired up to any DSL/property beyond the [maxParallelUsages] parameter
 * callers pass to [registerKotlinNativeConcurrencyLimitService], and it isn't covered by tests.
 */
private abstract class KotlinNativeToolConcurrencyLimitService : BuildService<BuildServiceParameters.None>

/**
 * Registers (or reuses) the build service that caps how many instances of the calling task (identified by
 * [name], used as a suffix of the shared service name) are allowed to execute concurrently. Callers
 * (e.g. [KotlinNativeLink]) are meant to call this once at task *configuration* time, for their own task
 * type/[name], and declare [org.gradle.api.Task.usesService] with the returned provider - Gradle's own
 * task scheduling (driven by [org.gradle.api.services.BuildServiceSpec.getMaxParallelUsages]) then takes
 * care of capping concurrency, so every task type ends up with its own, independently-limited instance
 * of the service.
 *
 * The concrete service type is a private implementation detail of this file, so it's exposed here only
 * as a [BuildService], which is enough for callers to declare [org.gradle.api.Task.usesService].
 */
internal fun Project.registerKotlinNativeConcurrencyLimitService(
    name: String,
    maxParallelUsages: Int,
): Provider<out BuildService<*>> =
    gradle.sharedServices.registerIfAbsent(
        "$CONCURRENCY_LIMIT_SERVICE_NAME$name",
        KotlinNativeToolConcurrencyLimitService::class.java
    ) { spec ->
        spec.maxParallelUsages.set(maxParallelUsages)
    }
