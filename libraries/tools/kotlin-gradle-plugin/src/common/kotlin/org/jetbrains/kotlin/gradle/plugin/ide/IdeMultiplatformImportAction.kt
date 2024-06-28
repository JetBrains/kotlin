/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await

/**
 * Registers a generic action that shall only run when a Kotlin Multiplatform project is being imported into the IDE.
 * Note: For heavy workloads, please consider using the 'prepareKotlinIdeaImport' task infrastructure.
 * (e.g. by implementing [IdeDependencyResolver.WithBuildDependencies]) instead.
 *
 * Note: The *exact* time of execution is not guaranteed and can be considered an implementation detail, however
 * it is guaranteed that the action will be executed
 * - only during Gradle import
 * - before any Kotlin Multiplatform Model will be built
 *
 *
 * Example of registering an action:
 *
 * ```kotlin
 *  kotlinExtension.createExternalKotlinTarget<MyExternalTarget> {
 *         configureIdeImport {
 *             registerImportAction {
 *                 println("Running ide import") // Will be executed during Gradle sync
 *             }
 *
 *             registerImportAction(MyIdeImportAction) // Will be executed during Gradle sync
 *         }
 *     }
 * ```
 */
@ExternalKotlinTargetApi
fun interface IdeMultiplatformImportAction {
    fun prepareIdeImport(project: Project)

    companion object {
        internal val extensionPoint = KotlinGradlePluginExtensionPoint<IdeMultiplatformImportAction>()
    }
}

internal val IdeMultiplatformImportActionSetupAction = KotlinProjectSetupCoroutine {
    if (!isInIdeaSync) return@KotlinProjectSetupCoroutine
    KotlinPluginLifecycle.Stage.ReadyForExecution.await()
    IdeMultiplatformImportAction.extensionPoint[this].forEach { action ->
        action.prepareIdeImport(this)
    }
}
