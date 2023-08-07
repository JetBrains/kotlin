/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

interface ObservableSet<T> : Set<T> {
    fun whenObjectAdded(action: (T) -> Unit)
    fun forAll(action: (T) -> Unit)
}

interface MutableObservableSet<T> : ObservableSet<T>, MutableSet<T>

internal class MutableObservableSetImpl<T>(vararg elements: T) : MutableObservableSet<T> {
    private val underlying = mutableSetOf(*elements)
    private val whenObjectAddedActions = mutableListOf<(T) -> Unit>()
    private val forAllActions = mutableListOf<(T) -> Unit>()

    override fun whenObjectAdded(action: (T) -> Unit) {
        whenObjectAddedActions.add(action)
    }

    override fun forAll(action: (T) -> Unit) {
        forAllActions.add(action)
        underlying.toList().forEach(action)
    }

    override val size: Int
        get() = underlying.size

    override fun clear() {
        underlying.clear()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val elementsToAdd = elements.toSet() - underlying
        elementsToAdd.forEach(this::add)
        return elementsToAdd.isNotEmpty()
    }

    override fun add(element: T): Boolean {
        val added = underlying.add(element)
        if (added) {
            whenObjectAddedActions.toTypedArray().forEach { action -> action(element) }
            forAllActions.toTypedArray().forEach { action -> action(element) }
        }
        return added
    }

    override fun isEmpty(): Boolean {
        return underlying.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        return underlying.iterator()
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return underlying.retainAll(elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return underlying.removeAll(elements)
    }

    override fun remove(element: T): Boolean {
        return underlying.remove(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return underlying.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return underlying.contains(element)
    }

    override fun equals(other: Any?): Boolean {
        return underlying == other
    }

    override fun hashCode(): Int {
        return underlying.hashCode()
    }

    override fun toString(): String {
        return underlying.toString()
    }

    init {
        underlying.addAll(elements)
    }
}
