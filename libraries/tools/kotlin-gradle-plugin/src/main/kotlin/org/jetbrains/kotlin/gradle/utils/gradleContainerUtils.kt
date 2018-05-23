/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.NamedDomainObjectContainer

/**
 * Applies [whenMatched] to pairs of items with the same name in [containerA] and [containerB],
 * regardless of the order in which they are added to the containers.
 */
internal fun <A, B> matchSymmetricallyByNames(
    containerA: NamedDomainObjectContainer<A>,
    containerB: NamedDomainObjectContainer<B>,
    whenMatched: (A, B) -> Unit
) {
    val matchedNames = mutableSetOf<String>()

    fun <T, R> NamedDomainObjectContainer<T>.matchAllWith(other: NamedDomainObjectContainer<R>, match: (T, R) -> Unit) {
        this@matchAllWith.all { item ->
            val itemName = this@matchAllWith.namer.determineName(item)
            if (itemName !in matchedNames) {
                val otherItem = other.findByName(itemName)
                if (otherItem != null) {
                    matchedNames += itemName
                    match(item, otherItem)
                }
            }
        }
    }
    containerA.matchAllWith(containerB) { a, b -> whenMatched(a, b) }
    containerB.matchAllWith(containerA) { b, a -> whenMatched(a, b) }
}