/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.types.*

internal fun KtAnalysisSession.getTypeReferenceFrom(type: KtType): TypeReference =
    getTypeReferenceFromPossiblyRecursive(type, emptyList())


// see `deep recursive typebound #1342` test
private fun KtAnalysisSession.getTypeReferenceFromPossiblyRecursive(
    type: KtType,
    paramTrace: List<KtType>
): TypeReference {
    if (type is KtTypeParameterType) {
        // compare by symbol since, e.g. T? and T have the different KtType, but the same type parameter
        paramTrace.indexOfFirst { it is KtTypeParameterType && type.symbol == it.symbol }
            .takeIf { it >= 0 }
            ?.let { return RecursiveType(it) }
    }

    return when (type) {
        is KtNonErrorClassType -> TypeConstructor(
            type.classId.asFqNameString(),
            type.ownTypeArguments.map {
                getTypeReferenceFromTypeProjection(
                    it,
                    paramTrace
                )
            }
        )

        is KtTypeParameterType -> {
            val upperBoundsOrNullableAny =
                type.symbol.upperBounds.takeIf { it.isNotEmpty() } ?: listOf(this.builtinTypes.NULLABLE_ANY)

            TypeParam(bounds = upperBoundsOrNullableAny.map {
                getTypeReferenceFromPossiblyRecursive(
                    it,
                    paramTrace + type
                )
            })
        }

        is KtClassErrorType -> TypeConstructor("$ERROR_CLASS_NAME $type", emptyList())

        is KtDefinitelyNotNullType -> getTypeReferenceFromPossiblyRecursive(
            type.original,
            paramTrace
        )

        is KtDynamicType -> TypeConstructor("[dynamic]", emptyList())
        is KtTypeErrorType -> TypeConstructor("$ERROR_CLASS_NAME $type", emptyList())
        // Non-denotable types, see https://kotlinlang.org/spec/type-system.html#type-kinds

        // By the definition [flexible types](https://kotlinlang.org/spec/type-system.html#flexible-types),
        // we can take any type between lower and upper bounds
        // In Dokka K1, a lower bound is taken
        // For example, most Java types T in Kotlin are flexible (T..T?) or T!
        // see https://github.com/JetBrains/kotlin/blob/master/spec-docs/flexible-java-types.md (warn: it is not official spec)
        is KtFlexibleType -> getTypeReferenceFromPossiblyRecursive(
            type.lowerBound,
            paramTrace
        )
        is KtCapturedType -> throw NotImplementedError()
        is KtIntegerLiteralType -> throw NotImplementedError()
        is KtIntersectionType -> throw NotImplementedError()
    }.let {
        if (type.isMarkedNullable) Nullable(it) else it
    }

}

private fun KtAnalysisSession.getTypeReferenceFromTypeProjection(
    typeProjection: KtTypeProjection,
    paramTrace: List<KtType>
): TypeReference =
    when (typeProjection) {
        is KtStarTypeProjection -> StarProjection
        is KtTypeArgumentWithVariance -> getTypeReferenceFromPossiblyRecursive(typeProjection.type, paramTrace)
    }
