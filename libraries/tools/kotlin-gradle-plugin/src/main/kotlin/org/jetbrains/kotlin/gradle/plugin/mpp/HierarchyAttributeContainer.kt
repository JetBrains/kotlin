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
class HierarchyAttributeContainer(val parent: AttributeContainer?) : AttributeContainer {
    private val attributesMap = Collections.synchronizedMap(mutableMapOf<Attribute<*>, Any>())

    override fun contains(key: Attribute<*>): Boolean =
        attributesMap.contains(key) || parent?.contains(key) ?: false

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getAttribute(key: Attribute<T>?): T? =
        attributesMap.get(key as Attribute<*>) as T? ?: parent?.getAttribute(key)

    override fun isEmpty(): Boolean = attributesMap.isEmpty() && parent?.isEmpty ?: false

    override fun keySet(): Set<Attribute<*>> = attributesMap.keys + parent?.keySet().orEmpty()

    override fun <T : Any?> attribute(key: Attribute<T>?, value: T): AttributeContainer {
        val checkedValue = requireNotNull(value as Any?) { "null values for attributes are not supported" }
        attributesMap[key as Attribute<*>] = checkedValue
        return this
    }

    override fun getAttributes(): AttributeContainer = this
}