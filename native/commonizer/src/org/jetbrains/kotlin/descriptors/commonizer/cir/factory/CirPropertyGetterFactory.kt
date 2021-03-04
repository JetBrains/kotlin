/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmProperty
import kotlinx.metadata.klib.getterAnnotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertyGetter

object CirPropertyGetterFactory {
    fun create(source: KmProperty, typeResolver: CirTypeResolver): CirPropertyGetter? {
        if (!Flag.Property.HAS_GETTER(source.flags))
            return null

        val getterFlags = source.getterFlags

        val isDefault = !Flag.PropertyAccessor.IS_NOT_DEFAULT(getterFlags)
        val annotations = CirAnnotationFactory.createAnnotations(getterFlags, typeResolver, source::getterAnnotations)

        if (isDefault && annotations.isEmpty())
            return CirPropertyGetter.DEFAULT_NO_ANNOTATIONS

        return CirPropertyGetter.createInterned(
            annotations = annotations,
            isDefault = isDefault,
            isExternal = Flag.PropertyAccessor.IS_EXTERNAL(getterFlags),
            isInline = Flag.PropertyAccessor.IS_INLINE(getterFlags)
        )
    }
}
