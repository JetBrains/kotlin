/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A function. Also used to represent a property's `getter`/`setter` when non-default; such a
 * function never has [overridens] populated.
 */
@Serializable
@SerialName("function")
data class KdFunction(
    val name: String,

    val isCompanion: Boolean,
    val isExternal: Boolean,
    val isSuspend: Boolean,
    val isOperator: Boolean,
    val isInfix: Boolean,
    val isInline: Boolean,
    val isTailRec: Boolean,

    /** The ID of the equivalent JDK/Kotlin declaration this one is mapped to; only present for some stdlib declarations. */
    val mappedTo: KdElementId?,

    val returnType: KdType,
    val typeParameters: List<KdTypeParameter>,
    val valueParameters: List<KdValueParameter>,
    val extensionReceiverParameter: KdExtensionReceiverParameter?,
    val contextParameters: List<KdContextParameter>,

    val overridens: List<KdElementId>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val since: String?,
    val deprecated: KdDeprecated?,
    val throws: List<KdThrows>,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
) : KdElement()
