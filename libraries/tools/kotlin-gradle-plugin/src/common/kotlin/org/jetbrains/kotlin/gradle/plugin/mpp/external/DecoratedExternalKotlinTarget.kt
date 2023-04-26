/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
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
 * class MyCustomJvmTarget(delegate: Delegate): DecoratedExternalKotlinTarget(delegate) {
 *     // Some property decorating our target
 *     val myCustomProperty: String = "hello there"
 *
 *     // Covariant override that allows for narrowing the type of compilations that this target contains
 *     override val compilations: NamedDomainObjectContainer<MyCustomCompilationType>
 *         get() = super.compilations as NamedDomainObjectContainer<MyCustomCompilationType>
 * }
 * ```
 *
 * #### Create an instance
 * Creating an instance of such classes will require an instance of [Delegate].
 * This instance can only be retrieved by calling the [KotlinMultiplatformExtension.createExternalKotlinTarget] function,
 * providing a factory function in the [ExternalKotlinTargetDescriptor]
 */
@ExternalKotlinTargetApi
open class DecoratedExternalKotlinTarget internal constructor(
    internal val delegate: ExternalKotlinTargetImpl
) : InternalKotlinTarget by delegate {
    constructor(delegate: Delegate) : this(delegate.impl)

    class Delegate internal constructor(internal val impl: ExternalKotlinTargetImpl)

    val apiElementsConfiguration: Configuration = delegate.apiElementsConfiguration

    val runtimeElementsConfiguration: Configuration = delegate.runtimeElementsConfiguration

    val apiElementsPublishedConfiguration: Configuration = delegate.apiElementsPublishedConfiguration

    val runtimeElementsPublishedConfiguration: Configuration = delegate.runtimeElementsPublishedConfiguration

    internal val logger: Logger = delegate.logger
}

internal val ExternalKotlinTargetImpl.decoratedInstance: DecoratedExternalKotlinTarget
    get() = project.multiplatformExtension.targets.getByName(name) as DecoratedExternalKotlinTarget

internal val ExternalKotlinTargetImpl.decoratedInstanceOrNull: DecoratedExternalKotlinTarget?
    get() = project.multiplatformExtensionOrNull?.targets?.findByName(name) as? DecoratedExternalKotlinTarget