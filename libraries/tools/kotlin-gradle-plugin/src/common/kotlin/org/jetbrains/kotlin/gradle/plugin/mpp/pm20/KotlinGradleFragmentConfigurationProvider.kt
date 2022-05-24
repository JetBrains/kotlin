/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration

interface GradleKpmConfigurationProvider {
    fun getConfiguration(context: GradleKpmFragmentConfigureContext): Configuration
}

fun GradleKpmConfigurationProvider(provider: GradleKpmFragmentConfigureContext.() -> Configuration):
        GradleKpmConfigurationProvider = object : GradleKpmConfigurationProvider {
    override fun getConfiguration(context: GradleKpmFragmentConfigureContext): Configuration = context.provider()
}
