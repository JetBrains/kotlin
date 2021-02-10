/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmValueParameter
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirValueParameterImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirValueParameterFactory {
    private val interner = Interner<CirValueParameter>()

    fun create(source: ValueParameterDescriptor): CirValueParameter = create(
        annotations = source.annotations.compactMap(CirAnnotationFactory::create),
        name = CirName.create(source.name),
        returnType = CirTypeFactory.create(source.returnType!!),
        varargElementType = source.varargElementType?.let(CirTypeFactory::create),
        declaresDefaultValue = source.declaresDefaultValue(),
        isCrossinline = source.isCrossinline,
        isNoinline = source.isNoinline
    )

    fun create(source: KmValueParameter, typeResolver: CirTypeResolver): CirValueParameter = create(
        annotations = CirAnnotationFactory.createAnnotations(source.flags, typeResolver, source::annotations),
        name = CirName.create(source.name),
        returnType = CirTypeFactory.create(source.type!!, typeResolver),
        varargElementType = source.varargElementType?.let { CirTypeFactory.create(it, typeResolver) },
        declaresDefaultValue = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(source.flags),
        isCrossinline = Flag.ValueParameter.IS_CROSSINLINE(source.flags),
        isNoinline = Flag.ValueParameter.IS_NOINLINE(source.flags)
    )

    fun create(
        annotations: List<CirAnnotation>,
        name: CirName,
        returnType: CirType,
        varargElementType: CirType?,
        declaresDefaultValue: Boolean,
        isCrossinline: Boolean,
        isNoinline: Boolean
    ): CirValueParameter {
        return interner.intern(
            CirValueParameterImpl(
                annotations = annotations,
                name = name,
                returnType = returnType,
                varargElementType = varargElementType,
                declaresDefaultValue = declaresDefaultValue,
                isCrossinline = isCrossinline,
                isNoinline = isNoinline
            )
        )
    }
}
