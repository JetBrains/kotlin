/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.model.ObjectFactory
import org.gradle.api.specs.Spec
import org.jetbrains.kotlin.gradle.dsl.NamedDomainImmutableCollection
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.util.*
import javax.inject.Inject

/**
 * Wrapper for regular [NamedDomainObjectCollection] to make immutable instance of [NamedDomainImmutableCollection].
 */
internal abstract class NamedDomainImmutableCollectionImpl<T : Any> @Inject constructor(
    private val objects: ObjectFactory,
    private val origin: NamedDomainObjectCollection<T>,
) : NamedDomainImmutableCollection<T> {

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

    override fun matching(spec: Spec<in T>): NamedDomainImmutableCollection<T> {
        val result = origin.matching(spec)
        return objects.newInstance<NamedDomainImmutableCollectionImpl<T>>(objects, result)
    }

    override fun matching(spec: Closure<*>): NamedDomainImmutableCollection<T> {
        val result = origin.matching(spec)
        return objects.newInstance<NamedDomainImmutableCollectionImpl<T>>(objects, result)
    }

    override fun configureEach(action: Action<in T>) {
        origin.configureEach(action)
    }
}