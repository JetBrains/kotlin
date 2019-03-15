/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import java.util.*

// TODO better implementation: attribute invariants (no attrs with same name and different types allowed), thread safety?
/** An attribute container that delegates attributes lookup to the [parent] when the key matches [filterParentAttributes] and is missing
 * in this container.
 *
 * This container should never be passed to any Gradle API, as Gradle assumes all [AttributeContainer] instances to
 * implement AttributeContainerInternal.
 * TODO expose Kotlin-specific API to the users, convert the user attributes to Gradle attributes internally
 */
class HierarchyAttributeContainer(
    val parent: AttributeContainer?,
    val filterParentAttributes: (Attribute<*>) -> Boolean = { true }
) : AttributeContainer {
    private val attributesMap = Collections.synchronizedMap(mutableMapOf<Attribute<*>, Any>())

    private fun getFilteredParentAttribute(key: Attribute<*>) =
        if (parent != null && filterParentAttributes(key)) parent.getAttribute(key) else null

    override fun contains(key: Attribute<*>): Boolean =
        attributesMap.contains(key) || getFilteredParentAttribute(key) != null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getAttribute(key: Attribute<T>?): T? =
        attributesMap.get(key as Attribute<*>) as T? ?: getFilteredParentAttribute(key) as T?

    override fun isEmpty(): Boolean = attributesMap.isEmpty() && (parent?.keySet().orEmpty().filter(filterParentAttributes).isEmpty())

    override fun keySet(): Set<Attribute<*>> = attributesMap.keys + parent?.keySet().orEmpty().filter(filterParentAttributes)

    override fun <T : Any?> attribute(key: Attribute<T>?, value: T): AttributeContainer {
        val checkedValue = requireNotNull(value as Any?) { "null values for attributes are not supported" }
        attributesMap[key as Attribute<*>] = checkedValue
        return this
    }

    override fun getAttributes(): AttributeContainer = this
}
