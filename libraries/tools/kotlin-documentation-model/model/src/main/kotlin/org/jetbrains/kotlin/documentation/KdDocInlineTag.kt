/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A link to a declaration, e.g. a KDoc `[Foo]` link or a Javadoc `{@link Foo}`/`{@linkplain Foo}` tag. */
@Serializable
@SerialName("declarationLink")
data class KdDocDeclarationLink(
    /** `null` if the link's target could not be resolved. */
    val address: KdElementId?,
    val children: KdDocContentNode = emptyList(),
) : KdDocNode

/** A Javadoc `{@code text}` inline tag. */
@Serializable
@SerialName("inlineCode")
data class KdDocInlineCode(
    val children: KdDocContentNode = emptyList(),
    val params: Map<String, String> = emptyMap(),
) : KdDocNode

/** A Javadoc `{@inheritDoc}` inline tag. */
@Serializable
@SerialName("inheritDoc")
data class KdDocInheritTag(
    /** `null` if the inherited declaration could not be resolved. */
    val address: KdElementId?,
) : KdDocNode

/**
 * A Javadoc inline tag not otherwise represented in this model, e.g. `{@docRoot}`, `{@index}`,
 * `{@literal}`, `{@return}`, `{@summary}`, `{@systemProperty}`, `{@value}`, or a `{@snippet}`
 * (JEP 413; not modeled further yet, as its shape isn't finalized).
 */
@Serializable
@SerialName("unknownInlineTag")
data class KdDocUnknownInlineTag(val name: String, val content: String) : KdDocNode
