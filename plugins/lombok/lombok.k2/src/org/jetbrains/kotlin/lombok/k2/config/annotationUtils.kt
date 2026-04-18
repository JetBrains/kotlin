/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.config

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.utils.trimToNull
import org.jetbrains.kotlin.name.Name

@DirectDeclarationsAccess
fun FirAnnotation?.getVisibility(field: Name, defaultAccessLevel: AccessLevel = AccessLevel.PUBLIC): Visibility? {
    val value = getArgumentAsString(field)?.let { arg -> AccessLevel.entries.find { it.name == arg } } ?: defaultAccessLevel
    return value.toVisibility()
}

@DirectDeclarationsAccess
private fun FirAnnotation?.getArgumentAsString(field: Name): String? {
    val argument = this?.findArgumentByName(field) ?: return null
    return when (argument) {
        is FirLiteralExpression -> argument.value as? String
        is FirEnumEntryDeserializedAccessExpression -> argument.enumEntryName.identifier
        is FirQualifiedAccessExpression -> {
            @OptIn(UnsafeExpressionUtility::class)
            val symbol = argument.toResolvedCallableSymbolUnsafe()
            if (symbol is FirEnumEntrySymbol) {
                symbol.callableId.callableName.identifier
            } else {
                (argument.calleeReference as? FirSimpleNamedReference)?.name?.identifier
            }
        }
        else -> null
    }
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
    val TOPIC = Name.identifier("topic")

    const val FLUENT_CONFIG = "lombok.accessors.fluent"
    const val CHAIN_CONFIG = "lombok.accessors.chain"
    const val PREFIX_CONFIG = "lombok.accessors.prefix"
    const val NO_IS_PREFIX_CONFIG = "lombok.getter.noIsPrefix"
    const val BUILDER_CLASS_NAME_CONFIG = "lombok.builder.className"
    const val FIELD_NAME_CONFIG = "lombok.log.fieldName"
    const val FIELD_IS_STATIC_CONFIG = "lombok.log.fieldIsStatic"
    const val LOG_FLAG_USAGE_CONFIG = "lombok.log.flagUsage"

    val INCLUDE_FIELD_NAMES = Name.identifier("includeFieldNames")
    val CALL_SUPER = Name.identifier("callSuper")
    val DO_NOT_USE_GETTERS = Name.identifier("doNotUseGetters")
    val ONLY_EXPLICITLY_INCLUDED = Name.identifier("onlyExplicitlyIncluded")
    val EXCLUDE = Name.identifier("exclude")
    val INCLUDE_NAME = Name.identifier("name")

    const val TO_STRING_INCLUDE_FIELD_NAMES_CONFIG = "lombok.toString.includeFieldNames"
    const val TO_STRING_CALL_SUPER_CONFIG = "lombok.toString.callSuper"
    const val TO_STRING_DO_NOT_USE_GETTERS_CONFIG = "lombok.toString.doNotUseGetters"
    const val TO_STRING_ONLY_EXPLICITLY_INCLUDED_CONFIG = "lombok.toString.onlyExplicitlyIncluded"
    const val TO_STRING_FLAG_USAGE_CONFIG = "lombok.toString.flagUsage"
}

enum class FlagUsageValue {
    Warning,
    Error,
}
