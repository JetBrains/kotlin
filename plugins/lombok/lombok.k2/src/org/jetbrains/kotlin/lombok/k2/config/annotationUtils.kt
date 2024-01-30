/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.config

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.utils.trimToNull
import org.jetbrains.kotlin.name.Name

fun FirAnnotation.getAccessLevel(field: Name = LombokConfigNames.VALUE): AccessLevel {
    val value = getArgumentAsString(field) ?: return AccessLevel.PUBLIC
    return AccessLevel.valueOf(value)
}

private fun FirAnnotation.getArgumentAsString(field: Name): String? {
    val argument = findArgumentByName(field) ?: return null
    return when (argument) {
        is FirLiteralExpression<*> -> argument.value as? String
        is FirEnumEntryDeserializedAccessExpression -> argument.enumEntryName.identifier
        is FirQualifiedAccessExpression -> {
            @OptIn(UnsafeExpressionUtility::class)
            val symbol = argument.toResolvedCallableSymbolUnsafe()
            if (symbol is FirEnumEntrySymbol) {
                symbol.callableId.callableName.identifier
            } else {
                null
            }
        }
        else -> null
    }
}

fun getVisibility(annotation: FirAnnotation, field: Name = LombokConfigNames.VALUE): Visibility {
    return annotation.getAccessLevel(field).toVisibility()
}

fun FirAnnotation.getNonBlankStringArgument(name: Name): String? = getStringArgument(name)?.trimToNull()

object LombokConfigNames {
    val VALUE = Name.identifier("value")
    val FLUENT = Name.identifier("fluent")
    val CHAIN = Name.identifier("chain")
    val PREFIX = Name.identifier("prefix")
    val ACCESS = Name.identifier("access")
    val STATIC_NAME = Name.identifier("staticName")
    val STATIC_CONSTRUCTOR = Name.identifier("staticConstructor")

    val BUILDER_CLASS_NAME = Name.identifier("builderClassName")
    val BUILD_METHOD_NAME = Name.identifier("buildMethodName")
    val BUILDER_METHOD_NAME = Name.identifier("builderMethodName")
    val TO_BUILDER = Name.identifier("toBuilder")
    val SETTER_PREFIX = Name.identifier("setterPrefix")
    val IGNORE_NULL_COLLECTIONS = Name.identifier("ignoreNullCollections")


    const val FLUENT_CONFIG = "lombok.accessors.fluent"
    const val CHAIN_CONFIG = "lombok.accessors.chain"
    const val PREFIX_CONFIG = "lombok.accessors.prefix"
    const val NO_IS_PREFIX_CONFIG = "lombok.getter.noIsPrefix"
    const val BUILDER_CLASS_NAME_CONFIG = "lombok.builder.className"
}
