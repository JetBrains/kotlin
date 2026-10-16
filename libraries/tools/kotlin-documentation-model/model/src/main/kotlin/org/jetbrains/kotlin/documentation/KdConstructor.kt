/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A constructor of a [KdClass]. */
@Serializable
@SerialName("constructor")
data class KdConstructor(
    /** Whether this is the primary constructor; also covers the canonical constructor of a Java record. */
    val isPrimary: Boolean,
    val isExternal: Boolean,

    /** The ID of the equivalent JDK/Kotlin declaration this one is mapped to; only present for some stdlib declarations. */
    val mappedTo: KdElementId?,

    val typeParameters: List<KdTypeParameter>,
    val valueParameters: List<KdValueParameter>,

    val visibility: KdVisibility,

    val since: String?,
    val deprecated: KdDeprecated?,
    val throws: List<KdThrows>,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
) : KdElement()
