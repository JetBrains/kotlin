/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable

internal abstract class CompilerSystemPropertiesService : BuildService<CompilerSystemPropertiesService.Parameters>, AutoCloseable {
    internal interface Parameters : BuildServiceParameters {
        val properties: MapProperty<String, Provider<String>>
    }

    private val properties by lazy { parameters.properties.get().mapValues { it.value.orNull }.toMutableMap() }

    fun startIntercept() {
        if (parameters.properties.get().isEmpty()) return

        CompilerSystemProperties.systemPropertyGetter = {
            if (it in properties) properties[it] else System.getProperty(it)
        }
        CompilerSystemProperties.systemPropertySetter = setter@{ key, value ->
            if (key !in properties) {
                return@setter System.setProperty(key, value)
            }
            val oldValue = properties[key]
            properties[key] = value
            oldValue
        }
        CompilerSystemProperties.systemPropertyCleaner = cleaner@{
            if (it !in properties) {
                return@cleaner System.clearProperty(it)
            }
            val oldValue = properties[it]
            properties.remove(it)
            oldValue
        }
    }

    override fun close() {
        CompilerSystemProperties.systemPropertyGetter = null
        CompilerSystemProperties.systemPropertySetter = null
        CompilerSystemProperties.systemPropertyCleaner = null
    }

    companion object {
        fun registerIfAbsent(gradle: Gradle): Provider<CompilerSystemPropertiesService> = gradle.sharedServices.registerIfAbsent(
            "${CompilerSystemPropertiesService::class.java.canonicalName}_${CompilerSystemPropertiesService::class.java.classLoader.hashCode()}",
            CompilerSystemPropertiesService::class.java
        ) { service ->
            if (isConfigurationCacheAvailable(gradle)) {
                service.parameters.properties.set(
                    CompilerSystemProperties.values()
                        .filterNot { it.alwaysDirectAccess }
                        .associate {
                            it.property to gradle.rootProject.providers.systemProperty(it.property).forUseAtConfigurationTime()
                        }.toMap()
                )
            }
        }
    }
}
