/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

fun renderTypeForUnsafeNumberAnnotation(type: CirType): String = when (type) {
    is CirSimpleType -> renderSimpleType(type)
    is CirFlexibleType -> renderFlexibleType(type)
}

private fun renderFlexibleType(type: CirFlexibleType): String =
    "${renderTypeForUnsafeNumberAnnotation(type.lowerBound)}..${renderTypeForUnsafeNumberAnnotation(type.upperBound)}"

private fun renderSimpleType(type: CirSimpleType): String {
    return when (type) {
        is CirTypeAliasType -> renderSimpleType(type.expandedType())
        is CirClassType -> "${type.classifierId.toQualifiedNameString()}${renderArguments(type.arguments)}${type.renderNullable()}"
        is CirTypeParameterType -> "$TYPE_PARAMETER_TYPE_PREFIX${type.index}${type.renderNullable()}"
    }
}

private fun renderArguments(arguments: List<CirTypeProjection>): String {
    return arguments.ifNotEmpty { joinToString(prefix = "<", postfix = ">") { renderTypeArgument(it) } }.orEmpty()
}

private fun renderTypeArgument(typeArgument: CirTypeProjection): String {
    return when (typeArgument) {
        is CirRegularTypeProjection -> "${renderVariance(typeArgument.projectionKind)}${renderTypeForUnsafeNumberAnnotation(typeArgument.type)}"
        CirStarTypeProjection -> STAR_PROJECTION
    }
}

private fun renderVariance(variance: Variance): String = "$variance ".takeIf { it.isNotBlank() }.orEmpty()

private fun CirSimpleType.renderNullable(): String = isMarkedNullable.ifTrue { NULLABLE }.orEmpty()

private const val NULLABLE: String = "?"
private const val STAR_PROJECTION: String = "*"
private const val TYPE_PARAMETER_TYPE_PREFIX = "#"
