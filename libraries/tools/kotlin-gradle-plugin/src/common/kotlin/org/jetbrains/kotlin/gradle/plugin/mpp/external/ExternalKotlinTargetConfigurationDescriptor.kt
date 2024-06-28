/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi

@ExternalKotlinTargetApi
interface ExternalKotlinTargetConfigurationDescriptor<T : DecoratedExternalKotlinTarget> {
    val configure: ((target: T, configuration: Configuration) -> Unit)?
}

@ExternalKotlinTargetApi
class ExternalKotlinTargetConfigurationDescriptorBuilder<T : DecoratedExternalKotlinTarget> internal constructor() {

    internal var configure: ((target: T, configuration: Configuration) -> Unit)? = null

    fun configure(action: (target: T, configuration: Configuration) -> Unit) = apply {
        val configure = this.configure
        if (configure == null) this.configure = action
        else this.configure = { target, configuration ->
            configure(target, configuration)
            action(target, configuration)
        }
    }

    internal fun build(): ExternalKotlinTargetConfigurationDescriptor<T> {
        return ExternalKotlinTargetConfigurationDescriptorImpl(configure)
    }
}

private data class ExternalKotlinTargetConfigurationDescriptorImpl<T : DecoratedExternalKotlinTarget>(
    override val configure: ((target: T, configuration: Configuration) -> Unit)?
) : ExternalKotlinTargetConfigurationDescriptor<T>
