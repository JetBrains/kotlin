/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.android.internal

import org.jetbrains.kotlin.gradle.PRESETS_DEPRECATION_MESSAGE_SUFFIX
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic

internal interface InternalKotlinTargetPreset<T : KotlinTarget> : KotlinTargetPreset<T> {
    fun createTargetInternal(name: String): T

    @Deprecated(
        "The KotlinTargetPreset.createTarget() $PRESETS_DEPRECATION_MESSAGE_SUFFIX",
        level = DeprecationLevel.WARNING
    )
    override fun createTarget(name: String): T {
        val target = createTargetInternal(name)
        target.project.reportDiagnostic(KotlinToolingDiagnostics.CreateTarget())
        return target
    }
}

internal val <T : KotlinTarget> KotlinTargetPreset<T>.internal: InternalKotlinTargetPreset<T>
    get() = this as InternalKotlinTargetPreset