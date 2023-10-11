/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.internal.ConfigurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.utils.localProperties
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * [BuildService] that looks up properties in the following precedence order:
 *   1. Project's extra properties ([org.gradle.api.plugins.ExtensionContainer.getExtraProperties])
 *   2. Project's Gradle properties ([org.gradle.api.provider.ProviderFactory.gradleProperty])
 *   3. Root project's `local.properties` file ([Project.localProperties])
 *
 *  Note that extra and Gradle properties may differ across projects, whereas `local.properties` is shared across all projects.
 */
internal abstract class PropertiesBuildService : BuildService<PropertiesBuildService.Params> {

    interface Params : BuildServiceParameters {
        val localProperties: MapProperty<String, String>
    }

    private val propertiesManager = ConcurrentHashMap<Project, PropertiesManager>()

    /** Returns a [Provider] of the value of the property with the given [propertyName] in the given [project]. */
    fun property(propertyName: String, project: Project): Provider<String> {
        return propertiesManager.computeIfAbsent(project) { PropertiesManager(project, parameters.localProperties.get()) }
            .property(propertyName)
    }

    /** Returns the value of the property with the given [propertyName] in the given [project]. */
    fun get(propertyName: String, project: Project): String? {
        return property(propertyName, project).orNull
    }

    companion object {

        fun registerIfAbsent(project: Project): Provider<PropertiesBuildService> =
            project.gradle.registerClassLoaderScopedBuildService(PropertiesBuildService::class) {
                it.parameters.localProperties.set(project.localProperties)
            }
    }
}

private class PropertiesManager(private val project: Project, private val localProperties: Map<String, String>) {

    private val configurationTimePropertiesAccessor: ConfigurationTimePropertiesAccessor by lazy {
        project.configurationTimePropertiesAccessor
    }

    private val properties = ConcurrentHashMap<String, Provider<String>>()

    fun property(propertyName: String): Provider<String> {
        // Note: The same property may be read many times (KT-62496). Therefore,
        //   - Use a map to create only one Provider per property.
        //   - Use MemoizedCallable to resolve the Provider only once
        return properties.computeIfAbsent(propertyName) {
            project.provider { computeValue(propertyName) }
        }
    }

    private fun computeValue(propertyName: String): String? {
        return project.extraProperties.getOrNull(propertyName) as String? // We don't memoize extraProperties as they can still change
            ?: MemoizedCallable {
                project.providers.gradleProperty(propertyName).usedAtConfigurationTime(configurationTimePropertiesAccessor).orNull
                    ?: localProperties[propertyName]
            }.call()
    }
}

private class MemoizedCallable<T>(valueResolver: Callable<T>) : Callable<T> {
    private val value by lazy { valueResolver.call() }
    override fun call(): T = value
}
