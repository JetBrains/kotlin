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
import org.gradle.api.provider.ProviderConvertible
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDeclaredModuleOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDependencySelector
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDependencySelectorFactory
import org.jetbrains.kotlin.gradle.utils.newInstance
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
abstract class ExportExtension @Inject internal constructor(
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory,
    dependencySelectorFactory: SwiftExportDependencySelectorFactory,
) {
    private val defaultSwiftExportConfiguration = DefaultSwiftExportConfiguration(
        objectFactory = objectFactory,
        providerFactory = providerFactory,
        dependencySelectorFactory = dependencySelectorFactory,
    )

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
interface SwiftExportConfigurationDsl : SwiftExportModuleOptionsDsl {
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
 * Swift Export options of a module: the exported module itself, or one of its dependencies.
 *
 * Shared by [SwiftExportConfigurationDsl] and [SwiftExportIntegration.configure].
 *
 * This API is experimental and may change in future versions.
 *
 * @since 2.5.0
 */
@ExperimentalSwiftExportDsl
@KotlinGradlePluginDsl
interface SwiftExportModuleOptionsDsl {
    /**
     * The Swift module name, used as is, so it must be a valid Swift module name.
     *
     * For a dependency, it takes precedence over the derived name and over the name the dependency publishes.
     */
    val moduleName: Property<String>

    /**
     * The root package to flatten.
     *
     * For a dependency, it takes precedence over the root package the dependency publishes. Ignored for
     * transitively exported dependencies.
     */
    val rootPackage: Property<String>
}

/**
 * Represents Swift Export configuration for an exported module.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalSwiftExportDsl
internal interface SwiftExportConfiguration : SwiftExportModuleOptionsDsl {
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

    /**
     * Overrides from [SwiftExportIntegration.configure], keyed by the dependency they select. A later call wins,
     * per property.
     */
    val dependencyOverrides: Provider<Map<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>>
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

    /**
     * Override the Swift Export options of [dependency].
     *
     * [dependency] takes what Gradle's dependency handler takes: `"group:name:version"`, `project(":path")`,
     * a [org.gradle.api.Project], a version catalog accessor like `libs.foo`, or a [Provider] of any of those.
     * The version is ignored; the override is matched against the resolved component.
     *
     * The dependency must already be in the Swift Export graph, this function does not add it. An override
     * that matches nothing is reported as an error.
     *
     * A notation that is not a valid dependency (coordinates without a group, for example) fails right here.
     * A [Provider] is only realized when the graph is assembled, so an error inside one shows up then.
     *
     * Repeated calls follow normal Gradle property semantics: the last assignment wins, per property.
     *
     * Overrides are local to this consumer and are not published.
     *
     * @since 2.5.0
     */
    fun configure(dependency: Any, configure: SwiftExportModuleOptionsDsl.() -> Unit)

    /**
     * Override the Swift Export options of [dependency].
     *
     * @see configure
     * @since 2.5.0
     */
    fun configure(dependency: Any, configure: Action<SwiftExportModuleOptionsDsl>)
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
    private val providerFactory: ProviderFactory,
    private val dependencySelectorFactory: SwiftExportDependencySelectorFactory,
) : SwiftExportConfiguration, SwiftExportConfigurationDsl {
    override val moduleName: Property<String> = objectFactory.property(String::class.java)
    override val rootPackage: Property<String> = objectFactory.property(String::class.java)

    private var xcodeIntegrationConfiguration: DefaultSwiftExportXcodeIntegration? = null

    override val activatedXcodeIntegration: SwiftExportXcodeIntegrationConfiguration?
        get() = xcodeIntegrationConfiguration

    override fun xcodeIntegration() = xcodeIntegration { }

    override fun xcodeIntegration(configure: SwiftExportXcodeIntegration.() -> Unit) {
        val integration = xcodeIntegrationConfiguration
            ?: DefaultSwiftExportXcodeIntegration(
                objectFactory = objectFactory,
                providerFactory = providerFactory,
                dependencySelectorFactory = dependencySelectorFactory,
            ).also { xcodeIntegrationConfiguration = it }
        integration.configure()
    }

    override fun xcodeIntegration(configure: Action<SwiftExportXcodeIntegration>) = xcodeIntegration {
        configure.execute(this)
    }
}

private class DefaultSwiftExportXcodeIntegration(
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
    private val dependencySelectorFactory: SwiftExportDependencySelectorFactory,
) : SwiftExportXcodeIntegration, SwiftExportXcodeIntegrationConfiguration {

    override val settings: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)

    /** One `configure(dependency) { }` call each, in declaration order, so that a later call wins. */
    private val pendingOverrides = mutableListOf<Pair<Provider<SwiftExportDependencySelector>, SwiftExportModuleOptionsDsl>>()

    override fun configure(dependency: Any, configure: SwiftExportModuleOptionsDsl.() -> Unit) {
        val dsl = objectFactory.newInstance<SwiftExportModuleOptionsDsl>()
        dsl.configure()
        pendingOverrides += dependency.selectorProvider() to dsl
    }

    override fun configure(dependency: Any, configure: Action<SwiftExportModuleOptionsDsl>) =
        configure(dependency) { configure.execute(this) }

    override val dependencyOverrides: Provider<Map<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>> =
        providerFactory.provider {
            val overrides = LinkedHashMap<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>()
            for ((selectorProvider, dsl) in pendingOverrides) {
                val selector = selectorProvider.get()
                val previous = overrides[selector]
                overrides[selector] = SwiftExportDeclaredModuleOptions(
                    moduleName = dsl.moduleName.orNull ?: previous?.moduleName,
                    rootPackage = dsl.rootPackage.orNull ?: previous?.rootPackage,
                )
            }
            overrides
        }

    /**
     * A [Provider] notation can't be turned into a selector until it is realized, and realizing it during
     * configuration would break laziness. Other notations are converted right away so that a bad one fails at
     * the call site.
     */
    private fun Any.selectorProvider(): Provider<SwiftExportDependencySelector> = when (this) {
        is Provider<*> -> map { resolved -> dependencySelectorFactory.fromNotation(resolved) }
        // A version catalog alias that has nested aliases (`libs.foo` next to `libs.foo.core`) is generated as a
        // ProviderConvertible rather than a Provider.
        is ProviderConvertible<*> -> asProvider().map { resolved -> dependencySelectorFactory.fromNotation(resolved) }
        else -> {
            val selector = dependencySelectorFactory.fromNotation(this)
            providerFactory.provider { selector }
        }
    }
}

internal fun ObjectFactory.ExportExtension(
    dependencySelectorFactory: SwiftExportDependencySelectorFactory,
): ExportExtension = newInstance(ExportExtension::class.java, dependencySelectorFactory)
