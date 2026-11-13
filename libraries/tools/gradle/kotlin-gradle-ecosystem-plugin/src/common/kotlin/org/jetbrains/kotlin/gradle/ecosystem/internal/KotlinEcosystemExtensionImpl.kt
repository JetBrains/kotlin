/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(
    InternalKotlinGradlePluginApi::class,
    ExperimentalNodeJsToolchainDsl::class,
    ExperimentalKotlinGradlePluginApi::class,
    ExperimentalWasmDsl::class,
)

package org.jetbrains.kotlin.gradle.ecosystem.internal

import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeSettingsDsl
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.ecosystem.KotlinToolchainManagementDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain.WasiAdapterToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain.WasmToolsToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainManagementDslImpl
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeSettingsDslImpl
import javax.inject.Inject

internal open class KotlinEcosystemExtensionImpl @Inject constructor(
    settings: Settings,
    objects: ObjectFactory,
) : KotlinEcosystemExtension {

    private val nativeDsl: KotlinNativeSettingsDsl = KotlinNativeSettingsDslImpl(objects, settings.providers, settings.gradle)

    override fun native(configure: KotlinNativeSettingsDsl.() -> Unit) {
        nativeDsl.configure()
    }

    private val toolchainManagementDsl = KotlinToolchainManagementDslImpl(
        NodeJsToolchainManagementDslImpl(settings.gradle),
        WasiAdapterToolchainManagementDslImpl(settings.gradle),
        WasmToolsToolchainManagementDslImpl(settings.gradle),
    )

    override fun toolchainManagement(configure: KotlinToolchainManagementDsl.() -> Unit) {
        toolchainManagementDsl.configure()
    }
}
