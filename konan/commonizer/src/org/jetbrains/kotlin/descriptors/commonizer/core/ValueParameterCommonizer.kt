/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.isNull
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType

interface ValueParameterCommonizer : Commonizer<ValueParameter, ValueParameter> {
    companion object {
        fun default(cache: ClassifiersCache): ValueParameterCommonizer = DefaultValueParameterCommonizer(cache)
    }
}

private class DefaultValueParameterCommonizer(cache: ClassifiersCache) :
    ValueParameterCommonizer,
    AbstractStandardCommonizer<ValueParameter, ValueParameter>() {

    private lateinit var name: Name
    private val returnType = TypeCommonizer.default(cache)
    private var varargElementType: UnwrappedType? = null
    private var isCrossinline = true
    private var isNoinline = true

    override fun commonizationResult() = CommonValueParameter(
        name = name,
        returnType = returnType.result,
        varargElementType = varargElementType,
        isCrossinline = isCrossinline,
        isNoinline = isNoinline
    )

    override fun initialize(first: ValueParameter) {
        name = first.name
        varargElementType = first.varargElementType
        isCrossinline = first.isCrossinline
        isNoinline = first.isNoinline
    }

    override fun doCommonizeWith(next: ValueParameter): Boolean {
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

interface ValueParameterListCommonizer : Commonizer<List<ValueParameter>, List<ValueParameter>> {
    companion object {
        fun default(cache: ClassifiersCache): ValueParameterListCommonizer = DefaultValueParameterListCommonizer(cache)
    }
}

private class DefaultValueParameterListCommonizer(cache: ClassifiersCache) :
    ValueParameterListCommonizer,
    AbstractListCommonizer<ValueParameter, ValueParameter>(
        singleElementCommonizerFactory = { ValueParameterCommonizer.default(cache) }
    )
