/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A base type for a deeply-parsed HTML block embedded in KDoc/Javadoc content. */
@Serializable
sealed interface KdDocHtmlNode : KdDocNode

/** An HTML element, e.g. `<iframe src="...">...</iframe>`. */
@Serializable
@SerialName("htmlElement")
data class KdDocHtmlElement(
    val tagName: String,
    val attributes: List<KdDocHtmlAttribute> = emptyList(),
    /** `null` for a self-closing element, e.g. `<br/>`; an empty list for an element with no content, e.g. `<div></div>`. */
    val children: List<KdDocHtmlNode>? = emptyList(),
) : KdDocHtmlNode

/** A single attribute of a [KdDocHtmlElement]; a `null` [value] represents a boolean attribute, e.g. `disabled`. */
@Serializable
data class KdDocHtmlAttribute(val name: String, val value: String?)

/** A run of raw text inside HTML content. */
@Serializable
@SerialName("htmlText")
data class KdDocHtmlText(val content: String) : KdDocHtmlNode

/** An HTML comment, e.g. `<!-- comment -->`. */
@Serializable
@SerialName("htmlComment")
data class KdDocHtmlComment(val content: String) : KdDocHtmlNode

/** An HTML processing instruction, e.g. `<?xml-stylesheet ... ?>`. */
@Serializable
@SerialName("htmlProcessingInstruction")
data class KdDocHtmlProcessingInstruction(val content: String) : KdDocHtmlNode

/** An HTML declaration, e.g. `<!DOCTYPE html>`. */
@Serializable
@SerialName("htmlDeclaration")
data class KdDocHtmlDeclaration(val content: String) : KdDocHtmlNode

/** An HTML CDATA section, e.g. `<![CDATA[ ... ]]>`. */
@Serializable
@SerialName("htmlCData")
data class KdDocHtmlCData(val content: String) : KdDocHtmlNode
