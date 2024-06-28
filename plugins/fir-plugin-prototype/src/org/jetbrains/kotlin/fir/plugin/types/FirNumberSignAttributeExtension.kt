/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirTypeAttributeExtension
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirNumberSignAttributeExtension(session: FirSession) : FirTypeAttributeExtension(session) {
    companion object {
        private val PACKAGE_FQN = FqName("org.jetbrains.kotlin.fir.plugin")
        private val PositiveClassId = ClassId(PACKAGE_FQN, Name.identifier("Positive"))
        private val NegativeClassId = ClassId(PACKAGE_FQN, Name.identifier("Negative"))
    }

    override fun extractAttributeFromAnnotation(annotation: FirAnnotation): ConeAttribute<*>? {
        val sign = when (annotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId) {
            PositiveClassId -> ConeNumberSignAttribute.Sign.Positive
            NegativeClassId -> ConeNumberSignAttribute.Sign.Negative
            else -> return null
        }
        return ConeNumberSignAttribute.fromSign(sign)
    }

    override fun convertAttributeToAnnotation(attribute: ConeAttribute<*>): FirAnnotation? {
        if (attribute !is ConeNumberSignAttribute) return null
        val classId = when (attribute.sign) {
            ConeNumberSignAttribute.Sign.Positive -> PositiveClassId
            ConeNumberSignAttribute.Sign.Negative -> NegativeClassId
        }
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(
                    classId.toLookupTag(),
                    ConeTypeProjection.EMPTY_ARRAY,
                    isNullable = false
                )
            }
            argumentMapping = FirEmptyAnnotationArgumentMapping
        }
    }
}
