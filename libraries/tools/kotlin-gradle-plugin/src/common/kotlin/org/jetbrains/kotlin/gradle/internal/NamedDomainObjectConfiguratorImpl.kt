/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.model.ObjectFactory
import org.gradle.api.specs.Spec
import org.jetbrains.kotlin.gradle.dsl.NamedDomainImmutableCollection
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.util.*

/**
 * A class that allows to add mutating methods in inheritors. They can be made public, or left internal and called from other functions.
 */
internal abstract class NamedDomainObjectConfiguratorImpl<T : Any>(private val objects: ObjectFactory) : NamedDomainImmutableCollection<T> {

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
     * @return `true` if the item was added, or `` false if an item with the same name already exists.
     */
    protected fun doAdd(e: T): Boolean {
        preConfigure(e)
        return origin.add(e)
    }

    protected fun doRegister(name: String, configurationAction: Action<T>? = null) {
        if (configurationAction == null) {
            origin.register(name)
        } else {
            origin.register(name, configurationAction)
        }
    }

    override fun configureEach(action: Action<in T>) {
        origin.configureEach(action)
    }

    override fun getNames(): SortedSet<String> {
        return origin.names
    }

    override fun named(name: String): NamedDomainObjectProvider<T> {
        return origin.named(name)
    }

    override fun named(name: String, configurationAction: Action<in T>): NamedDomainObjectProvider<T> {
        return origin.named(name, configurationAction)
    }

    override fun <S : T> named(name: String, type: Class<S>): NamedDomainObjectProvider<S> {
        return origin.named(name, type)
    }

    override fun <S : T> named(
        name: String,
        type: Class<S>,
        configurationAction: Action<in S>,
    ): NamedDomainObjectProvider<S> {
        return origin.named(name, type, configurationAction)
    }

    override fun matching(spec: Closure<*>): NamedDomainImmutableCollection<T> {
        val result = origin.matching(spec)
        return objects.newInstance<NamedDomainImmutableCollectionImpl<T>>(objects, result)
    }

    override fun matching(spec: Spec<in T>): NamedDomainImmutableCollection<T> {
        val result = origin.matching(spec)
        return objects.newInstance<NamedDomainImmutableCollectionImpl<T>>(objects, result)
    }

    override fun <S : T> withType(type: Class<S>): NamedDomainImmutableCollection<S> {
        val result = origin.withType(type)
        return objects.newInstance<NamedDomainImmutableCollectionImpl<S>>(objects, result)
    }

    override fun <S : T> withType(
        type: Class<S>,
        configureAction: Action<in S>,
    ): NamedDomainImmutableCollection<S> {
        val result = origin.withType(type, configureAction)
        return objects.newInstance<NamedDomainImmutableCollectionImpl<S>>(objects, result)
    }

    override fun <S : T> withType(
        type: Class<S>,
        configureClosure: Closure<*>,
    ): NamedDomainImmutableCollection<S> {
        val result = origin.withType(type, configureClosure)
        return objects.newInstance<NamedDomainImmutableCollectionImpl<S>>(objects, result)
    }
}