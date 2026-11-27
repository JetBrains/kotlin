/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(
    ExperimentalNpmDependencyManagementDsl::class,
    DelicateKotlinGradlePluginApi::class,
    ExperimentalNodeJsToolchainDsl::class,
)

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalNpmDependencyManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsVersion
import org.jetbrains.kotlin.gradle.ecosystem.internal.KotlinEcosystemExtensionImpl
import org.jetbrains.kotlin.gradle.ecosystem.npm.*

abstract class KotlinEcosystemPlugin internal constructor() : Plugin<Settings> {

    override fun apply(settings: Settings) {
        val kotlin = settings.extensions.create(
            KotlinEcosystemExtension::class.java,
            "kotlin",
            KotlinEcosystemExtensionImpl::class.java,
            settings,
        ) as KotlinEcosystemExtensionImpl

        setNpmDependencyManagementConventions(settings, kotlin.js.npmDependencyManagement)
        setNpmDependencyManagementConventions(settings, kotlin.wasmJs.npmDependencyManagement)
    }

    private fun setNpmDependencyManagementConventions(
        settings: Settings,
        npmDeps: NpmDependencyManagementDsl,
    ) {
        npmDeps.ignoreScripts.convention(true)
        npmDeps.sharedNpmDependenciesMode.convention(SharedNpmDependenciesMode.SHARED_IN_ROOT_PROJECT)
        npmDeps.lockFile.directory.convention(
            when (npmDeps) {
                is JsNpmDependencyManagementDsl -> settings.layout.settingsDirectory.dir("kotlin-js-store")
                is WasmJsNpmDependencyManagementDsl -> settings.layout.settingsDirectory.dir("kotlin-js-store/wasm")
                else -> error("Unexpected ${npmDeps::class} instance")
            }
        )
        npmDeps.lockFile.mismatchReport.convention(NpmLockFileMismatchReport.FAIL)
        npmDeps.lockFile.reportNewLockFile.convention(false)
        npmDeps.lockFile.autoReplace.convention(false)
        npmDeps.nodeJsVersionForDependencyResolution.convention(
            settings.providers.gradleProperty("kotlin.defaultNodeJsVersion")
                .orElse("24.22.1")
                .map(::NodeJsVersion)
        )
    }
}
