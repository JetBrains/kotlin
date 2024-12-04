/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

interface CirPropertySetter : CirDeclaration, CirPropertyAccessor, CirHasVisibility {
    val parameterAnnotations: List<CirAnnotation>

    companion object {
        fun createInterned(
            annotations: List<CirAnnotation>,
            parameterAnnotations: List<CirAnnotation>,
            visibility: Visibility,
            isDefault: Boolean,
            isInline: Boolean
        ): CirPropertySetter = interner.intern(
            CirPropertySetterInternedImpl(
                annotations = annotations,
                parameterAnnotations = parameterAnnotations,
                visibility = visibility,
                isDefault = isDefault,
                isInline = isInline
            )
        )

        @Suppress("NOTHING_TO_INLINE")
        inline fun createDefaultNoAnnotations(visibility: Visibility): CirPropertySetter = createInterned(
            annotations = emptyList(),
            parameterAnnotations = emptyList(),
            visibility = visibility,
            isDefault = visibility == Visibilities.Public,
            isInline = false
        )

        private val interner = Interner<CirPropertySetterInternedImpl>()
    }
}

private data class CirPropertySetterInternedImpl(
    override val annotations: List<CirAnnotation>,
    override val parameterAnnotations: List<CirAnnotation>,
    override val visibility: Visibility,
    override val isDefault: Boolean,
    override val isInline: Boolean
) : CirPropertySetter
