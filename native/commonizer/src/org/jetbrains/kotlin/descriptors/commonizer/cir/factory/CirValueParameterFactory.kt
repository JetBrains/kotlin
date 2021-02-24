/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

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
