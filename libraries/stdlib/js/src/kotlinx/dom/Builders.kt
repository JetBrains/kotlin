/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.dom

import org.w3c.dom.*
import kotlin.contracts.*

/**
 * Creates a new element with the specified [name].
 *
 * The element is initialized with the specified [init] function.
 */
@SinceKotlin("1.4")
public fun Document.createElement(name: String, init: Element.() -> Unit): Element {
    contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
    return createElement(name).apply(init)
}

/**
 * Appends a newly created element with the specified [name] to this element.
 *
 * The element is initialized with the specified [init] function.
 */
@SinceKotlin("1.4")
public fun Element.appendElement(name: String, init: Element.() -> Unit): Element {
    contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
    return ownerDocument!!.createElement(name, init).also { appendChild(it) }
}

