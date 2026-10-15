/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/**
 * Deprecation information of a declaration, from either a `@Deprecated` annotation (Kotlin) or a
 * `@Deprecated` annotation and/or `@deprecated` documentation tag (Java).
 */
@Serializable
data class KdDeprecated(
    val since: String?,
    val message: KdDocumentation = KdDocumentation.EMPTY,
    val level: KdDeprecationLevel,
    val replaceWith: KdReplaceWith?,
)

/** The severity of a deprecation. There is no `HIDDEN` level, as such declarations are filtered out of the model entirely. */
@Serializable
enum class KdDeprecationLevel { WARNING, ERROR }

/** A suggested code replacement for a deprecated declaration, as in `@Deprecated(replaceWith = ReplaceWith(...))`. */
@Serializable
data class KdReplaceWith(
    val imports: List<String>,
    val expression: KdExpression,
)
