/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirCommonFunction
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirFunction

class FunctionCommonizer(cache: CirClassifiersCache) : AbstractFunctionOrPropertyCommonizer<CirFunction>(cache) {
    private val annotations = AnnotationsCommonizer()
    private val modifiers = FunctionModifiersCommonizer()
    private val valueParameters = ValueParameterListCommonizer(cache)
    private var hasStableParameterNames = true
    private var hasSynthesizedParameterNames = false

    override fun commonizationResult() = CirCommonFunction(
        annotations = annotations.result,
        name = name,
        modality = modality.result,
        visibility = visibility.result,
        extensionReceiver = extensionReceiver.result,
        returnType = returnType.result,
        kind = kind,
        modifiers = modifiers.result,
        valueParameters = valueParameters.result,
        typeParameters = typeParameters.result,
        hasStableParameterNames = hasStableParameterNames,
        hasSynthesizedParameterNames = hasSynthesizedParameterNames
    )

    override fun doCommonizeWith(next: CirFunction): Boolean {
        val result = super.doCommonizeWith(next)
                && annotations.commonizeWith(next.annotations)
                && modifiers.commonizeWith(next)
                && valueParameters.commonizeWith(next.valueParameters)

        if (result) {
            hasStableParameterNames = hasStableParameterNames && next.hasStableParameterNames
            hasSynthesizedParameterNames = hasSynthesizedParameterNames || next.hasSynthesizedParameterNames
        }

        return result
    }
}
