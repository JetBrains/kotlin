/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.KotlinWasmtimeSubtarget
import org.jetbrains.kotlin.gradle.targets.wasm.WasmtimeEnvironmentConfigurator
import org.jetbrains.kotlin.gradle.targets.wasm.dsl.KotlinWasmtimeDsl
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal fun ObjectFactory.KotlinWasmTarget(
    project: Project,
    platformType: KotlinPlatformType,
): KotlinWasmTarget = newInstance(project, platformType)

abstract class KotlinWasmTarget
@Inject
internal constructor(
    project: Project,
    platformType: KotlinPlatformType,
) :
    KotlinJsIrTarget(
        project,
        platformType
    ),
    KotlinWasmJsTargetDsl,
    KotlinWasmWasiTargetDsl,
    KotlinWasmSubTargetContainerDsl {
    // Specify if webpack should be used as a bundler.
    // It is captured on the first access to [browserLazyDelegate] and can't be changed afterwards,
    // because the corresponding configurator registers its tasks during configuration.
    private var bundler: KotlinBrowserBundler? = null

    override fun KotlinBrowserJsIr.bundleConfigurator() {
        val bundlerValue = bundler ?: error("Bundler should be defined in ${this@KotlinWasmTarget.name}")
        when (bundlerValue) {
            KotlinBrowserBundler.WEBPACK -> {
                subTargetConfigurators.add(WebpackConfigurator(this))
            }
            KotlinBrowserBundler.NONE -> {
                subTargetConfigurators.add(NoBundleConfigurator(this))
            }
        }
    }

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        bundler = KotlinBrowserBundler.WEBPACK
        browser.body()
    }

    override fun browser(bundler: KotlinBrowserBundler, body: KotlinWasmJsBrowserDsl.() -> Unit) {
        if (this@KotlinWasmTarget.bundler == null) {
            this@KotlinWasmTarget.bundler = bundler
        } else if (this@KotlinWasmTarget.bundler != bundler) {
            error("")
        }
        (browser as KotlinBrowserJsIr).body()
    }

    //region d8
    @OptIn(ExperimentalWasmDsl::class)
    private val d8LazyDelegate = lazy {
        webTargetVariant(
            { NodeJsRootPlugin.apply(project.rootProject) },
            { WasmNodeJsRootPlugin.apply(project.rootProject) },
        )

        addSubTarget(KotlinD8Ir::class.java) {
            configureSubTarget()
            subTargetConfigurators.add(LibraryConfigurator(this))
            subTargetConfigurators.add(D8EnvironmentConfigurator(this))
        }
    }

    override val d8: KotlinWasmD8Dsl by d8LazyDelegate

    override fun d8(body: KotlinWasmD8Dsl.() -> Unit) {
        body(d8)
    }
    //endregion

    //region wasmtime
    @OptIn(ExperimentalWasmDsl::class)
    private val wasmtimeLazyDelegate = lazy {
        check(wasmTargetType == KotlinWasmTargetType.WASI) {
            "Wasmtime execution environment is supported only for the Kotlin/Wasm WASI target."
        }

        addSubTarget(KotlinWasmtimeSubtarget::class.java) {
            configureSubTarget()
            subTargetConfigurators.add(LibraryConfigurator(this))
            subTargetConfigurators.add(WasmtimeEnvironmentConfigurator(this))
        }
    }

    @ExperimentalWasmDsl
    private val wasmtime: KotlinWasmtimeDsl by wasmtimeLazyDelegate

    @ExperimentalWasmDsl
    override fun wasmtime(body: KotlinWasmtimeDsl.() -> Unit) {
        body(wasmtime)
    }
    //endregion
}
