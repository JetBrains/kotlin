/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedDependency

/**
 * A common interface for [SwiftExportConfiguration] and [org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension]
 * used during the migration from the latter to the former. Will be removed once the legacy
 * [org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension] is fully deprecated.
 */
internal interface SwiftExportConfigurationCompat {
    /**
     * The name of this module that will be used in Swift Export.
     */
    val moduleName: Property<String>

    /**
     * This module's root package.
     */
    val rootPackage: Property<String>

    /**
     * Returns a list of exported modules.
     */
    val exportedModules: Provider<Set<SwiftExportedDependency>>

    /**
     * Configure SwiftExportConfig.settings parameters
     */
    val settings: MapProperty<String, String>

    /**
     * Specifies additional compiler arguments to be passed to the compiler.
     */
    val freeCompilerArgs: ListProperty<String>

    fun addBinary(binary: AbstractNativeLibrary)

    companion object {
        fun from(
            configuration: SwiftExportConfiguration,
            providers: ProviderFactory,
            objects: ObjectFactory,
        ): SwiftExportConfigurationCompat =
            object : SwiftExportConfigurationCompat {
                override val moduleName: Property<String> get() = configuration.moduleName
                override val rootPackage: Property<String> get() = configuration.rootPackage
                override val exportedModules: Provider<Set<SwiftExportedDependency>>
                    get() = providers.provider { emptySet() } // TODO: KT-85687
                override val settings: MapProperty<String, String>
                    get() = objects.mapProperty(String::class.java, String::class.java) // TODO: KT-87890
                override val freeCompilerArgs: ListProperty<String>
                    get() = objects.listProperty(String::class.java) // TODO: KT-87890

                override fun addBinary(binary: AbstractNativeLibrary) {
                    // TODO: KT-87890
                }
            }

        fun from(extension: SwiftExportExtension): SwiftExportConfigurationCompat =
            object : SwiftExportConfigurationCompat {
                override val moduleName: Property<String> get() = extension.moduleName
                override val rootPackage: Property<String> get() = extension.flattenPackage
                override val exportedModules: Provider<Set<SwiftExportedDependency>> get() = extension.exportedModules
                override val settings: MapProperty<String, String> get() = extension.advancedConfiguration.settings
                override val freeCompilerArgs: ListProperty<String> get() = extension.advancedConfiguration.freeCompilerArgs
                override fun addBinary(binary: AbstractNativeLibrary) {
                    extension.addBinary(binary)
                }
            }
    }
}
