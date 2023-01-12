/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.Serializable
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * A build service for configuring the [VariantImplementationFactories] build service.
 * Provides a way for Gradle plugin variants to register specific implementation factories,
 * that could be used inside the common code.
 *
 * We cannot register them directly in [VariantImplementationFactories] as we would lose the factories after
 * the service reinitialization on configuration cache retrieval. Thus, this service should be used to register factories
 * and [VariantImplementationFactories] should be used to use them.
 */
internal abstract class VariantImplementationFactoriesConfigurator : BuildService<BuildServiceParameters.None> {
    val factories: MutableMap<String, VariantImplementationFactories.VariantImplementationFactory> = ConcurrentHashMap()

    fun <T : VariantImplementationFactories.VariantImplementationFactory> putIfAbsent(
        type: KClass<T>,
        factory: T
    ) {
        factories.putIfAbsent(type.java.name, factory)
    }

    operator fun <T : VariantImplementationFactories.VariantImplementationFactory> set(
        type: KClass<T>,
        factory: T
    ) {
        factories[type.java.name] = factory
    }

    companion object {
        fun getProvider(
            gradle: Gradle
        ): Provider<VariantImplementationFactoriesConfigurator> {
            // Use class loader hashcode in case there are multiple class loaders in the same build
            return gradle.sharedServices
                .registerIfAbsent(
                    "variant_impl_factories_configurator_${VariantImplementationFactoriesConfigurator::class.java.classLoader.hashCode()}",
                    VariantImplementationFactoriesConfigurator::class.java
                ) {}
        }

        fun get(gradle: Gradle): VariantImplementationFactoriesConfigurator = getProvider(gradle).get()
    }
}

internal interface UsesVariantImplementationFactories : Task

/**
 * Provides a way for Gradle plugin variants to use specific implementation factories in the common code
 */
abstract class VariantImplementationFactories : BuildService<VariantImplementationFactories.Parameters> {
    interface Parameters : BuildServiceParameters {
        val factories: MapProperty<String, VariantImplementationFactory>
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : VariantImplementationFactory> get(type: KClass<T>): T {
        return parameters.factories.get()[type.java.name] as? T
            ?: throw IllegalArgumentException("${type.simpleName} type is not known for plugin variants")
    }

    /**
     * Marker interface for actual implementation factories.
     */
    interface VariantImplementationFactory : Serializable

    companion object {
        /**
         * Please don't change the visibility modifier. This method isn't intended to be used directly.
         * This method doesn't declare the service usage from Gradle tasks.
         */
        private fun getProvider(
            gradle: Gradle
        ): Provider<VariantImplementationFactories> {
            val configProvider = VariantImplementationFactoriesConfigurator.getProvider(gradle)
            // Use class loader hashcode in case there are multiple class loaders in the same build
            return gradle.sharedServices
                .registerIfAbsent(
                    "variant_impl_factories_${VariantImplementationFactories::class.java.classLoader.hashCode()}",
                    VariantImplementationFactories::class.java
                ) {
                    it.parameters.factories.value(configProvider.get().factories)
                }
        }

        fun getProvider(
            project: Project
        ) = getProvider(project.gradle).also { serviceProvider ->
            SingleActionPerProject.run(project, UsesVariantImplementationFactories::class.java.name) {
                project.tasks.withType<UsesVariantImplementationFactories>().configureEach { task ->
                    task.usesService(serviceProvider)
                }
            }
        }

        fun get(project: Project): VariantImplementationFactories = getProvider(project).get()
    }
}

internal inline fun <reified T : VariantImplementationFactories.VariantImplementationFactory> Project.variantImplementationFactory(): T =
    VariantImplementationFactories.get(this)[T::class]

internal inline fun <
        reified T : VariantImplementationFactories.VariantImplementationFactory
        > Project.variantImplementationFactoryProvider(): Provider<T> =
    VariantImplementationFactories.getProvider(this).map { it[T::class] }
