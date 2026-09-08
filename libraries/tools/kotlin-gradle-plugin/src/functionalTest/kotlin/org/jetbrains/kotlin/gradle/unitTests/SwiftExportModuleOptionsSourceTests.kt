/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDeclaredModuleOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportModuleOptionsSource
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportResolvedComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.declaredModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.declaredRootPackage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The seam only reads what a layer returns for a component, so opaque identifiers are enough. Matching real
 * identifiers is covered in [ExportExtensionSwiftExportTests].
 */
class SwiftExportModuleOptionsSourceTests {

    private val component = SwiftExportResolvedComponent(ComponentIdentifier { "test-component" }, moduleVersion = null)

    private fun layer(moduleName: String?, rootPackage: String?) =
        SwiftExportModuleOptionsSource { SwiftExportDeclaredModuleOptions(moduleName, rootPackage) }

    @Test
    fun `the highest precedence layer wins for module name`() {
        val layers = listOf(
            layer(moduleName = "Higher", rootPackage = null),
            layer(moduleName = "Lower", rootPackage = null),
        )

        assertEquals("Higher", layers.declaredModuleName(component))
    }

    @Test
    fun `properties resolve independently of each other`() {
        val layers = listOf(
            layer(moduleName = "Higher", rootPackage = null),
            layer(moduleName = "Lower", rootPackage = "org.example.lower"),
        )

        assertEquals("Higher", layers.declaredModuleName(component))
        assertEquals("org.example.lower", layers.declaredRootPackage(component))
    }

    @Test
    fun `a layer with no metadata for the component is skipped`() {
        val layers = listOf(
            SwiftExportModuleOptionsSource { null },
            layer(moduleName = "Lower", rootPackage = "org.example.lower"),
        )

        assertEquals("Lower", layers.declaredModuleName(component))
        assertEquals("org.example.lower", layers.declaredRootPackage(component))
    }

    @Test
    fun `nothing declared falls through to the derived default`() {
        val layers = listOf(
            layer(moduleName = null, rootPackage = null),
            SwiftExportModuleOptionsSource { null },
        )

        assertNull(layers.declaredModuleName(component))
        assertNull(layers.declaredRootPackage(component))
    }

    @Test
    fun `no layers at all falls through to the derived default`() {
        val layers = emptyList<SwiftExportModuleOptionsSource>()

        assertNull(layers.declaredModuleName(component))
        assertNull(layers.declaredRootPackage(component))
    }
}
