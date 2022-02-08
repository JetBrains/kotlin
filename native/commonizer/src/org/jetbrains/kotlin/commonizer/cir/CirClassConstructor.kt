/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.descriptors.Visibility

interface CirClassConstructor :
    CirDeclaration,
    CirHasAnnotations,
    CirHasTypeParameters,
    CirHasVisibility,
    CirMaybeCallableMemberOfClass,
    CirCallableMemberWithParameters {

    val isPrimary: Boolean
    override val containingClass: CirContainingClass // non-nullable

    override fun withContainingClass(containingClass: CirContainingClass): CirClassConstructor

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun create(
            annotations: List<CirAnnotation>,
            typeParameters: List<CirTypeParameter>,
            visibility: Visibility,
            containingClass: CirContainingClass,
            valueParameters: List<CirValueParameter>,
            hasStableParameterNames: Boolean,
            isPrimary: Boolean
        ): CirClassConstructor = CirClassConstructorImpl(
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

data class CirClassConstructorImpl(
    override val annotations: List<CirAnnotation>,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val containingClass: CirContainingClass,
    override var valueParameters: List<CirValueParameter>,
    override var hasStableParameterNames: Boolean,
    override val isPrimary: Boolean
) : CirClassConstructor {
    override fun withContainingClass(containingClass: CirContainingClass): CirClassConstructor {
        return copy(containingClass = containingClass)
    }
}
