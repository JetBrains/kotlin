/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation

internal fun Project.getCommonizerTarget(compilation: KotlinCompilation<*>): CommonizerTarget? {
    if (compilation is KotlinNativeCompilation) {
        return LeafCommonizerTarget(compilation.konanTarget)
    }

    if (compilation is KotlinSharedNativeCompilation) {
        return getCommonizerTarget(compilation.defaultSourceSet) ?: CommonizerTarget(compilation.konanTargets)
    }

    return null
}
