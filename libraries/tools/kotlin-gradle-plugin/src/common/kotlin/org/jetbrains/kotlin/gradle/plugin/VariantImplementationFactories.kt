/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Provides a way for Gradle plugin variants to register specific implementation factories,
 * that could be used inside common code.
 */
abstract class VariantImplementationFactories : BuildService<BuildServiceParameters.None> {
    private val factories: MutableMap<KClass<*>, VariantImplementationFactory> = ConcurrentHashMap()
    operator fun <T : VariantImplementationFactory> set(
        type: KClass<T>,
        factory: T
    ) {
        factories[type] = factory
    }

    fun <T : VariantImplementationFactory> putIfAbsent(
        type: KClass<T>,
        factory: T
    ) {
        factories.putIfAbsent(type, factory)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : VariantImplementationFactory> get(type: KClass<T>): T {
        return factories[type] as? T ?: throw IllegalArgumentException("${type.simpleName} type is not known for plugin variants")
    }

    /**
     * Marker interface for actual implementation factories.
     */
    interface VariantImplementationFactory

    companion object {
        fun getProvider(
            gradle: Gradle
        ): Provider<VariantImplementationFactories> {
            // Use class loader hashcode in case there are multiple class loaders in the same build
            return gradle.sharedServices
                .registerIfAbsent(
                    "variant_impl_factories_${VariantImplementationFactories::class.java.classLoader.hashCode()}",
                    VariantImplementationFactories::class.java
                ) {}
        }

        fun get(gradle: Gradle): VariantImplementationFactories = getProvider(gradle).get()
    }
}

internal inline fun <reified T : VariantImplementationFactories.VariantImplementationFactory> Gradle.variantImplementationFactory(): T =
    VariantImplementationFactories.get(this)[T::class]

internal inline fun <
        reified T : VariantImplementationFactories.VariantImplementationFactory
        > Gradle.variantImplementationFactoryProvider(): Provider<T> =
    VariantImplementationFactories.getProvider(this).map { it[T::class] }
