/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import javax.inject.Inject

const val EXPORT_EXTENSION_NAME = "export"

/**
 * An *experimental* plugin DSL extension to configure platform-specific export functionality.
 *
 * This extension is available inside the `kotlin {}` block in your build script:
 *
 * ```kotlin
 * kotlin {
 *     export {
 *         // Platform-specific export configuration
 *     }
 * }
 * ```
 *
 * Note that this DSL is experimental, and it will likely change in future versions until it is stable.
 *
 * @since 2.5.0
 */
/*
We can't mark top level extensions with @ExperimentalExportDsl because
in buildSrc Gradle always creates accessors for these extensions which cause the opt-in error,
which cannot be suppressed.

See Gradle issue https://github.com/gradle/gradle/issues/32019
 */
@KotlinGradlePluginDsl
abstract class ExportExtension @Inject constructor(
    objectFactory: ObjectFactory,
) {
    internal val swiftExportConfiguration: SwiftExportConfiguration = DefaultSwiftExportConfiguration(objectFactory)

    /**
     * Configure Swift Export.
     */
    @ExperimentalExportDsl
    fun swift(configure: SwiftExportConfigurationDsl.() -> Unit) {
        (swiftExportConfiguration as SwiftExportConfigurationDsl).configure()
    }

    /**
     * Configure Swift Export.
     */
    @ExperimentalExportDsl
    fun swift(configure: Action<SwiftExportConfigurationDsl>) = swift {
        configure.execute(this)
    }
}

/**
 * An *experimental* plugin DSL to configure Swift Export for an exported module.
 *
 * This DSL is available inside the `kotlin.export {}` block in your build script:
 *
 * ```kotlin
 * kotlin {
 *     export {
 *         swift {
 *             // Swift-specific export configuration
 *         }
 *     }
 * }
 * ```
 *
 * Note that this DSL is experimental, and it will likely change in future versions until it is stable.
 *
 * @since 2.5.0
 */
@ExperimentalExportDsl
interface SwiftExportConfigurationDsl {
    /**
     * Configure the name of this module that will be used in Swift Export.
     */
    val moduleName: Property<String>

    /**
     * Configure this module's root package. If provided, the root package will be used for package flattening in Swift Export.
     */
    val rootPackage: Property<String>
}

/**
 * Represents Swift Export configuration for an exported module.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalExportDsl
internal interface SwiftExportConfiguration {
    /**
     * The name of this module that will be used in Swift Export.
     */
    val moduleName: Property<String>

    /**
     * This module's root package.
     */
    val rootPackage: Property<String>
}

private class DefaultSwiftExportConfiguration(objectFactory: ObjectFactory) : SwiftExportConfiguration, SwiftExportConfigurationDsl {
    override val moduleName: Property<String> = objectFactory.property(String::class.java)
    override val rootPackage: Property<String> = objectFactory.property(String::class.java)
}

internal fun ObjectFactory.ExportExtension(): ExportExtension = newInstance(ExportExtension::class.java)
