/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirFunction

class FunctionCommonizer(
    private val typeCommonizer: TypeCommonizer,
    private val functionOrPropertyBaseCommonizer: FunctionOrPropertyBaseCommonizer,
) : NullableSingleInvocationCommonizer<CirFunction> {
    override fun invoke(values: List<CirFunction>): CirFunction? {
        if (values.isEmpty()) return null
        val functionOrProperty = functionOrPropertyBaseCommonizer(values) ?: return null
        val valueParametersResult = CallableValueParametersCommonizer(typeCommonizer).commonize(values) ?: return null
        return CirFunction(
            annotations = functionOrProperty.annotations,
            name = values.first().name,
            typeParameters = functionOrProperty.typeParameters,
            visibility = functionOrProperty.visibility,
            modality = functionOrProperty.modality,
            containingClass = null, // does not matter
            valueParameters = valueParametersResult.valueParameters,
            hasStableParameterNames = valueParametersResult.hasStableParameterNames,
            extensionReceiver = functionOrProperty.extensionReceiver,
            returnType = functionOrProperty.returnType,
            kind = functionOrProperty.kind,
            modifiers = FunctionModifiersCommonizer().commonize(values.map { it.modifiers }) ?: return null
        )
    }
}