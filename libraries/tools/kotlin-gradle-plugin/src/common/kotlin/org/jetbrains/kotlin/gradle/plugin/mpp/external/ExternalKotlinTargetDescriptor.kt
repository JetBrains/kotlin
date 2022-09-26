/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptor.DecoratedExternalTargetFactory
import kotlin.properties.Delegates

@ExternalKotlinTargetApi
interface ExternalKotlinTargetDescriptor<T : DecoratedExternalKotlinTarget> {

    fun interface DecoratedExternalTargetFactory<T : DecoratedExternalKotlinTarget> {
        fun create(target: ExternalKotlinTarget): T
    }

    val targetName: String
    val platformType: KotlinPlatformType
    val decoratedExternalTargetFactory: DecoratedExternalTargetFactory<T>
    val configure: ((T) -> Unit)?
}

@ExternalKotlinTargetApi
interface ExternalKotlinTargetDescriptorBuilder<T : DecoratedExternalKotlinTarget> {
    var targetName: String
    var platformType: KotlinPlatformType
    var decoratedExternalTargetFactory: DecoratedExternalTargetFactory<T>
    var configure: ((T) -> Unit)?
    fun configure(action: (T) -> Unit) = apply {
        val configure = this.configure
        if (configure == null) this.configure = action
        else this.configure = { configure(it); action(it) }
    }
}

@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> ExternalKotlinTargetDescriptor(
    configure: ExternalKotlinTargetDescriptorBuilder<T>.() -> Unit
): ExternalKotlinTargetDescriptor<T> {
    return ExternalKotlinTargetDescriptorBuilderImpl<T>().also(configure).build()
}

@ExternalKotlinTargetApi
private class ExternalKotlinTargetDescriptorBuilderImpl<T : DecoratedExternalKotlinTarget> : ExternalKotlinTargetDescriptorBuilder<T> {
    override var targetName: String by Delegates.notNull()
    override var platformType: KotlinPlatformType by Delegates.notNull()
    override var decoratedExternalTargetFactory: DecoratedExternalTargetFactory<T> by Delegates.notNull()
    override var configure: ((T) -> Unit)? = null

    fun build(): ExternalKotlinTargetDescriptorImpl<T> = ExternalKotlinTargetDescriptorImpl(
        targetName = targetName,
        platformType = platformType,
        decoratedExternalTargetFactory = decoratedExternalTargetFactory,
        configure = configure
    )
}

@ExternalKotlinTargetApi
private data class ExternalKotlinTargetDescriptorImpl<T : DecoratedExternalKotlinTarget>(
    override val targetName: String,
    override val platformType: KotlinPlatformType,
    override val decoratedExternalTargetFactory: DecoratedExternalTargetFactory<T>,
    override val configure: ((T) -> Unit)?
) : ExternalKotlinTargetDescriptor<T>


