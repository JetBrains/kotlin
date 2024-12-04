/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.Interner

interface CirPropertyGetter : CirPropertyAccessor {
    companion object {
        // optimization for speed
        val DEFAULT_NO_ANNOTATIONS: CirPropertyGetter

        fun createInterned(
            annotations: List<CirAnnotation>,
            isDefault: Boolean,
            isInline: Boolean
        ): CirPropertyGetter = interner.intern(
            CirPropertyGetterInternedImpl(
                annotations = annotations,
                isDefault = isDefault,
                isInline = isInline
            )
        )

        private val interner = Interner<CirPropertyGetterInternedImpl>()

        init {
            DEFAULT_NO_ANNOTATIONS = createInterned(
                annotations = emptyList(),
                isDefault = true,
                isInline = false
            )
        }
    }
}

private data class CirPropertyGetterInternedImpl(
    override val annotations: List<CirAnnotation>,
    override val isDefault: Boolean,
    override val isInline: Boolean
) : CirPropertyGetter
