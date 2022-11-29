/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget

@ExternalKotlinTargetApi
abstract class DecoratedExternalKotlinTarget internal constructor(
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