/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project

/**
 * Autonomous/Independent/Encapsulated peace of code that has a single objective.
 * Typical applications:
 * ### Setup Actions:
 *
 * ```kotlin
 * internal val MyTaskSetupAction = KotlinProjectAction {
 *     tasks.create("myTask") {
 *         group = "myGroup"
 *         // ..
 *     }
 * }
 * ```
 */
internal fun interface KotlinProjectAction {
    suspend fun Project.runAction()

    companion object {
        val extensionPoint = KotlinExtensionPoint<KotlinProjectAction>()
    }
}

internal fun Project.runKotlinProjectActions() {
    KotlinProjectAction.extensionPoint(this).forEach { action ->
        with(action) { launch { runAction() } }
    }
}
