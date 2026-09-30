/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.scripting.resolve.resolvedImportScripts
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.util.PropertiesCollection

private val appendableOverlayKeys: Set<PropertiesCollection.Key<*>> = setOf(
    ScriptCompilationConfiguration.defaultImports,
    ScriptCompilationConfiguration.dependencies,
    ScriptCompilationConfiguration.compilerOptions,
    ScriptCompilationConfiguration.implicitReceivers,
    ScriptCompilationConfiguration.importScripts,
    ScriptCompilationConfiguration.resolvedImportScripts,
)

/**
 * Applies [overlay] on top of the configuration being built.
 */
internal fun ScriptCompilationConfiguration.Builder.applyOverlay(overlay: ScriptCompilationConfiguration) {
    for (entry in overlay.entries()) {
        val key = entry.key
        val overlayValue = entry.value
        val baseValue = data[key]
        data[key] =
            if (key in appendableOverlayKeys && baseValue is List<*> && overlayValue is List<*>) {
                baseValue + overlayValue.filterNot { it in baseValue }
            } else {
                overlayValue
            }
    }
}
