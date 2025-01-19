/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion

internal class ComposeWithAgpConfig(
    private val project: Project,
    private val agpConfigurationProvider: AgpConfigurationProvider = AgpConfigurationProvider.Default,
    private val agpVersionProvider: AgpVersionProvider = AgpVersionProvider.Default,
) {
    val agpComposeConfiguration get() = agpConfigurationProvider.get(project)
    val isAgpComposeEnabled get() = agpComposeConfiguration != null

    @OptIn(InternalKotlinGradlePluginApi::class)
    val isDisableIncludeSourceInformationForAgp: Boolean
        get() {
            val agpVersion = agpVersionProvider.get()
            return isAgpComposeEnabled &&
                    agpVersion != null &&
                    agpVersion < AndroidGradlePluginVersion("8.10.0-alpha02")
        }

    internal interface AgpConfigurationProvider {
        fun get(project: Project): Configuration?

        object Default : AgpConfigurationProvider {
            override fun get(project: Project): Configuration? {
                return project.configurations.findByName("kotlin-extension")
            }
        }
    }

    @OptIn(InternalKotlinGradlePluginApi::class)
    internal interface AgpVersionProvider {
        fun get(): AndroidGradlePluginVersion?

        object Default : AgpVersionProvider {
            override fun get(): AndroidGradlePluginVersion? {
                return AndroidGradlePluginVersion.currentOrNull
            }
        }
    }
}