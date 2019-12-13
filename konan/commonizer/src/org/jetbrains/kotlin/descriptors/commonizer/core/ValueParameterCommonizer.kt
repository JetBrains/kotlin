/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirCommonValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.utils.isNull
import org.jetbrains.kotlin.name.Name

interface ValueParameterCommonizer : Commonizer<CirValueParameter, CirValueParameter> {
    companion object {
        fun default(cache: CirClassifiersCache): ValueParameterCommonizer = DefaultValueParameterCommonizer(cache)
    }
}

private class DefaultValueParameterCommonizer(cache: CirClassifiersCache) :
    ValueParameterCommonizer,
    AbstractStandardCommonizer<CirValueParameter, CirValueParameter>() {

    private lateinit var name: Name
    private val returnType = TypeCommonizer.default(cache)
    private var varargElementType: CirType? = null
    private var isCrossinline = true
    private var isNoinline = true

    override fun commonizationResult() = CirCommonValueParameter(
        name = name,
        returnType = returnType.result,
        varargElementType = varargElementType,
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

interface ValueParameterListCommonizer : Commonizer<List<CirValueParameter>, List<CirValueParameter>> {
    companion object {
        fun default(cache: CirClassifiersCache): ValueParameterListCommonizer = DefaultValueParameterListCommonizer(cache)
    }
}

private class DefaultValueParameterListCommonizer(cache: CirClassifiersCache) :
    ValueParameterListCommonizer,
    AbstractListCommonizer<CirValueParameter, CirValueParameter>(
        singleElementCommonizerFactory = { ValueParameterCommonizer.default(cache) }
    )
