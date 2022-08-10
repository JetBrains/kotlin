/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.utils

import org.jetbrains.kotlin.project.model.infra.KpmTestEntity

class ObservableIndexedCollection<T : KpmTestEntity> private constructor(
    private val _items: MutableMap<String, T>
) : Collection<T> by _items.values {
    constructor() : this(mutableMapOf())

    private val allItemsActions = mutableListOf<T.() -> Unit>()

    fun add(item: T) {
        _items[item.name] = item
        allItemsActions.forEach { action -> action(item) }
    }

    fun withAll(action: T.() -> Unit) {
        _items.values.forEach(action)
        allItemsActions.add(action)
    }

    fun getOrPut(name: String, defaultValue: () -> T): T =
        if (!_items.contains(name)) defaultValue().also { add(it) } else _items[name]!!

    operator fun get(name: String): T? = _items[name]
}
