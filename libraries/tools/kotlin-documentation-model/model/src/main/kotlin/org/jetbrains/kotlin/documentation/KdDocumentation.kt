/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/**
 * A base type for a single node of parsed KDoc/Javadoc content: a Markdown block or inline
 * element ([KdDocParagraph], [KdDocText], ...), a deeply-parsed HTML node ([KdDocHtmlNode]), or
 * an inline documentation tag ([KdDocDeclarationLink], [KdDocInlineCode], ...).
 */
@Serializable
sealed interface KdDocNode

/** A sequence of sibling [KdDocNode]s, e.g. the children of a block node or the body of a [KdDocTag]. */
typealias KdDocContentNode = List<KdDocNode>

/**
 * The parsed documentation (KDoc or Javadoc) of a declaration or parameter: [content] is the main
 * body, [tags] are block tags such as `@param` or `@see`.
 */
@Serializable
data class KdDocumentation(
    val content: KdDocContentNode,
    val tags: List<KdDocTag>,
) {
    companion object {
        /** The documentation of a declaration without any KDoc/Javadoc comment. */
        val EMPTY: KdDocumentation = KdDocumentation(emptyList(), emptyList())
    }
}
