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
}

@ExternalKotlinTargetApi
interface ExternalKotlinTargetDescriptorBuilder<T : DecoratedExternalKotlinTarget> {
    var targetName: String
    var platformType: KotlinPlatformType
    var targetFactory: TargetFactory<T>
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
    override var targetFactory: TargetFactory<T> by Delegates.notNull()
    override var configure: ((T) -> Unit)? = null

    fun build(): ExternalKotlinTargetDescriptorImpl<T> = ExternalKotlinTargetDescriptorImpl(
        targetName = targetName,
        platformType = platformType,
        targetFactory = targetFactory,
        configure = configure
    )
}

@ExternalKotlinTargetApi
private data class ExternalKotlinTargetDescriptorImpl<T : DecoratedExternalKotlinTarget>(
    override val targetName: String,
    override val platformType: KotlinPlatformType,
    override val targetFactory: TargetFactory<T>,
    override val configure: ((T) -> Unit)?
) : ExternalKotlinTargetDescriptor<T>


