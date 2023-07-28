/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetComponent
import org.jetbrains.kotlin.gradle.utils.Future

internal fun KotlinTargetComponent.kotlinUsagesFutureOrNull(): Future<Set<DefaultKotlinUsageContext>>? = when (this) {
    is KotlinVariant -> kotlinUsagesFuture
    is JointAndroidKotlinTargetComponent -> kotlinUsagesFuture
    is ExternalKotlinTargetComponent -> kotlinUsagesFuture
    else -> null
}

internal suspend fun KotlinTargetComponent.awaitKotlinUsagesOrEmpty() = kotlinUsagesFutureOrNull()?.await().orEmpty()
