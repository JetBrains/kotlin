/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptor.TargetFactory
import kotlin.properties.Delegates

@ExternalKotlinTargetApi
interface ExternalKotlinTargetDescriptor<T : DecoratedExternalKotlinTarget> {

    fun interface TargetFactory<T : DecoratedExternalKotlinTarget> {
        fun create(target: DecoratedExternalKotlinTarget.Delegate): T
    }

    val targetName: String
    val platformType: KotlinPlatformType
    val targetFactory: TargetFactory<T>

    val configure: ((T) -> Unit)?

    val apiElements: ExternalKotlinTargetConfigurationDescriptor<T>
    val runtimeElements: ExternalKotlinTargetConfigurationDescriptor<T>
    val apiElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>
    val runtimeElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>
}

@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> ExternalKotlinTargetDescriptor(
    configure: ExternalKotlinTargetDescriptorBuilder<T>.() -> Unit
): ExternalKotlinTargetDescriptor<T> {
    return ExternalKotlinTargetDescriptorBuilder<T>().also(configure).build()
}

@ExternalKotlinTargetApi
class ExternalKotlinTargetDescriptorBuilder<T : DecoratedExternalKotlinTarget> internal constructor() {
    var targetName: String by Delegates.notNull()
    var platformType: KotlinPlatformType by Delegates.notNull()
    var targetFactory: TargetFactory<T> by Delegates.notNull()

    val apiElements: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    val runtimeElements: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    val apiElementsPublished: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    val runtimeElementsPublished: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    var configure: ((T) -> Unit)? = null

    internal fun build(): ExternalKotlinTargetDescriptor<T> = ExternalKotlinTargetDescriptorImpl(
        targetName = targetName,
        platformType = platformType,
        targetFactory = targetFactory,
        apiElements = apiElements.build(),
        runtimeElements = runtimeElements.build(),
        apiElementsPublished = apiElementsPublished.build(),
        runtimeElementsPublished = runtimeElementsPublished.build(),
        configure = configure
    )
}

private data class ExternalKotlinTargetDescriptorImpl<T : DecoratedExternalKotlinTarget>(
    override val targetName: String,
    override val platformType: KotlinPlatformType,
    override val targetFactory: TargetFactory<T>,
    override val apiElements: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val runtimeElements: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val apiElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val runtimeElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val configure: ((T) -> Unit)?,
) : ExternalKotlinTargetDescriptor<T>


