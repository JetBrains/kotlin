/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider

/**
 * Interface for generic checks of a Gradle Project with any Kotlin Gradle Plugin applied.
 * Checkers are launched within the [KotlinPluginLifecycle] during the [Plugin.apply] function.
 * The respective [runChecks] method is marked as suspending and should await for necessary data to be finally available.
 *
 * This ensures that the checker can emit the warning as soon as possible, while still only running on healthy projects
 * (removing noisy induced errors).
 *
 * #### Why prefer suspend functions over generic `KotlinPluginLifecycle.Stage.ReadyForExecution.await()`?
 * Fine-grained suspension calls will improve the checker by enabling the checker to emit the Diagnostic as soon as it really becomes
 * relevant. Let's consider the example where an exception is thrown during [KotlinPluginLifecycle.Stage.FinaliseRefinesEdges].
 * (this might happen on ill-defined projects having ill-defined target hierarchies declared). The checker in the example however,
 * only verifies the user input, so it only requires some properties final value:
 *
 *```kotlin
 *
 * // correct implementation
 * override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
 *   ↪ val myFinalValue = getUserProperty().awaitFinalValue()
 *     if(!myFinalValue.isAllowed) {
 *         // emit diagnostic
 *     }
 * }
 *
 * // non-ideal / lazy implementation
 * override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
 *   ↪ Stage.ReadyForExecution.await()
 *     val myFinalValue = getUserProperty().orNull
 *     if(!myFinalValue.isAllowed) {
 *         // emit diagnostic
 *     }
 * }
 * ```
 *
 * In this example the diagnostic will not be emitted in the non-ideal implementation as the exception thrown in previous Stage
 * will never cause the 'isAllowed' check to never execute. Maybe the illegal property value was causing the exception? User will never know!
 *
 * However, the correct implementation can unsuspend as soon as the relevant data is available. This will run before the exception,
 * the diagnostic will be emitted and user can be warned about the issue!
 *
 * #### Example: Checker requiring all source sets to be there:
 * ```kotlin
 * override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
 *     ↪ project.multiplatformExtension.awaitSourceSets() // <- instead of .sourceSets | suspending until finally available
 * }
 * ```
 *
 * #### Example: Checker that requires platform compilations of a given SourceSet
 * ```kotlin
 * override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
 *      ↪   project.multiplatformExtension.awaitSourceSets().getByName("commonMain") // suspending until all source sets available
 *      ↪       .internal.awaitPlatformCompilations() // suspending until all platform compilations are finally known
 * }
 * ```
 *
 * #### Example: Checker that just wants to run in a given [KotlinPluginLifecycle.Stage]
 * ```kotlin
 * override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
 *      ↪   KotlinPluginLifecycle.Stage.FinaliseDsl.await() // suspending until FinaliseDsl stage is reached
 * }
 *
 * ```
 *
 * #### Example: Very simple checker without fine-grained of data requirements
 * If building a simple project checker which requires a fully successfully configured project, but the data requirements
 * cannot (yet) be implemented by calling into respective suspend functions, then awaiting the [KotlinPluginLifecycle.Stage.ReadyForExecution]
 * stage will be sufficient.
 * ```kotlin
 * override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
 *      ↪   KotlinPluginLifecycle.Stage.ReadyForExecution.await()
 *      // ... run my check on fully configured project
 * }
 * ```
 * However, this approach is discouraged and shall only be used if there are no suspend functions available to express the
 * requirements of this checker.
 *
 */
internal interface KotlinGradleProjectChecker {
    suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector)

    companion object {
        val extensionPoint = KotlinGradlePluginExtensionPoint<KotlinGradleProjectChecker>()
    }
}

internal open class KotlinGradleProjectCheckerContext(
    val project: Project,
    val kotlinPropertiesProvider: PropertiesProvider,
    val multiplatformExtension: KotlinMultiplatformExtension?,
)
