/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.junit.jupiter.api.Test
import kotlin.test.*

@OptIn(InternalKotlinGradlePluginApi::class)
class ComposeWithAgpConfigTest {

    private val project = ProjectBuilder.builder().build()
    private val agpConfigurationProvider = TestAgpConfigurationProvider()
    private val agpVersionProvider = TestAgpVersionProvider()
    private val composeAgpConfig = ComposeWithAgpConfig(
        project,
        agpConfigurationProvider,
        agpVersionProvider,
    )

    @Test
    fun shouldReturnAgpIsDisableOnMissingConfiguration() {
        agpConfigurationProvider.configuration = null

        assertFalse(composeAgpConfig.isAgpComposeEnabled)
    }

    @Test
    fun shouldReturnAgpIsEnabledOnConfigurationPresent() {
        agpConfigurationProvider.configuration = project.configurations.dependencyScope("test").get()

        assertTrue(composeAgpConfig.isAgpComposeEnabled)
    }

    @Test
    fun shouldEnableIncludeSourceInformationIfAgpComposeIsDisabled() {
        agpConfigurationProvider.configuration = null

        assertFalse(composeAgpConfig.isDisableIncludeSourceInformationForAgp)
    }

    @Test
    fun shouldEnableIncludeSourceInformationIfAgpComposeIsEnabledButVersionIsNull() {
        agpConfigurationProvider.configuration = project.configurations.dependencyScope("test").get()
        agpVersionProvider.agpVersion = null

        assertFalse(composeAgpConfig.isDisableIncludeSourceInformationForAgp)
    }

    @Test
    fun shouldEnableIncludeSourceInformationIfAgpVersion8100Alpha02OrAbove() {
        agpConfigurationProvider.configuration = project.configurations.dependencyScope("test").get()
        agpVersionProvider.agpVersion = AndroidGradlePluginVersion(8, 10, 0, "alpha02")

        assertFalse(composeAgpConfig.isDisableIncludeSourceInformationForAgp)
    }

    @Test
    fun shouldDisableIncludeSourceInformationIfAgpVersion8100Alpha01OrBelow() {
        agpConfigurationProvider.configuration = project.configurations.dependencyScope("test").get()
        agpVersionProvider.agpVersion = AndroidGradlePluginVersion(8, 10, 0, "alpha01")

        assertTrue(composeAgpConfig.isDisableIncludeSourceInformationForAgp)
    }

    private class TestAgpConfigurationProvider : ComposeWithAgpConfig.AgpConfigurationProvider {
        var configuration: Configuration? = null

        override fun get(project: Project): Configuration? {
            return configuration
        }
    }

    private class TestAgpVersionProvider : ComposeWithAgpConfig.AgpVersionProvider {
        var agpVersion: AndroidGradlePluginVersion? = null
        override fun get(): AndroidGradlePluginVersion? {
            return agpVersion
        }
    }
}