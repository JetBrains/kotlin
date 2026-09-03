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

    override fun KotlinBrowserJsIr.bundleConfigurator() {
        val bundlerValue: KotlinBrowserBundler = bundler.get()
        when(bundlerValue) {
            KotlinBrowserBundler.WEBPACK -> {
                subTargetConfigurators.add(WebpackConfigurator(this))
            }
            KotlinBrowserBundler.NONE -> {
                subTargetConfigurators.add(NoBundleConfigurator(this))
            }
        }
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
