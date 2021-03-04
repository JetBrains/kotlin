/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmProperty
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.setterAnnotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeVisibility

object CirPropertySetterFactory {
    fun create(source: KmProperty, typeResolver: CirTypeResolver): CirPropertySetter? {
        if (!Flag.Property.HAS_SETTER(source.flags))
            return null

        val setterFlags = source.setterFlags

        return CirPropertySetter.createInterned(
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
}
