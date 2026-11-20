/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A property, in a broad sense: an ordinary Kotlin property, a `const val`, an enum entry, a
 * Java field, or a Java synthetic property (combining Java getter/setter methods into one
 * property).
 *
 * [getter] and [setter] are only present if non-default (e.g. have annotations, a different
 * visibility, or represent the accessor of a Java synthetic property); they are always `null`
 * for `const val`, Java fields, and enum entries.
 */
@Serializable
@SerialName("property")
data class KdProperty(
    val name: String,

    val isInline: Boolean,
    val isCompanion: Boolean,
    val isExternal: Boolean,
    val isSuspend: Boolean,

    val isMutable: Boolean,
    /** Whether this property is declared in the primary constructor; also covers a Java record's canonical constructor. */
    val isFromPrimaryConstructor: Boolean,

    val constExpression: KdConstExpression?,
    val getter: KdElementId?,
    val setter: KdElementId?,

    /** The ID of the equivalent JDK/Kotlin declaration this one is mapped to; only present for some stdlib declarations. */
    val mappedTo: KdElementId?,

    val type: KdType,
    val typeParameters: List<KdTypeParameter>,
    val extensionReceiverParameter: KdExtensionReceiverParameter?,
    val contextParameters: List<KdContextParameter>,

    val overridens: List<KdElementId>,

    /** The visibility of the `getter`, if this property has a non-default getter/setter. */
    val visibility: KdVisibility,
    val modality: KdModality,

    val throws: List<KdThrows>,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
) : KdElement()
