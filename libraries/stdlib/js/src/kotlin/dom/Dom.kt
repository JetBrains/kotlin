/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.dom

import org.w3c.dom.Element
import org.w3c.dom.Node
import kotlin.internal.LowPriorityInOverloadResolution
import kotlinx.dom.isElement as newIsElement
import kotlinx.dom.isText as newIsText

/**
 * Gets a value indicating whether this node is a TEXT_NODE or a CDATA_SECTION_NODE.
 */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.isText' instead.",
    replaceWith = ReplaceWith("this.isText", "kotlinx.dom.isText")
)
@DeprecatedSinceKotlin(warningSince = "1.4")
public val Node.isText: Boolean
    inline get() = this.newIsText

/**
 * Gets a value indicating whether this node is an [Element].
 */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.isElement' instead.",
    replaceWith = ReplaceWith("this.isElement", "kotlinx.dom.isElement")
)
@DeprecatedSinceKotlin(warningSince = "1.4")
public val Node.isElement: Boolean
    inline get() = this.newIsElement
