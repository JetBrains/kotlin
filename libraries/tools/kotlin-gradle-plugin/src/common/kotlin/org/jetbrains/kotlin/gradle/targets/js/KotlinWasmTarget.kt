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
    private var useWebpack: Boolean? = null

    override fun KotlinBrowserJsIr.bundleConfigurator() {
        if (useWebpack!!) {
            subTargetConfigurators.add(WebpackConfigurator(this))
        } else {
            subTargetConfigurators.add(NoBundleConfigurator(this))
        }
    }

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        useWebpack = true
        browser.body()
    }

    override fun browser(useWebpack: Boolean, body: KotlinWasmJsBrowserDsl.() -> Unit) {
        if (this@KotlinWasmTarget.useWebpack == null) {
            this@KotlinWasmTarget.useWebpack = useWebpack
        } else if (this@KotlinWasmTarget.useWebpack != useWebpack) {
            project.logger.warn(
                "w: Kotlin browser target '$targetName' is already configured with bundler '${this@KotlinWasmTarget.useWebpack}'; " +
                        "the request to use '$useWebpack' will be ignored. " +
                        "The bundler must be specified on the first 'browser { }' call."
            )
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
}
