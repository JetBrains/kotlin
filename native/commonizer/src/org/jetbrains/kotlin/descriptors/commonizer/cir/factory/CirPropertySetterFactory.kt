/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmProperty
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.setterAnnotations
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirPropertySetterImpl
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeVisibility
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirPropertySetterFactory {
    private val interner = Interner<CirPropertySetter>()

    fun create(source: PropertySetterDescriptor): CirPropertySetter = create(
        annotations = source.annotations.compactMap(CirAnnotationFactory::create),
        parameterAnnotations = source.valueParameters[0].annotations.compactMap(CirAnnotationFactory::create),
        visibility = source.visibility,
        isDefault = source.isDefault,
        isExternal = source.isExternal,
        isInline = source.isInline
    )

    fun create(source: KmProperty, typeResolver: CirTypeResolver): CirPropertySetter? {
        if (!Flag.Property.HAS_SETTER(source.flags))
            return null

        val setterFlags = source.setterFlags

        return create(
            annotations = CirAnnotationFactory.createAnnotations(setterFlags, typeResolver, source::setterAnnotations),
            parameterAnnotations = source.setterParameter?.let { setterParameter ->
                CirAnnotationFactory.createAnnotations(setterParameter.flags, typeResolver, setterParameter::annotations)
            }.orEmpty(),
            visibility = decodeVisibility(setterFlags),
            isDefault = !Flag.PropertyAccessor.IS_NOT_DEFAULT(setterFlags),
            isExternal = Flag.PropertyAccessor.IS_EXTERNAL(setterFlags),
            isInline = Flag.PropertyAccessor.IS_INLINE(setterFlags)
        )
    }

    fun create(
        annotations: List<CirAnnotation>,
        parameterAnnotations: List<CirAnnotation>,
        visibility: DescriptorVisibility,
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
    inline fun createDefaultNoAnnotations(visibility: DescriptorVisibility): CirPropertySetter = create(
        annotations = emptyList(),
        parameterAnnotations = emptyList(),
        visibility = visibility,
        isDefault = visibility == DescriptorVisibilities.PUBLIC,
        isExternal = false,
        isInline = false
    )
}
