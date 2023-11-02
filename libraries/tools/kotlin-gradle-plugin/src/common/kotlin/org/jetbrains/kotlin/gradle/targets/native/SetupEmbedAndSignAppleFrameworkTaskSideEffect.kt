/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedAndSignAppleFrameworkTask
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect

internal val SetupEmbedAndSignAppleFrameworkTaskSideEffect = KotlinTargetSideEffect<KotlinNativeTarget> { target ->
    if (!target.konanTarget.family.isAppleFamily) return@KotlinTargetSideEffect
    target.binaries.withType(Framework::class.java).all { framework ->
        target.project.registerEmbedAndSignAppleFrameworkTask(framework)
    }
}
