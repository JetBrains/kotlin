/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A class, interface, object, enum class, annotation class, or Java record.
 *
 * Whether this is, e.g., a `class` or an `interface` is not (yet) tracked by a dedicated
 * property, only by the `kind` discriminator used during JSON serialization; see the "class
 * likes" kinds in the KDM spec. Splitting this model per such sub-kind is an open question.
 */
@Serializable
@SerialName("class")
data class KdClass(
    val name: String,

    val isCompanion: Boolean,
    val isData: Boolean,
    val isValue: Boolean,
    val isFun: Boolean,
    val isInner: Boolean,
    val isExternal: Boolean,
    /** Whether this is an error class, see the Kotlin rich-errors feature. */
    val isError: Boolean,

    /** The ID of the equivalent JDK/Kotlin declaration this one is mapped to; only present for some stdlib declarations. */
    val mappedTo: KdElementId?,

    val typeParameters: List<KdTypeParameter>,
    val superTypes: List<KdType>,

    val declarations: List<KdElementId>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val since: String?,
    val deprecated: KdDeprecated?,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
) : KdElement
