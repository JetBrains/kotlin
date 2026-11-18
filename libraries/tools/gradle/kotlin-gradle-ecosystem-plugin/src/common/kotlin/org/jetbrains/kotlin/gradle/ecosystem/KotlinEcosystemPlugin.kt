/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(
    InternalKotlinGradlePluginApi::class,
    DelicateKotlinGradlePluginApi::class,
    ExperimentalKotlinGradlePluginApi::class,
)

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ecosystem.internal.KotlinEcosystemExtensionImpl
import org.jetbrains.kotlin.gradle.ecosystem.internal.isRootProject
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.isKotlinSharedNpmProjectEnabled
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.registerKotlinSharedNpmProjectsRegistry

abstract class KotlinEcosystemPlugin internal constructor() : Plugin<Settings> {

    override fun apply(settings: Settings) {
        settings.extensions.create(
            KotlinEcosystemExtension::class.java,
            "kotlin",
            KotlinEcosystemExtensionImpl::class.java,
            settings,
        )

        setupKotlinSharedNpmProject(settings)
    }

    private fun setupKotlinSharedNpmProject(settings: Settings) {
        if (!isKotlinSharedNpmProjectEnabled(settings.providers)) {
            // TODO KT-91658 Replace `isKotlinSharedNpmProjectEnabled` opt-in with the settings DSL's `sharedNpmDependenciesMode`.
            //      Register the registry and apply the plugin if JS or WasmJS is enabled, pass the enabled platforms to the registry,
            //      and have `KotlinSharedNpmProjectPlugin` only collect registered projects for enabled platforms.
            return
        }

        registerKotlinSharedNpmProjectsRegistry(settings.gradle)
        settings.gradle.lifecycle.beforeProject { project ->
            if (project.isRootProject()) {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
            }
        }
    }
}
