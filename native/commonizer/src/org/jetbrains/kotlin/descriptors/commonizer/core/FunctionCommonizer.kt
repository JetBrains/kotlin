/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunction
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirFunctionFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers

class FunctionCommonizer(classifiers: CirKnownClassifiers) : AbstractFunctionOrPropertyCommonizer<CirFunction>(classifiers) {
    private val annotations = AnnotationsCommonizer()
    private val modifiers = FunctionModifiersCommonizer()
    private val valueParameters = CallableValueParametersCommonizer(classifiers)

    override fun commonizationResult(): CirFunction {
        val valueParameters = valueParameters.result
        valueParameters.patchCallables()

        return CirFunctionFactory.create(
            annotations = annotations.result,
            name = name,
            typeParameters = typeParameters.result,
            visibility = visibility.result,
            modality = modality.result,
            containingClassDetails = null,
            valueParameters = valueParameters.valueParameters,
            hasStableParameterNames = valueParameters.hasStableParameterNames,
            extensionReceiver = extensionReceiver.result,
            returnType = returnType.result,
            kind = kind,
            modifiers = modifiers.result
        )
    }

    override fun doCommonizeWith(next: CirFunction): Boolean {
        return super.doCommonizeWith(next)
                && annotations.commonizeWith(next.annotations)
                && modifiers.commonizeWith(next.modifiers)
                && valueParameters.commonizeWith(next)
    }
}
