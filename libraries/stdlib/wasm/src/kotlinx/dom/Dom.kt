/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.dom

import org.w3c.dom.*

/**
 * Gets a value indicating whether this node is a TEXT_NODE or a CDATA_SECTION_NODE.
 */
@SinceKotlin("1.4")
public val Node.isText: Boolean
    get() = nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE

/**
 * Gets a value indicating whether this node is an [Element].
 */
@SinceKotlin("1.4")
public val Node.isElement: Boolean
    get() = nodeType == Node.ELEMENT_NODE
