/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirFunctionOrProperty
import org.jetbrains.kotlin.commonizer.cir.CirProperty
import org.jetbrains.kotlin.commonizer.cir.CirType

class ReturnTypeCommonizer(
    private val typeCommonizer: TypeCommonizer,
) : NullableContextualSingleInvocationCommonizer<CirFunctionOrProperty, CirType> {
    override fun invoke(values: List<CirFunctionOrProperty>): CirType? {
        if (values.isEmpty()) return null
        val isTopLevel = values.all { it.containingClass == null }
        val isCovariant = values.none { it is CirProperty && it.isVar }
        return typeCommonizer
            .withContext { withCovariantNullabilityCommonizationEnabled(isTopLevel && isCovariant) }
            .invoke(values.map { it.returnType })
    }
}