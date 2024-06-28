/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget

/**
 * Class intended to be used to implement a given adhoc/external KotlinTarget maintained outside kotlin.git.
 * The implementation will be backed by the [Delegate] holding an internal implementation of [KotlinTarget]
 * Methods of [KotlinTarget] will be handled by this backing implementation.
 * This method shall be used for decorating the class with additional, domain specific properties
 *
 * #### Sample
 *
 * ```kotlin
 * @OptIn(ExperimentalKotlinGradlePluginApi::class)
 * class MyCustomJvmTarget(delegate: Delegate): DecoratedExternalKotlinTarget(delegate),
 *     HasConfigurableCompilerOptions<KotlinJvmCompilerOptions> {
 *
 *     // Some property decorating our target
 *     val myCustomProperty: String = "hello there"
 *
 *     // Covariant override that allows for narrowing the type of compilations that this target contains
 *     override val compilations: NamedDomainObjectContainer<MyCustomCompilationType>
 *         get() = super.compilations as NamedDomainObjectContainer<MyCustomCompilationType>
 *
 *     // Covariant override of target compiler options that should be used to configure all target compilation compiler options
 *     @ExperimentalKotlinGradlePluginApi
 *     override val compilerOptions: KotlinJvmCompilerOptions
 *         get() = super.compilerOptions as KotlinJvmCompilerOptions
 * }
 * ```
 *
 * #### Create an instance
 * Creating an instance of such classes will require an instance of [Delegate].
 * This instance can only be retrieved by calling the [KotlinMultiplatformExtension.createExternalKotlinTarget] function,
 * providing a factory function in the [ExternalKotlinTargetDescriptor]
 */
@ExternalKotlinTargetApi
abstract class DecoratedExternalKotlinTarget internal constructor(
    internal val delegate: ExternalKotlinTargetImpl,
) : InternalKotlinTarget by delegate {
    constructor(delegate: Delegate) : this(delegate.impl)

    class Delegate internal constructor(internal val impl: ExternalKotlinTargetImpl)

    val apiElementsConfiguration: Configuration = delegate.apiElementsConfiguration

    val runtimeElementsConfiguration: Configuration = delegate.runtimeElementsConfiguration

    /**
     * @since 1.9.20
     */
    val sourcesElementsConfiguration: Configuration = delegate.sourcesElementsConfiguration

    val apiElementsPublishedConfiguration: Configuration = delegate.apiElementsPublishedConfiguration

    val runtimeElementsPublishedConfiguration: Configuration = delegate.runtimeElementsPublishedConfiguration

    /**
     * @since 1.9.20
     */
    val sourcesElementsPublishedConfiguration: Configuration = delegate.sourcesElementsPublishedConfiguration

    internal val logger: Logger = delegate.logger

    /**
     * Target implementation could override return type to the specific platform type:
     * - [KotlinPlatformType.common] - should be [KotlinCommonCompilerOptions]
     * - [KotlinPlatformType.jvm] or [KotlinPlatformType.androidJvm] - could be [KotlinJvmCompilerOptions]
     * - [KotlinPlatformType.js] or [KotlinPlatformType.wasm] - could be [KotlinJsCompilerOptions]
     * - [KotlinPlatformType.native] - could be [KotlinNativeCompilerOptions]
     *
     * Example:
     * ```kotlin
     * @OptIn(ExperimentalKotlinGradlePluginApi::class)
     * class MyCustomNativeTarget(delegate: Delegate): DecoratedExternalKotlinTarget(delegate),
     *     HasConfigurableCompilerOptions<KotlinNativeCompilerOptions> {
     *
     *     // Covariant override of target compiler options that should be used to configure
     *     // all target compilation compiler options
     *     @ExperimentalKotlinGradlePluginApi
     *     override val compilerOptions: KotlinNativeCompilerOptions
     *         get() = super.compilerOptions as KotlinNativeCompilerOptions
     * }
     * ```
     *
     * @since 2.0.0
     */
    @ExperimentalKotlinGradlePluginApi
    open val compilerOptions: KotlinCommonCompilerOptions = delegate.compilerOptions
}

internal val ExternalKotlinTargetImpl.decoratedInstance: DecoratedExternalKotlinTarget
    get() = project.multiplatformExtension.targets.getByName(name) as DecoratedExternalKotlinTarget

internal val ExternalKotlinTargetImpl.decoratedInstanceOrNull: DecoratedExternalKotlinTarget?
    get() = project.multiplatformExtensionOrNull?.targets?.findByName(name) as? DecoratedExternalKotlinTarget