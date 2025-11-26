/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.SwcConfig

internal abstract class KotlinSwcConfig {
    @get:Input
    abstract val esTarget: Property<String>

    @get:Input
    abstract val sourceMaps: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val moduleKind: Property<JsModuleKind>

    internal val moduleSystemToUse: Provider<ModuleKind> =
        moduleKind
            .orElse(esTarget.map { if (it == ES_2015) JsModuleKind.MODULE_ES else JsModuleKind.MODULE_UMD })
            .map { ModuleKind.fromType(it.kind) }

    fun toConfigMap() =
        SwcConfig.getConfigWhen(
            sourceMapEnabled = sourceMaps.get(),
            target = esTarget.get(),
            // To reduce the final code size, we always turn on this option
            includeExternalHelpers = true,
            moduleKind = moduleSystemToUse.get()
        )
}