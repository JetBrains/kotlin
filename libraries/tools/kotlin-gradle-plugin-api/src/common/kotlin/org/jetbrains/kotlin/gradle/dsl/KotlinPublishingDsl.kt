/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPublishing

/**
 * A Kotlin extension DSL to configure publishing settings.
 */
interface KotlinPublishingDsl {
    /**
     * Provides [KotlinPublishing] DSL extension to configure Kotlin publication details.
     *
     * @since 2.1.20
     */
    @ExperimentalKotlinGradlePluginApi
    val publishing: KotlinPublishing

    /**
     * Configures [KotlinPublishing] DSL extension.
     *
     * @since 2.1.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun publishing(configure: KotlinPublishing.() -> Unit) = publishing.configure()

    /**
     * Configures [KotlinPublishing] DSL extension.
     *
     * @since 2.1.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun publishing(configure: Action<KotlinPublishing>) = configure.execute(publishing)
}