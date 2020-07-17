/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertyGetter
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirPropertyGetterImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner

object CirPropertyGetterFactory {
    private val interner = Interner<CirPropertyGetter>()

    // speed optimization
    val DEFAULT_NO_ANNOTATIONS: CirPropertyGetter = create(
        annotations = emptyList(),
        isDefault = true,
        isExternal = false,
        isInline = false
    )

    fun create(source: PropertyGetterDescriptor): CirPropertyGetter {
        return if (source.isDefault && source.annotations.isEmpty())
            DEFAULT_NO_ANNOTATIONS
        else
            create(
                annotations = source.annotations.map(CirAnnotationFactory::create),
                isDefault = source.isDefault,
                isExternal = source.isExternal,
                isInline = source.isInline
            )
    }

    fun create(
        annotations: List<CirAnnotation>,
        isDefault: Boolean,
        isExternal: Boolean,
        isInline: Boolean
    ): CirPropertyGetter {
        return interner.intern(
            CirPropertyGetterImpl(
                annotations = annotations,
                isDefault = isDefault,
                isExternal = isExternal,
                isInline = isInline
            )
        )
    }
}
