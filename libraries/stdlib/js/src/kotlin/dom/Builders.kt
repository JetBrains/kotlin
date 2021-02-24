/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.dom

import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.internal.LowPriorityInOverloadResolution
import kotlinx.dom.appendElement as newAppendElement
import kotlinx.dom.createElement as newCreateElement

/**
 * Creates a new element with the specified [name].
 *
 * The element is initialized with the specified [init] function.
 */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.createElement' instead.",
    replaceWith = ReplaceWith("this.createElement(name, init)", "kotlinx.dom.createElement")
)
@DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun Document.createElement(name: String, noinline init: Element.() -> Unit): Element = this.newCreateElement(name, init)

/**
 * Appends a newly created element with the specified [name] to this element.
 *
 * The element is initialized with the specified [init] function.
 */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.appendElement' instead.",
    replaceWith = ReplaceWith("this.appendElement(name, init)", "kotlinx.dom.appendElement")
)
@DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun Element.appendElement(name: String, noinline init: Element.() -> Unit): Element = this.newAppendElement(name, init)

