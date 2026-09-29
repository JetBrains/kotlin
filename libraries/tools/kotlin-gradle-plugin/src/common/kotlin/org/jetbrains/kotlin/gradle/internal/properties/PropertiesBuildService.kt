/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.properties

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull
import org.jetbrains.kotlin.gradle.utils.localProperties
import org.jetbrains.kotlin.gradle.utils.mapOrNull
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * [BuildService] that looks up properties in the following precedence order:
 *   1. Project's extra properties ([org.gradle.api.plugins.ExtensionContainer.getExtraProperties])
 *   2. Project's Gradle properties ([org.gradle.api.provider.ProviderFactory.gradleProperty])
 *   3. Root project's `local.properties` file ([Project.localProperties])
 *
 *  Note that extra and Gradle properties may differ across projects, whereas `local.properties` is shared across all projects.
 */
internal abstract class PropertiesBuildService @Inject constructor(
    private val providerFactory: ProviderFactory
) : BuildService<PropertiesBuildService.Params> {

    interface Params : BuildServiceParameters {
        val localProperties: MapProperty<String, String>

        /**
         * Snapshot of the values of every `kotlin.*` Gradle property known to the
         * Kotlin Gradle Plugin (see [PropertiesProvider.PropertyNames.allProperties]),
         * resolved once at BuildService registration time.
         *
         * Reading these values through this map (a BuildService parameter) makes the
         * configuration-cache fingerprint inputs build-scoped instead of project-scoped,
         * which is critical with Isolated Projects: in a build with N projects applying
         * the Kotlin plugin, this turns ~M*N project-scope property reads into ~M
         * build-scope reads (where M is the number of `kotlin.*` properties).
         *
         * Only entries with an actual value are present; absence here means the
         * property was not set as a Gradle property (the lookup will fall back to
         * project extra properties and `local.properties`).
         */
        val gradleProperties: MapProperty<String, String>

        /**
         * The set of property names that were pre-scanned into [gradleProperties].
         * Used to distinguish "known property, absent value" (use the snapshot) from
         * "unknown property" (fall back to a per-project `gradleProperty()` lookup).
         */
        val knownPropertyNames: SetProperty<String>
    }

    /**
     * Key should be `project.path/propertyName`.
     */
    private val propertiesPerProject = ConcurrentHashMap<String, Provider<String>>()

    private val localProperties by lazy { parameters.localProperties.get() }
    private val gradleProperties by lazy { parameters.gradleProperties.get() }
    private val knownPropertyNames by lazy { parameters.knownPropertyNames.get() }
    private val logger = Logging.getLogger(this::class.java)

    /**
     * Returns a [Provider] of the value of the property with the given [propertyName] either from project [extraPropertiesExtension],
     * or from configured project properties or from root project `local.properties` file.
     */
    fun property(
        propertyName: String,
        projectPath: String,
        extraPropertiesExtension: ExtraPropertiesExtension,
    ): Provider<String> {
        // Note: The same property may be read many times (KT-62496).
        // Therefore,
        //   - Use a map to create only one Provider per property.
        //   - Use MemoizedCallable to resolve the Provider only once.
        return propertiesPerProject.computeIfAbsent("$projectPath/$propertyName") {
            // We need to create the MemoizedCallable instance up front so that each time the Provider is resolved, it will reuse the same
            // MemoizedCallable.
            val valueFromGradleAndLocalProperties = MemoizedCallable {
                extraPropertiesExtension.getOrNull(propertyName)?.toString()
                    ?: gradlePropertyValue(propertyName)
                    ?: localProperties[propertyName]
            }
            providerFactory.provider { valueFromGradleAndLocalProperties.call() }
        }
    }

    /**
     * Look up [propertyName] as a Gradle property, going through the build-scoped
     * snapshot in [Params.gradleProperties] when possible to avoid registering a
     * project-scope configuration-cache fingerprint input on every project that
     * applies the Kotlin plugin (see [Params.gradleProperties] for context).
     *
     * Falls back to [providerFactory] for property names that were not part of the
     * pre-scan (e.g. dynamic `kotlin.native.binary.*` keys).
     */
    private fun gradlePropertyValue(propertyName: String): String? {
        return if (knownPropertyNames.contains(propertyName)) {
            gradleProperties[propertyName]
        } else {
            providerFactory.gradleProperty(propertyName).orNull
        }
    }

    /**
     * Returns a [Provider] of the value of the property with the given [propertyName] either from project extra properties,
     * or from configured project properties or from root project `local.properties` file.
     */
    fun property(
        propertyName: String,
        project: Project,
    ) = property(propertyName, project.path, project.extraProperties)

    fun <T : Any?, PROP : GradleProperty<T>> property(
        property: PROP,
        project: Project
    ): Provider<T> {
        return property(property.name, project.path, project.extraProperties)
            .mapOrNull(providerFactory) {
                val result = when (property) {
                    is BooleanGradleProperty -> property.toBooleanFromString(it)
                    is NullableBooleanGradleProperty -> property.toNullableBooleanFromString(it)
                    is StringGradleProperty, is NullableStringGradleProperty -> it
                    is IntGradleProperty -> property.toIntFromString(it)
                    else -> throw IllegalStateException("Unknown Gradle property type $property")
                }

                @Suppress("UNCHECKED_CAST")
                result as T?
            }
            .run {
                val propDefaultValue = property.defaultValue
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                if (propDefaultValue != null) orElse(propDefaultValue) else this
            }
    }

    private fun BooleanGradleProperty.toBooleanFromString(
        value: String?
    ): Boolean = parseBoolean(value, defaultValue, name)

    private fun NullableBooleanGradleProperty.toNullableBooleanFromString(
        value: String?
    ): Boolean? = parseBoolean(value, defaultValue, name)

    private fun <T : Boolean?> parseBoolean(
        value: String?,
        defaultValue: T,
        propName: String,
    ): T = when {
        value.equals("true", ignoreCase = true) -> {
            @Suppress("UNCHECKED_CAST")
            true as T
        }
        value.equals("false", ignoreCase = true) -> {
            @Suppress("UNCHECKED_CAST")
            false as T
        }
        else -> {
            warnInvalidPropertyValue("Boolean", propName, value, defaultValue)
            defaultValue
        }
    }

    private fun IntGradleProperty.toIntFromString(value: String?): Int {
        return value?.toIntOrNull() ?: run {
            warnInvalidPropertyValue("Int", name, value, defaultValue)
            defaultValue
        }
    }

    private fun warnInvalidPropertyValue(
        propertyType: String,
        name: String,
        value: String?,
        defaultValue: Any?,
    ) = logger.warn(
        "$propertyType option '$name' was set to an invalid value: `$value`." +
                " Using default value '$defaultValue' instead."
    )

    /** Returns the value of the property with the given [propertyName] in the given [project]. */
    fun get(propertyName: String, project: Project): String? {
        return property(propertyName, project).orNull
    }

    companion object {

        fun registerIfAbsent(project: Project): Provider<PropertiesBuildService> =
            project.gradle.registerClassLoaderScopedBuildService(PropertiesBuildService::class) {
                it.parameters.localProperties.set(project.localProperties)

                // Pre-scan every kotlin.* Gradle property the plugin knows about and route
                // the reads through a BuildService parameter rather than a per-project
                // providerFactory call. This makes the configuration-cache fingerprint input
                // build-scoped instead of project-scoped, so the cost is paid once per build
                // instead of once per project that applies the Kotlin plugin. With Isolated
                // Projects this is the difference between ~30 and ~30 * (#kotlin-projects)
                // CC inputs.
                //
                // Resolve each provider eagerly here so we can put plain `String` values
                // into the MapProperty: putting an absent-value `Provider` would make the
                // whole MapProperty "missing" (see https://github.com/gradle/gradle/issues/13364
                // and the MapProperty.put(K, Provider) javadoc), which would break every
                // build that doesn't explicitly set every known kotlin.* property.
                val knownNames = PropertiesProvider.PropertyNames.allProperties()
                it.parameters.knownPropertyNames.set(knownNames)
                it.parameters.gradleProperties.set(
                    knownNames.mapNotNull { name ->
                        project.providers.gradleProperty(name).orNull?.let { value -> name to value }
                    }.toMap()
                )
            }
    }

    private class MemoizedCallable<T>(valueResolver: Callable<T>) : Callable<T> {
        private val value: T? by lazy { valueResolver.call() }
        override fun call(): T? = value
    }

    internal sealed interface GradleProperty<T : Any?> {
        val name: String
        val defaultValue: T
    }

    internal class BooleanGradleProperty(
        override val name: String,
        override val defaultValue: Boolean
    ) : GradleProperty<Boolean>

    internal class NullableBooleanGradleProperty(
        override val name: String,
    ) : GradleProperty<Boolean?> {
        override val defaultValue: Boolean? = null
    }

    internal class StringGradleProperty(
        override val name: String,
        override val defaultValue: String
    ) : GradleProperty<String>

    internal class NullableStringGradleProperty(
        override val name: String,
    ) : GradleProperty<String?> {
        override val defaultValue: String? = null
    }

    internal class IntGradleProperty(
        override val name: String,
        override val defaultValue: Int
    ) : GradleProperty<Int>
}

internal val Project.propertiesService: Provider<PropertiesBuildService>
    get() = PropertiesBuildService.registerIfAbsent(this)

internal fun <T> PropertiesBuildService.propertyWithDeprecatedName(
    nonDeprecatedProperty: PropertiesBuildService.GradleProperty<T>,
    deprecatedProperty: PropertiesBuildService.GradleProperty<T>,
    project: Project,
): Provider<T> = property(nonDeprecatedProperty, project)
    .orElse(
        property(deprecatedProperty, project)
            .map {
                project.reportDiagnosticOncePerBuild(
                    KotlinToolingDiagnostics.DeprecatedPropertyWithReplacement(
                        deprecatedProperty.name,
                        nonDeprecatedProperty.name
                    )
                )
                it!!
            }
    )