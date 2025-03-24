/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.specs.Spec
import org.gradle.api.provider.Provider
import java.util.*

/**
 * An immutable analogue of the [org.gradle.api.NamedDomainObjectCollection] Gradle class.
 *
 * The methods of adding and removing elements have been removed from it to make it easier
 * to ensure that the user does not add new elements that lack the necessary initial configuration and checks.
 *
 * A stripped-down copy of [org.gradle.api.NamedDomainObjectCollection](https://github.com/gradle/gradle/blob/v8.13.0/subprojects/core-api/src/main/java/org/gradle/api/NamedDomainObjectCollection.java)
 * @since 2.2.0
 */
interface NamedDomainImmutableCollection<T : Any> {
    /**
     *
     * Returns the names of the objects in this collection as a Set of Strings.
     *
     *
     * The set of names is in *natural ordering*.
     *
     * @return The names. Returns an empty set if this collection is empty.
     */
    fun getNames(): SortedSet<String>


    /**
     * Locates a object by name, without triggering its creation or configuration, failing if there is no such object.
     *
     * @param name The object's name
     * @return A [Provider] that will return the object when queried. The object may be created and configured at this point, if not already.
     * @throws UnknownDomainObjectException If a object with the given name is not defined.
     * @since 4.10
     */
    fun named(name: String): NamedDomainObjectProvider<T>

    /**
     * Locates a object by name, without triggering its creation or configuration, failing if there is no such object.
     * The given configure action is executed against the object before it is returned from the provider.
     *
     * @param name The object's name
     * @return A [Provider] that will return the object when queried. The object may be created and configured at this point, if not already.
     * @throws UnknownDomainObjectException If an object with the given name is not defined.
     * @since 5.0
     */
    fun named(name: String, configurationAction: Action<in T>): NamedDomainObjectProvider<T>

    /**
     * Locates a object by name and type, without triggering its creation or configuration, failing if there is no such object.
     *
     * @param name The object's name
     * @param type The object's type
     * @return A [Provider] that will return the object when queried. The object may be created and configured at this point, if not already.
     * @throws UnknownDomainObjectException If an object with the given name is not defined.
     * @since 5.0
     */
    fun <S : T> named(name: String, type: Class<S>): NamedDomainObjectProvider<S>

    /**
     * Locates a object by name and type, without triggering its creation or configuration, failing if there is no such object.
     * The given configure action is executed against the object before it is returned from the provider.
     *
     * @param name The object's name
     * @param type The object's type
     * @param configurationAction The action to use to configure the object.
     * @return A [Provider] that will return the object when queried. The object may be created and configured at this point, if not already.
     * @throws UnknownDomainObjectException If an object with the given name is not defined.
     * @since 5.0
     */
    fun <S : T> named(name: String, type: Class<S>, configurationAction: Action<in S>): NamedDomainObjectProvider<S>


    /**
     * Returns a collection containing the objects in this collection of the given type.  The returned collection is
     * live, so that when matching objects are later added to this collection, they are also visible in the filtered
     * collection.
     *
     * @param type The type of objects to find.
     * @return The matching objects. Returns an empty collection if there are no such objects in this collection.
     */
    fun <S : T> withType(type: Class<S>): NamedDomainImmutableCollection<S>

    /**
     * Returns a collection containing the objects in this collection of the given type. Equivalent to calling
     * `withType(type).all(configureAction)`
     *
     * @param type The type of objects to find.
     * @param configureAction The action to execute for each object in the resulting collection.
     * @return The matching objects. Returns an empty collection if there are no such objects in this collection.
     */
    fun <S : T> withType(type: Class<S>, configureAction: Action<in S>): NamedDomainImmutableCollection<S>

    /**
     * Returns a collection containing the objects in this collection of the given type. Equivalent to calling
     * `withType(type).all(configureClosure)`.
     *
     * @param type The type of objects to find.
     * @param configureClosure The closure to execute for each object in the resulting collection.
     * @return The matching objects. Returns an empty collection if there are no such objects in this collection.
     */
    fun <S : T> withType(
        @DelegatesTo.Target type: Class<S>,
        @DelegatesTo(genericTypeIndex = 0) configureClosure: Closure<*>,
    ): NamedDomainImmutableCollection<S>

    /**
     * Returns a collection which contains the objects in this collection which meet the given specification. The
     * returned collection is live, so that when matching objects are added to this collection, they are also visible in
     * the filtered collection.
     *
     * @param spec The specification to use.
     * @return The collection of matching objects. Returns an empty collection if there are no such objects in this
     * collection.
     */
    fun matching(spec: Spec<in T>): NamedDomainImmutableCollection<T>

    /**
     * Returns a collection which contains the objects in this collection which meet the given closure specification. The
     * returned collection is live, so that when matching objects are added to this collection, they are also visible in
     * the filtered collection.
     *
     * @param spec The specification to use. The closure gets a collection element as an argument.
     * @return The collection of matching objects. Returns an empty collection if there are no such objects in this
     * collection.
     */
    fun matching(spec: Closure<*>): NamedDomainImmutableCollection<T>

    /**
     * Configures each element in this collection using the given action, as each element is required. Actions are run in the order added.
     *
     * @param action A [Action] that can configure the element when required.
     * @since 4.9
     */
    fun configureEach(action: Action<in T>)
}