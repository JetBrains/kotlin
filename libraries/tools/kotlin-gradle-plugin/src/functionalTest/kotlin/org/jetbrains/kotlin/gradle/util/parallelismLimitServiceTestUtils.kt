/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project

/**
 * Returns the [org.gradle.api.services.BuildServiceSpec.getMaxParallelUsages] configured for the
 * `KotlinNativeToolParallelismLimitService` registered under [taskName], or `null` if no limit was set.
 */
internal fun Project.maxParallelUsagesOf(taskName: String): Int? =
    gradle.sharedServices.registrations.getByName("KotlinNativeToolParallelismLimitService$taskName").maxParallelUsages.orNull

/**
 * Returns `true` if a `KotlinNativeToolParallelismLimitService` is registered under [taskName].
 */
internal fun Project.isParallelismLimitServiceRegistered(taskName: String): Boolean =
    gradle.sharedServices.registrations.findByName("KotlinNativeToolParallelismLimitService$taskName") != null
