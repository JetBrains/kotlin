/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinOnlyTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.tasks.dependsOn

open class KotlinJsIrTargetConfigurator :
    KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget>(true) {

    override fun configureTarget(target: KotlinJsIrTarget) {
        super.configureTarget(target)

        val assemble = target.project.tasks.named(ASSEMBLE_TASK_NAME)

        target.compilations.all { compilation ->
            if (compilation.isMain()) {
                compilation.binaries
                    .matching { it.mode == KotlinJsBinaryMode.PRODUCTION }
                    .all {
                        when (target.wasmTargetType) {
                            KotlinWasmTargetType.WASI -> assemble.dependsOn((it as WasmBinary).optimizeTask)
                            KotlinWasmTargetType.JS, null -> assemble.dependsOn(it.linkSyncTask)
                        }
                    }
            }
        }
    }

    internal companion object {
        internal fun KotlinJsCompilerOptions.configureJsDefaultOptions() {
            sourceMap.convention(true)
            sourceMapEmbedSources.convention(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_NEVER)
        }
    }
}
