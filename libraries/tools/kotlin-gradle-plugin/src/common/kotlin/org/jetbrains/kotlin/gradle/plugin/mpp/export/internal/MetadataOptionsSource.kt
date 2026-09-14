/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export.internal

import org.gradle.api.artifacts.component.ComponentIdentifier

/**
 * The second-highest precedence layer (after [ConsumerOverridesOptionsSource]): overrides coming from Gradle metadata.
 */
internal class MetadataOptionsSource(
    private val metadataByComponent: Map<ComponentIdentifier, SwiftExportMetadata>,
) : SwiftExportModuleOptionsSource {
    override fun optionsFor(component: SwiftExportResolvedComponent): SwiftExportDeclaredModuleOptions? {
        return metadataByComponent[component.rootComponentId]?.let { metadata ->
            SwiftExportDeclaredModuleOptions(
                moduleName = metadata.moduleName,
                rootPackage = metadata.rootPackage,
            )
        }
    }
}
