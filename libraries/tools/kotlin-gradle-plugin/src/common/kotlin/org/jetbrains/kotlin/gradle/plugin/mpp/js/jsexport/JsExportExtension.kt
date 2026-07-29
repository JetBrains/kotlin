/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER") // Ignore not-yet-used API added for PoC.

package org.jetbrains.kotlin.gradle.plugin.mpp.js.jsexport

import org.gradle.api.provider.Property

/**
 * An *experimental* plugin DSL extension to configure JS Export.
 */
interface JsExportedModuleMetadata {
    /**
     * Configure name of the JS export module from this project.
     */
    val npmPackageName: Property<String>

    /**
     * Specify module's root package to configure package collapsing rule.
     */
    val rootPackage: Property<String>
}

abstract class JsExportExtension : JsExportedModuleMetadata {
    /**
     * Configure the module metadata of a module already added to JS export.
     */
    fun component(dependency: Any, configure: JsExportedModuleMetadata.() -> Unit = {}) {
    }
}
