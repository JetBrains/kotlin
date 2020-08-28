/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirPropertySetterImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner

object CirPropertySetterFactory {
    private val interner = Interner<CirPropertySetter>()

    fun create(source: PropertySetterDescriptor): CirPropertySetter = create(
        annotations = source.annotations.map(CirAnnotationFactory::create),
        parameterAnnotations = source.valueParameters[0].annotations.map(CirAnnotationFactory::create),
        visibility = source.visibility,
        isDefault = source.isDefault,
        isExternal = source.isExternal,
        isInline = source.isInline
    )

    fun create(
        annotations: List<CirAnnotation>,
        parameterAnnotations: List<CirAnnotation>,
        visibility: Visibility,
        isDefault: Boolean,
        isExternal: Boolean,
        isInline: Boolean
    ): CirPropertySetter {
        return interner.intern(
            CirPropertySetterImpl(
                annotations = annotations,
                parameterAnnotations = parameterAnnotations,
                visibility = visibility,
                isDefault = isDefault,
                isExternal = isExternal,
                isInline = isInline
            )
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun createDefaultNoAnnotations(visibility: Visibility): CirPropertySetter = create(
        annotations = emptyList(),
        parameterAnnotations = emptyList(),
        visibility = visibility,
        isDefault = visibility == Visibilities.PUBLIC,
        isExternal = false,
        isInline = false
    )
}
