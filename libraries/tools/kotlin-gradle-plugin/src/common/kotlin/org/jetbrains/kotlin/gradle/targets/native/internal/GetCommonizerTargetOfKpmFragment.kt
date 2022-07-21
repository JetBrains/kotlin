/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmNativeVariantInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.containingVariants

internal fun Project.getCommonizerTarget(fragment: GradleKpmFragment): CommonizerTarget? {
    val konanTargets = fragment.containingVariants.map { variant ->
        if (variant !is GradleKpmNativeVariantInternal) return null
        variant.konanTarget
    }
    return CommonizerTarget(konanTargets)
}