/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmConstructor
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassConstructorImpl
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeVisibility
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirClassConstructorFactory {
    fun create(source: KmConstructor, containingClass: CirContainingClass, typeResolver: CirTypeResolver): CirClassConstructor {
        return create(
            annotations = CirAnnotationFactory.createAnnotations(source.flags, typeResolver, source::annotations),
            typeParameters = emptyList(), // TODO: nowhere to read constructor type parameters from
            visibility = decodeVisibility(source.flags),
            containingClass = containingClass,
            valueParameters = source.valueParameters.compactMap { CirValueParameterFactory.create(it, typeResolver) },
            hasStableParameterNames = !Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(source.flags),
            isPrimary = !Flag.Constructor.IS_SECONDARY(source.flags)
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        typeParameters: List<CirTypeParameter>,
        visibility: Visibility,
        containingClass: CirContainingClass,
        valueParameters: List<CirValueParameter>,
        hasStableParameterNames: Boolean,
        isPrimary: Boolean
    ): CirClassConstructor {
        return CirClassConstructorImpl(
            annotations = annotations,
            typeParameters = typeParameters,
            visibility = visibility,
            containingClass = containingClass,
            valueParameters = valueParameters,
            hasStableParameterNames = hasStableParameterNames,
            isPrimary = isPrimary
        )
    }
}
