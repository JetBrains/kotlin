/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * ### Extension Point for the Kotlin Gradle Plugin
 *
 * Note: Not stable for implementation: Custom implementations of this interface will not be supported by the Kotlin Gradle Plugin!
 *
 * #### Motivation
 * This [KotlinGradlePluginExtensionPoint] will be used to 'generically' extend parts of the Kotlin Gradle Plugin (internally and externally).
 * General Kotlin Gradle Plugin code will call into this 'semantically' structured extension points to load logic
 * Such extension points are to be used for public as well as for internal extension points.
 *
 * ### Usage
 * #### Declaring an Extension Point (internal in Kotlin Gradle Plugin)
 * Extension Points are to be declared on companion objects on the corresponding interface
 * ```kotlin
 * interface MyKotlinGradlePluginExtension {
 *     fun foo(project: Project)
 *
 *     companion object {
 *         val extensionPoint = KotlinExtensionPoint<MyKotlinGradlePluginExtension>()
 *     }
 * }
 * ```
 *
 * #### Registering an Extension Point
 * Extension Points are scoped by [Project] and should be registered as early as possible
 * ```kotlin
 * class MyPlugin: Plugin<Project> {
 *     fun apply(project: Project) {
 *         MyKotlinGradlePluginExtension.extensionPoint.register(project, MyKotlinGradlePluginExtensionImpl())
 *     }
 * }
 * ```
 *
 * #### Kotlin Gradle Plugin Entry Point:
 * Extensions registered by the Kotlin Gradle Plugin directly will use the
 * [org.jetbrains.kotlin.gradle.plugin.registerKotlinPluginExtensions] entry point.
 */
@ExperimentalKotlinGradlePluginApi
interface KotlinGradlePluginExtensionPoint<T> {
    /**
     * @return all currently registered extension points.
     * The returned list is *not* live and just represents the current snapshot.
     */
    operator fun get(project: Project): List<T>

    /**
     * @param project The current [project] to register an extension. The extension will only be visible to this particular [project]
     * @param extension The implementation of the extension to register. If registered twice, it will be returned twice when
     * the extensions are queried (no de-duplication)
     */
    fun register(project: Project, extension: T)
}
