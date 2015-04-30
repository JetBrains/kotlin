/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.w3c.dom

import java.util.AbstractList
import kotlin.js.splitWithRegex

native
public val window: Window = noImpl

native
public val document: Document = noImpl

native
public val localStorage: Storage = noImpl

native
public val sessionStorage: Storage = noImpl


private class HTMLCollectionListView(val collection: HTMLCollection) : AbstractList<HTMLElement>() {
    override fun size(): Int = collection.length.toInt()

    override fun get(index: Int): HTMLElement =
            if (index in 0..size() - 1) collection.item(index) as HTMLElement
            else throw IndexOutOfBoundsException("index $index is not in range [0 .. ${size() - 1})")
}

public fun HTMLCollection.asList(): List<HTMLElement> = HTMLCollectionListView(this)

private class NodeListAsList(val delegate: NodeList) : AbstractList<Node>() {
    override fun size(): Int = delegate.length

    override fun get(index: Int): Node =
            if (index in 0..size() - 1) delegate.get(index)!!
            else throw IndexOutOfBoundsException("index $index is not in range [0 .. ${size() - 1})")
}

public fun NodeList.asList() : List<Node> = NodeListAsList(this)

/**
 * Adds CSS class to element. Has no effect if all specified classes are already in class attribute of the element
 */
public fun Element.addClass(vararg cssClasses: String): Boolean {
    val missingClasses = cssClasses.filterNot { hasClass(it) }
    if (missingClasses.isNotEmpty()) {
        className = StringBuilder {
            append(className)
            missingClasses.joinTo(this, " ", " ")
        }.toString()
        return true
    }

    return false
}

/**
 * Removes all [cssClasses] from element. Has no effect if all specified classes are missing in class attribute of the element
 */
public fun Element.removeClass(vararg cssClasses: String): Boolean {
    if (cssClasses.any { hasClass(it) }) {
        val toBeRemoved = cssClasses.toSet()
        className = className.splitWithRegex("\\s+").filter { it !in toBeRemoved }.joinToString(" ")
        return true
    }

    return false
}

/**
 * Adds [cssClasses] to element if []condition] is true or removes it otherwise.
 * Has no effect if class attribute already has corresponding content
 */
public fun Element.addOrRemoveClassWhen(condition : Boolean, vararg cssClasses : String) {
    if (condition) {
        addClass(*cssClasses)
    } else {
        removeClass(*cssClasses)
    }
}

/**
 * Adds [trueClassName] class to element if [condition] is true and removes [falseClassName].
 * If [condition] is false then [trueClassName] will be removed and [falseClassName] will be added.
 * Has no effect if class attribute already has corresponding content
 */
public fun Element.swapClassWhen(condition: Boolean, trueClassName: String, falseClassName: String) {
    if (condition) {
        addClass(trueClassName)
        removeClass(falseClassName)
    } else {
        removeClass(trueClassName)
        addClass(falseClassName)
    }
}

/**
 * If [condition] is true then [attributeName] with value [attributeValue] will be added to the element, otherwise attribute with
 * name [attributeName] will be removed
 */
public fun Element.addOrRemoveAttributeWhen(condition : Boolean, attributeName : String, attributeValue : String = attributeName) {
    if (condition) {
        setAttribute(attributeName, attributeValue)
    } else {
        removeAttribute(attributeName)
    }
}

/** Returns true if the element has the given CSS class style in its 'class' attribute */
public fun Element.hasClass(cssClass: String): Boolean {
    val c = this.className
    return c.matches("""(^|.*\s+)$cssClass($|\s+.*)""")
}

/**
 * Removes all child nodes
 */
public fun Node.clear() {
    while (hasChildNodes()) {
        removeChild(firstChild!!)
    }
}

/**
 * Removes this node from parent node. Does nothing if no parent node
 */
public fun Node.removeFromParent() {
    parentNode?.removeChild(this)
}

/**
 * it is *true* when [Node.nodeType] is TEXT_NODE or CDATA_SECTION_NODE
 */
public val Node.isText : Boolean
    get() = nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE

/**
 * Creates text node and append it to the element
 */
public fun Element.appendText(text : String, doc : Document = this.ownerDocument!!) {
    appendChild(doc.createTextNode(text))
}

/**
 * Appends the node to the specified parent element
 */
public fun Node.appendTo(parent : Element) {
    parent.appendChild(this)
}
