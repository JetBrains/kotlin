/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Block-level nodes ---

/** A paragraph; also used for an HTML `<p>`. */
@Serializable
@SerialName("paragraph")
data class KdDocParagraph(val children: KdDocContentNode) : KdDocNode

/** A Markdown heading, e.g. `# Title`; [level] is 1 to 6. */
@Serializable
@SerialName("heading")
data class KdDocHeading(val level: Int, val children: KdDocContentNode) : KdDocNode

/** A Markdown block quote, e.g. `> quoted text`. */
@Serializable
@SerialName("blockQuote")
data class KdDocBlockQuote(val children: KdDocContentNode) : KdDocNode

/** A Markdown bullet list. */
@Serializable
@SerialName("unorderedList")
data class KdDocUnorderedList(val items: List<KdDocListItem>) : KdDocNode

/** A Markdown numbered list. */
@Serializable
@SerialName("orderedList")
data class KdDocOrderedList(val items: List<KdDocListItem>) : KdDocNode

/** A single item of a [KdDocUnorderedList] or [KdDocOrderedList]. */
@Serializable
@SerialName("listItem")
data class KdDocListItem(val children: KdDocContentNode) : KdDocNode

/** A fenced code block, e.g. ` ```kotlin ... ``` `; [language] is the fence's info string, if any. */
@Serializable
@SerialName("codeBlock")
data class KdDocCodeBlock(val language: String?, val content: String) : KdDocNode

/** A Markdown thematic break, e.g. `---`. */
@Serializable
@SerialName("thematicBreak")
data object KdDocThematicBreak : KdDocNode

// --- Inline nodes ---

/** A run of plain text. */
@Serializable
@SerialName("text")
data class KdDocText(val value: String) : KdDocNode

/** Emphasized (`*italic*`) text. */
@Serializable
@SerialName("emphasis")
data class KdDocEmphasis(val children: KdDocContentNode) : KdDocNode

/** Strongly emphasized (`**bold**`) text. */
@Serializable
@SerialName("strong")
data class KdDocStrong(val children: KdDocContentNode) : KdDocNode

/** An inline code span, e.g. `` `code` ``. */
@Serializable
@SerialName("codeSpan")
data class KdDocCodeSpan(val content: String) : KdDocNode

/** A Markdown hyperlink, e.g. `[label](destination)`. For links to declarations, see [KdDocDeclarationLink]. */
@Serializable
@SerialName("link")
data class KdDocLink(
    val destination: String,
    val label: String?,
    val children: KdDocContentNode,
) : KdDocNode

/** A Markdown image, e.g. `![alt](destination "title")`. */
@Serializable
@SerialName("image")
data class KdDocImage(
    val destination: String,
    val title: String?,
    val alt: KdDocContentNode,
) : KdDocNode

/** A bare URL or email address automatically turned into a link, e.g. `<https://example.com>`. */
@Serializable
@SerialName("autolink")
data class KdDocAutolink(val destination: String) : KdDocNode

/** An explicit line break within a paragraph, e.g. two trailing spaces or `\` at the end of a line. */
@Serializable
@SerialName("hardLineBreak")
data object KdDocHardLineBreak : KdDocNode

/** A regular line break within a paragraph that doesn't force a new line when rendered. */
@Serializable
@SerialName("softLineBreak")
data object KdDocSoftLineBreak : KdDocNode
