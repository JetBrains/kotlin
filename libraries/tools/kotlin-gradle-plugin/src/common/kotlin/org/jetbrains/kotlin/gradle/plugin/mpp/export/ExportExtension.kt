/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import javax.inject.Inject

internal const val EXPORT_EXTENSION_NAME = "export"

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
    private val defaultSwiftExportConfiguration = DefaultSwiftExportConfiguration(objectFactory)

    internal val swiftExportConfiguration: SwiftExportConfiguration get() = defaultSwiftExportConfiguration

    /**
     * Whether the [swift] block was configured at least once in this project.
     */
    internal var isSwiftExportConfigured: Boolean = false
        private set

    /**
     * Configure Swift Export.
     */
    @ExperimentalExportDsl
    fun swift(configure: SwiftExportConfigurationDsl.() -> Unit) {
        isSwiftExportConfigured = true
        defaultSwiftExportConfiguration.configure()
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
@ExperimentalSwiftExportDsl
@KotlinGradlePluginDsl
interface SwiftExportConfigurationDsl {
    /**
     * Configure the name of this module that will be used in Swift Export.
     */
    val moduleName: Property<String>

    /**
     * Configure this module's root package. If provided, the root package will be used for package flattening in Swift Export.
     */
    val rootPackage: Property<String>

    /**
     * Activate the Xcode integration for this module.
     *
     * Repeated calls configure the same integration, so the order of the calls doesn't matter.
     * 
     * The integration is only activated in the projects where this function is called.
     */
    fun xcodeIntegration()

    /**
     * Activate and configure the Xcode integration for this module.
     *
     * Repeated calls configure the same integration, so the order of the calls doesn't matter.
     *
     * The integration is only activated in the projects where this function is called.
     */
    fun xcodeIntegration(configure: SwiftExportXcodeIntegration.() -> Unit)

    /**
     * Activate and configure the Xcode integration for this module.
     *
     * Repeated calls configure the same integration, so the order of the calls doesn't matter.
     *
     * The integration is only activated in the projects where this function is called.
     */
    fun xcodeIntegration(configure: Action<SwiftExportXcodeIntegration>)
}

/**
 * Represents Swift Export configuration for an exported module.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalSwiftExportDsl
internal interface SwiftExportConfiguration {
    /**
     * The name of this module that will be used in Swift Export.
     */
    val moduleName: Property<String>

    /**
     * This module's root package.
     */
    val rootPackage: Property<String>

    /**
     * The Xcode integration activated via [SwiftExportConfigurationDsl.xcodeIntegration],
     * or `null` if it was never activated for this module.
     */
    val activatedXcodeIntegration: SwiftExportXcodeIntegrationConfiguration?
}

/**
 * Represents the Xcode integration activated for an exported module.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalSwiftExportDsl
internal interface SwiftExportXcodeIntegrationConfiguration {
    /**
     * The settings passed to Swift Export for this module.
     */
    val settings: Provider<Map<String, String>>
}

/**
 * Represents Swift Export integration for consumers.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalSwiftExportDsl
@KotlinGradlePluginDsl
interface SwiftExportIntegration {
    /**
     * Configure the settings passed to Swift Export for this module.
     */
    val settings: MapProperty<String, String>
}

/**
 * Represents Swift Export integration for Xcode consumers.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalSwiftExportDsl
@KotlinGradlePluginDsl
interface SwiftExportXcodeIntegration : SwiftExportIntegration

private class DefaultSwiftExportConfiguration(
    private val objectFactory: ObjectFactory,
) : SwiftExportConfiguration, SwiftExportConfigurationDsl {
    override val moduleName: Property<String> = objectFactory.property(String::class.java)
    override val rootPackage: Property<String> = objectFactory.property(String::class.java)

    private var xcodeIntegrationConfiguration: DefaultSwiftExportXcodeIntegration? = null

    override val activatedXcodeIntegration: SwiftExportXcodeIntegrationConfiguration?
        get() = xcodeIntegrationConfiguration

    override fun xcodeIntegration() = xcodeIntegration { }

    override fun xcodeIntegration(configure: SwiftExportXcodeIntegration.() -> Unit) {
        val integration = xcodeIntegrationConfiguration
            ?: DefaultSwiftExportXcodeIntegration(objectFactory).also { xcodeIntegrationConfiguration = it }
        integration.configure()
    }

    override fun xcodeIntegration(configure: Action<SwiftExportXcodeIntegration>) = xcodeIntegration {
        configure.execute(this)
    }
}

private class DefaultSwiftExportXcodeIntegration(
    objectFactory: ObjectFactory,
) : SwiftExportXcodeIntegration, SwiftExportXcodeIntegrationConfiguration {
    override val settings: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
}

internal fun ObjectFactory.ExportExtension(): ExportExtension = newInstance(ExportExtension::class.java)
