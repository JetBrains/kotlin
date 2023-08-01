/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.internal.component.SoftwareComponentInternal
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.tooling.core.UnsafeApi

@InternalKotlinGradlePluginApi
abstract class InternalKotlinTargetComponent : KotlinTargetComponent, SoftwareComponentInternal {
    /**
     * Use 'kotlinUsagesFuture' instead
     */
    @UnsafeApi
    abstract override fun getUsages(): Set<KotlinUsageContext>

    internal abstract val kotlinUsagesFuture: Future<Set<DefaultKotlinUsageContext>>
}

internal val KotlinTargetComponent.internal: InternalKotlinTargetComponent
    get() = this as InternalKotlinTargetComponent