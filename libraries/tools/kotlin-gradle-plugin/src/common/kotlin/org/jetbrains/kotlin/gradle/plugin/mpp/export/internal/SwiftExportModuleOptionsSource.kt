/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export.internal

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier

/**
 * A node of the resolved Swift Export graph.
 *
 * Holds both the selected [ComponentIdentifier] and the module coordinates it resolved to, because they can
 * differ: a `group:name` dependency substituted with a project selects a project component that still resolves
 * to `group:name`. Coordinates are matched through [moduleVersion], project paths through [id].
 */
internal data class SwiftExportResolvedComponent(
    val id: ComponentIdentifier,
    /** `null` when Gradle resolved the component without module coordinates. */
    val moduleVersion: ModuleVersionIdentifier?,
) {
    val displayName: String get() = id.displayName
}

/**
 * Options declared for a module, as opposed to derived from its coordinates or project path. A `null` property
 * means this layer does not declare it.
 */
internal data class SwiftExportDeclaredModuleOptions(
    val moduleName: String?,
    val rootPackage: String?,
)

/**
 * One layer of declared module options.
 *
 * Layers are consulted in order and the first non-null value wins, per property:
 *
 *  1. consumer overrides from `xcodeIntegration { configure(dependency) { } }` (KT-87990)
 *  2. options published by the dependency itself (KT-87987)
 *
 * The derived default applies when no layer declares a value.
 */
internal fun interface SwiftExportModuleOptionsSource {
    fun optionsFor(component: SwiftExportResolvedComponent): SwiftExportDeclaredModuleOptions?
}

/**
 * The declared module name for [component], or `null` to use the derived default.
 */
internal fun List<SwiftExportModuleOptionsSource>.declaredModuleName(component: SwiftExportResolvedComponent): String? =
    firstNotNullOfOrNull { it.optionsFor(component)?.moduleName }

/**
 * The declared root package for [component], or `null`.
 */
internal fun List<SwiftExportModuleOptionsSource>.declaredRootPackage(component: SwiftExportResolvedComponent): String? =
    firstNotNullOfOrNull { it.optionsFor(component)?.rootPackage }
