/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.CoroutineStart.Undispatched
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.launch

internal val KotlinProjectExtension.targets: Iterable<KotlinTarget>
    get() = when (this) {
        is KotlinSingleTargetExtension<*> -> listOf(this.target)
        is KotlinMultiplatformExtension -> targets
        else -> error("Unexpected 'kotlin' extension $this")
    }


internal fun KotlinProjectExtension.forAllTargets(action: (target: KotlinTarget) -> Unit) {
    when (this) {
        is KotlinSingleTargetExtension<*> -> project.launch(Undispatched) { action(targetFuture.await()) }
        is KotlinMultiplatformExtension -> targets.all(action)
        else -> error("Unexpected 'kotlin' extension $this")
    }
}