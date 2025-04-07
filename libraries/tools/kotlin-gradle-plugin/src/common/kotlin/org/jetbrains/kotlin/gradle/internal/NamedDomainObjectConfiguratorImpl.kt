/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.specs.Spec
import java.util.*

/**
 * A class that allows to add mutating methods in inheritors. They can be made public, or left internal and called from other functions.
 */
internal abstract class NamedDomainObjectConfiguratorImpl<T : Any>(objects: ObjectFactory) {

    protected abstract val type: Class<T>

    protected abstract fun factory(name: String): T

    protected abstract fun preConfigure(element: T)

    private val origin: NamedDomainObjectContainer<T> = objects.domainObjectContainer(type) { variantName ->
        factory(variantName).also { element -> preConfigure(element) }
    }

    /**
     * Adds an object to the collection, if there is no existing object in the collection with the same name.
     *
     * @param e the item to add to the collection
     * @return `true` if the item was added, or `false` if an item with the same name already exists.
     */
    fun add(e: T): Boolean {
        preConfigure(e)
        return origin.add(e)
    }

    fun register(name: String, configurationAction: Action<T>? = null) {
        if (configurationAction == null) {
            origin.register(name)
        } else {
            origin.register(name, configurationAction)
        }
    }

    fun configureEach(action: Action<in T>) {
        origin.configureEach(action)
    }

    fun getNames(): SortedSet<String> {
        return origin.names
    }

    fun named(name: String): NamedDomainObjectProvider<T> {
        return origin.named(name)
    }

    fun named(name: String, configurationAction: Action<in T>): NamedDomainObjectProvider<T> {
        return origin.named(name, configurationAction)
    }

    fun <S : T> named(name: String, type: Class<S>): NamedDomainObjectProvider<S> {
        return origin.named(name, type)
    }

    fun <S : T> named(
        name: String,
        type: Class<S>,
        configurationAction: Action<in S>,
    ): NamedDomainObjectProvider<S> {
        return origin.named(name, type, configurationAction)
    }

    fun matching(spec: Closure<*>): NamedDomainObjectSet<T> {
        return origin.matching(spec)
    }

    fun matching(spec: Spec<in T>): NamedDomainObjectSet<T> {
        return origin.matching(spec)
    }

    fun <S : T> withType(type: Class<S>): NamedDomainObjectSet<S> {
        return origin.withType(type)
    }

    fun <S : T> withType(
        type: Class<S>,
        configureAction: Action<in S>,
    ): DomainObjectCollection<S> {
        return origin.withType(type, configureAction)
    }

    fun <S : T> withType(
        type: Class<S>,
        configureClosure: Closure<*>,
    ): DomainObjectCollection<S> {
        return origin.withType(type, configureClosure)
    }
}