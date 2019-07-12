/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.dom

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import kotlin.reflect.KProperty

/**
 * Gets a value indicating whether this node is a TEXT_NODE or a CDATA_SECTION_NODE.
 */
public val Node.isText: Boolean
    get() = nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE

/**
 * Gets a value indicating whether this node is an [Element].
 */
public val Node.isElement: Boolean
    get() = nodeType == Node.ELEMENT_NODE


/**
 * Provides a short access to document elements by ID via delegated
 * property syntax. Received element is not cached and received
 * directly from the [Document] by calling [Document.getElementById]
 * function on every property access. Throws an exception if element
 * is not found or has different type
 *
 * To access an element with `theId` ID use the following property declaration
 * ```
 * val theId by document.gettingElementById
 * ```
 *
 * To access an element of specific type, just add it to the property declaration
 * ```
 * val theId: HTMLImageElement by document.gettingElementById
 * ```
 */
inline val Document.gettingElementById get() = DocumentGettingElementById(this)

/**
 * Implementation details of [Document.gettingElementById]
 * @see Document.gettingElementById
 */
inline class DocumentGettingElementById(val document: Document) {
    /**
     * Implementation details of [Document.gettingElementById]. Delegated property
     * @see Document.gettingElementById
     */
    inline operator fun <reified T : Element> getValue(x: Any?, kProperty: KProperty<*>): T {
        val id = kProperty.name
        val element = document.getElementById(id) ?: throw NullPointerException("Element $id is not found")
        return element as? T ?: throw ClassCastException(
            "Element $id has type ${element::class.simpleName} which does not implement ${T::class.simpleName}"
        )
    }
}
