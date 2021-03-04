/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.descriptors.commonizer.utils.isNull

class ValueParameterCommonizer(classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirValueParameter, CirValueParameter>() {
    private lateinit var name: CirName
    private val returnType = TypeCommonizer(classifiers)
    private var varargElementType: CirType? = null
    private var isCrossinline = true
    private var isNoinline = true

    override fun commonizationResult() = CirValueParameter.createInterned(
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
                && returnType.commonizeWith(next.returnType)

        if (result) {
            isCrossinline = isCrossinline && next.isCrossinline
            isNoinline = isNoinline && next.isNoinline
        }

        return result
    }

    fun overwriteName(name: CirName) {
        this.name = name
    }
}
