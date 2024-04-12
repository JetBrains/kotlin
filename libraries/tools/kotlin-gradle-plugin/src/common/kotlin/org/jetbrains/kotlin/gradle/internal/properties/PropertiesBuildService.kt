/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.properties

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull
import org.jetbrains.kotlin.gradle.plugin.internal.ConfigurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.utils.localProperties
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
        val configurationTimePropertiesAccessor: Property<ConfigurationTimePropertiesAccessor>
    }

    /**
     * Key should be `project.path/propertyName`.
     */
    private val propertiesPerProject = ConcurrentHashMap<String, Provider<String>>()

    private val configurationTimePropertiesAccessor by lazy { parameters.configurationTimePropertiesAccessor.get() }
    private val localProperties by lazy { parameters.localProperties.get() }

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
                    ?: providerFactory.gradleProperty(propertyName).usedAtConfigurationTime(configurationTimePropertiesAccessor).orNull
                    ?: localProperties[propertyName]
            }
            providerFactory.provider { valueFromGradleAndLocalProperties.call() }
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

    /** Returns the value of the property with the given [propertyName] in the given [project]. */
    fun get(propertyName: String, project: Project): String? {
        return property(propertyName, project).orNull
    }

    companion object {

        fun registerIfAbsent(project: Project): Provider<PropertiesBuildService> =
            project.gradle.registerClassLoaderScopedBuildService(PropertiesBuildService::class) {
                it.parameters.localProperties.set(project.localProperties)
                it.parameters.configurationTimePropertiesAccessor.set(project.configurationTimePropertiesAccessor)
            }
    }

    private class MemoizedCallable<T>(valueResolver: Callable<T>) : Callable<T> {
        private val value: T? by lazy { valueResolver.call() }
        override fun call(): T? = value
    }
}
