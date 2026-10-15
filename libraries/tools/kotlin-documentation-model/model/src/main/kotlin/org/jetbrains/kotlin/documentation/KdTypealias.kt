/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A type alias. [declarations] lists the "aliased" declarations, materialized in the model so
 * that expect/actual mapping works correctly for an `actual typealias`.
 */
@Serializable
@SerialName("typealias")
data class KdTypealias(
    val name: String,

    val typeParameters: List<KdTypeParameter>,
    val underlyingType: KdType,

    val declarations: List<KdElementId>,

    val visibility: KdVisibility,

    val since: String?,
    val deprecated: KdDeprecated?,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
) : KdElement()
