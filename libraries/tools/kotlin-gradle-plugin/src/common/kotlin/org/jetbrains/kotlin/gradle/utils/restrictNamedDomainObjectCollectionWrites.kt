/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.NamedDomainObjectCollection

internal interface KotlinNamedDomainObjectCollectionWriter<T : Any> {
    fun add(element: T): Unit
}

/**
 * Restricts any mutation of [this] NamedDomainObjectSet except
 * the ones that happen through returned [KotlinNamedDomainObjectCollectionWriter]
 *
 * For example:
 * ```kotlin
 * val objectSet = project.objects.namedDomainObjectSet<Foo>()
 *
 * objectSet.add(Foo1) // is allowed!
 *
 * val writer = objectSet.restrictWritesToSingleWriterOnly()
 * objectSet.add(Foo2) // no longer allowed and will throw a RuntimeException
 * writer.add(Foo2) // is possible and this is the only way to add elements to objectSet
 * ```
 *
 * ## Important implementation detail
 * All subscribers (i.e. all {}, whenObjectAdded {}, etc.) that were add before restricting the collection
 * will be notified upon mutation, however elements still will not be added into collection
 */
internal fun <T : Any> NamedDomainObjectCollection<T>.restrictWritesToSingleWriterOnly(): KotlinNamedDomainObjectCollectionWriter<T> {
    val allowedElements = mutableSetOf<T>()
    val illegallyAddedElements = mutableSetOf<T>()
    whenObjectAdded { element ->
        if (element !in allowedElements) {
            remove(element)
            error("Illegal mutation of $this. This collection is read only.")
        }
    }
    whenObjectRemoved { element ->
        // when object was illegally added to the collection, we just ignore
        if (illegallyAddedElements.remove(element)) return@whenObjectRemoved

        if (element !in allowedElements) error("Illegal mutation of $this. This collection is read only.")
    }

    return KotlinNamedDomainObjectCollectionWriterImpl(allowedElements, this)
}

private class KotlinNamedDomainObjectCollectionWriterImpl<T : Any>(
    private val allowedElements: MutableSet<T>,
    private val namedDomainObjectSet: NamedDomainObjectCollection<T>
) : KotlinNamedDomainObjectCollectionWriter<T> {
    override fun add(element: T) {
        allowedElements.add(element)
        namedDomainObjectSet.add(element)
    }
}