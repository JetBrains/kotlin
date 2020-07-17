/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirValueParameterFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.utils.isNull
import org.jetbrains.kotlin.name.Name

class ValueParameterCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirValueParameter, CirValueParameter>() {
    private lateinit var name: Name
    private val returnType = TypeCommonizer(cache)
    private var varargElementType: CirType? = null
    private var isCrossinline = true
    private var isNoinline = true

    override fun commonizationResult() = CirValueParameterFactory.create(
        annotations = emptyList(),
        name = name,
        returnType = returnType.result,
        varargElementType = varargElementType,
        declaresDefaultValue = false,
        isCrossinline = isCrossinline,
        isNoinline = isNoinline
    )

    override fun initialize(first: CirValueParameter) {
        name = first.name
        varargElementType = first.varargElementType
        isCrossinline = first.isCrossinline
        isNoinline = first.isNoinline
    }

    override fun doCommonizeWith(next: CirValueParameter): Boolean {
        val result = !next.declaresDefaultValue
                && varargElementType.isNull() == next.varargElementType.isNull()
                && name == next.name
                && returnType.commonizeWith(next.returnType)

        if (result) {
            isCrossinline = isCrossinline && next.isCrossinline
            isNoinline = isNoinline && next.isNoinline
        }

        return result
    }
}
