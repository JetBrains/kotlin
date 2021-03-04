/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmValueParameter
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter

object CirValueParameterFactory {
    fun create(source: KmValueParameter, typeResolver: CirTypeResolver) = CirValueParameter.createInterned(
        annotations = CirAnnotationFactory.createAnnotations(source.flags, typeResolver, source::annotations),
        name = CirName.create(source.name),
        returnType = CirTypeFactory.create(source.type!!, typeResolver),
        varargElementType = source.varargElementType?.let { CirTypeFactory.create(it, typeResolver) },
        declaresDefaultValue = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(source.flags),
        isCrossinline = Flag.ValueParameter.IS_CROSSINLINE(source.flags),
        isNoinline = Flag.ValueParameter.IS_NOINLINE(source.flags)
    )
}
