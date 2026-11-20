/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/**
 * A Kotlin code fragment with optional [links] to other declarations, used e.g. for default
 * parameter values, constant initializers, and `ReplaceWith` expressions.
 *
 * [text] always represents real Kotlin code, so it can be syntax-highlighted as such even
 * without resolving [links].
 */
@Serializable
data class KdExpression(
    val text: String,
    val links: List<KdExpressionLink>,
)

/** A link from a range of [KdExpression.text] to the declaration it references. */
@Serializable
data class KdExpressionLink(
    /** The offset of the first character of the link in [KdExpression.text]. */
    val start: Int,
    /** The offset right after the last character of the link in [KdExpression.text]. */
    val end: Int,
    val elementId: KdElementId,
)
