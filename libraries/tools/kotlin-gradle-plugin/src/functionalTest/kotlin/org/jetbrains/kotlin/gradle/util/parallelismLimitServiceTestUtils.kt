/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.internal.compilerRunner.native.parallelismLimitServiceName

/**
 * Returns the [org.gradle.api.services.BuildServiceSpec.getMaxParallelUsages] configured for the
 * `KotlinNativeToolParallelismLimitService` registered for task type [T].
 */
internal inline fun <reified T : Task> Project.maxParallelUsagesOf(): Int? =
    gradle.sharedServices.registrations.getByName(parallelismLimitServiceName(T::class.java)).maxParallelUsages.orNull

internal inline fun <reified T : Task> Project.isParallelismLimitServiceRegistered(): Boolean =
    gradle.sharedServices.registrations.findByName(parallelismLimitServiceName(T::class.java)) != null
