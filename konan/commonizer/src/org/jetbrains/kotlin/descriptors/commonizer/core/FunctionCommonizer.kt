/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonFunction
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver.Companion.toReceiverNoAnnotations
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Function

class FunctionCommonizer(cache: ClassifiersCache) : AbstractFunctionOrPropertyCommonizer<Function>(cache) {
    private val modifiers = FunctionModifiersCommonizer.default()
    private val valueParameters = ValueParameterListCommonizer.default(cache)
    private var hasStableParameterNames = true
    private var hasSynthesizedParameterNames = false

    override fun commonizationResult() = CommonFunction(
        name = name,
        modality = modality.result,
        visibility = visibility.result,
        extensionReceiver = extensionReceiver.result?.toReceiverNoAnnotations(),
        returnType = returnType.result,
        modifiers = modifiers.result,
        valueParameters = valueParameters.result,
        typeParameters = typeParameters.result,
        hasStableParameterNames = hasStableParameterNames,
        hasSynthesizedParameterNames = hasSynthesizedParameterNames
    )

    override fun doCommonizeWith(next: Function): Boolean {
        val result = super.doCommonizeWith(next)
                && modifiers.commonizeWith(next)
                && valueParameters.commonizeWith(next.valueParameters)

        if (result) {
            hasStableParameterNames = hasStableParameterNames && next.hasStableParameterNames
            hasSynthesizedParameterNames = hasSynthesizedParameterNames || next.hasSynthesizedParameterNames
        }

        return result
    }
}
