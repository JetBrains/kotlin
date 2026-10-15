/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An annotation present on a declaration. Only annotations themselves annotated with
 * `@MustBeDocumented` are included in the model.
 *
 * [arguments] contains only the arguments explicitly specified in the source code; default
 * values of annotation parameters are not included, as they might not always be available.
 */
@Serializable
data class KdAnnotation(
    val classLikeId: KdElementId,
    val arguments: Map<String, KdAnnotationArgumentValue>,
)

/** The value of a single [KdAnnotation] argument. */
@Serializable
sealed class KdAnnotationArgumentValue {
    @Serializable
    @SerialName("const")
    data class Const(val expression: KdConstExpression) : KdAnnotationArgumentValue()

    @Serializable
    @SerialName("enumEntry")
    data class EnumEntry(val enumEntryId: KdElementId) : KdAnnotationArgumentValue()

    @Serializable
    @SerialName("classLikeValue")
    data class Classlike(val classLikeId: KdElementId) : KdAnnotationArgumentValue()

    @Serializable
    @SerialName("annotation")
    data class Annotation(val annotation: KdAnnotation) : KdAnnotationArgumentValue()

    @Serializable
    @SerialName("array")
    data class Array(val elements: List<KdAnnotationArgumentValue>) : KdAnnotationArgumentValue()
}
