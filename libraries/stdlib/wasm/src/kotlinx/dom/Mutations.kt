/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.dom

import org.w3c.dom.*

/** Removes all the children from this node. */
@SinceKotlin("1.4")
public fun Node.clear() {
    while (hasChildNodes()) {
        removeChild(firstChild!!)
    }
}

/**
 * Creates text node and append it to the element.
 *
 * @return this element
 */
@SinceKotlin("1.4")
fun Element.appendText(text: String): Element {
    appendChild(ownerDocument!!.createTextNode(text))
    return this
}
