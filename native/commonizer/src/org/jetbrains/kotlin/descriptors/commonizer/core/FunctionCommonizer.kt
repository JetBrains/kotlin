/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunction
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirFunctionFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache

class FunctionCommonizer(cache: CirClassifiersCache) : AbstractFunctionOrPropertyCommonizer<CirFunction>(cache) {
    private val annotations = AnnotationsCommonizer()
    private val modifiers = FunctionModifiersCommonizer()
    private val valueParameters = ValueParameterListCommonizer(cache)
    private var hasStableParameterNames = true
    private var hasSynthesizedParameterNames = false

    override fun commonizationResult() = CirFunctionFactory.create(
        annotations = annotations.result,
        name = name,
        typeParameters = typeParameters.result,
        visibility = visibility.result,
        modality = modality.result,
        containingClassDetails = null,
        valueParameters = valueParameters.result,
        hasStableParameterNames = hasStableParameterNames,
        hasSynthesizedParameterNames = hasSynthesizedParameterNames,
        isExternal = false,
        extensionReceiver = extensionReceiver.result,
        returnType = returnType.result,
        kind = kind,
        modifiers = modifiers.result
    )

    override fun doCommonizeWith(next: CirFunction): Boolean {
        val result = super.doCommonizeWith(next)
                && annotations.commonizeWith(next.annotations)
                && modifiers.commonizeWith(next.modifiers)
                && valueParameters.commonizeWith(next.valueParameters)

        if (result) {
            hasStableParameterNames = hasStableParameterNames && next.hasStableParameterNames
            hasSynthesizedParameterNames = hasSynthesizedParameterNames || next.hasSynthesizedParameterNames
        }

        return result
    }
}
