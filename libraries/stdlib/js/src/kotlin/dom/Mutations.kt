/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.dom

import org.w3c.dom.Element
import org.w3c.dom.Node
import kotlin.internal.LowPriorityInOverloadResolution
import kotlinx.dom.appendText as newAppendText
import kotlinx.dom.clear as newClear

/** Removes all the children from this node. */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.clear' instead.",
    replaceWith = ReplaceWith("this.clear()", "kotlinx.dom.clear")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
public inline fun Node.clear(): Unit = this.newClear()

/**
 * Creates text node and append it to the element.
 *
 * @return this element
 */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.appendText' instead.",
    replaceWith = ReplaceWith("this.appendText(text)", "kotlinx.dom.appendText")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
public inline fun Element.appendText(text: String): Element = this.newAppendText(text)
