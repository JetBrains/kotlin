/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.artifacts.Configuration

interface KotlinGradleFragmentConfigurationProvider {
    fun getConfiguration(context: KotlinGradleFragmentConfigurationContext): Configuration
}

fun ConfigurationProvider(provider: KotlinGradleFragmentConfigurationContext.() -> Configuration):
        KotlinGradleFragmentConfigurationProvider = object : KotlinGradleFragmentConfigurationProvider {
    override fun getConfiguration(context: KotlinGradleFragmentConfigurationContext): Configuration = context.provider()
}
