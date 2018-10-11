/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.dom

import org.w3c.dom.*

/**
 * Creates a new element with the specified [name].
 *
 * The element is initialized with the speicifed [init] function.
 */
public fun Document.createElement(name: String, init: Element.() -> Unit): Element = createElement(name).apply(init)

/**
 * Appends a newly created element with the specified [name] to this element.
 *
 * The element is initialized with the speicifed [init] function.
 */
public fun Element.appendElement(name: String, init: Element.() -> Unit): Element =
    ownerDocument!!.createElement(name, init).also { appendChild(it) }

