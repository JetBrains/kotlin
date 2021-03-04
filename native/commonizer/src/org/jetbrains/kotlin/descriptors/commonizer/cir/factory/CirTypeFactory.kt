/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.descriptors.commonizer.metadata.CirTypeAliasExpander
import org.jetbrains.kotlin.descriptors.commonizer.metadata.CirTypeAliasExpansion
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.types.Variance

object CirTypeFactory {
    fun create(source: KmType, typeResolver: CirTypeResolver): CirType {
        @Suppress("NAME_SHADOWING")
        val source = source.abbreviatedType ?: source
        val isMarkedNullable = Flag.Type.IS_NULLABLE(source.flags)

        return when (val classifier = source.classifier) {
            is KmClassifier.Class -> {
                val classId = CirEntityId.create(classifier.name)

                val outerType = source.outerType?.let { outerType ->
                    val outerClassType = create(outerType, typeResolver)
                    check(outerClassType is CirClassType) { "Outer type of $classId is not a class: $outerClassType" }
                    outerClassType
                }

                val clazz: CirProvided.Class = typeResolver.resolveClassifier(classId)

                CirClassType.createInterned(
                    classId = classId,
                    outerType = outerType,
                    visibility = clazz.visibility,
                    arguments = createArguments(source.arguments, typeResolver),
                    isMarkedNullable = isMarkedNullable
                )
            }
            is KmClassifier.TypeAlias -> {
                val typeAliasId = CirEntityId.create(classifier.name)

                val arguments = createArguments(source.arguments, typeResolver)

                val underlyingType = CirTypeAliasExpander.expand(
                    CirTypeAliasExpansion.create(typeAliasId, arguments, isMarkedNullable, typeResolver)
                )

                CirTypeAliasType.createInterned(
                    typeAliasId = typeAliasId,
                    underlyingType = underlyingType,
                    arguments = arguments,
                    isMarkedNullable = isMarkedNullable
                )
            }
            is KmClassifier.TypeParameter -> {
                CirTypeParameterType.createInterned(
                    index = typeResolver.resolveTypeParameterIndex(classifier.id),
                    isMarkedNullable = isMarkedNullable
                )
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun decodeVariance(variance: KmVariance): Variance = when (variance) {
        KmVariance.INVARIANT -> Variance.INVARIANT
        KmVariance.IN -> Variance.IN_VARIANCE
        KmVariance.OUT -> Variance.OUT_VARIANCE
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun createArguments(arguments: List<KmTypeProjection>, typeResolver: CirTypeResolver): List<CirTypeProjection> {
        return arguments.compactMap { argument ->
            val variance = argument.variance ?: return@compactMap CirStarTypeProjection
            val argumentType = argument.type ?: return@compactMap CirStarTypeProjection

            CirTypeProjectionImpl(
                projectionKind = decodeVariance(variance),
                type = create(argumentType, typeResolver)
            )
        }
    }
}
