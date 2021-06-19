/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter

internal interface KotlinUMethodParametersProducer {
    fun produceUastParameters(uMethod: BaseKotlinUMethod, receiverTypeReference: KtTypeReference?): List<UParameter> {

        fun parameterOrigin(psiParameter: PsiParameter?): KtElement? = when (psiParameter) {
            is KtLightElement<*, *> -> psiParameter.kotlinOrigin
            is UastKotlinPsiParameter -> psiParameter.ktParameter
            else -> null
        }

        val lightParams = uMethod.psi.parameterList.parameters
        val receiver = receiverTypeReference ?: return lightParams.map { KotlinUParameter(it, parameterOrigin(it), uMethod) }
        val receiverLight = lightParams.firstOrNull() ?: return emptyList()
        val uParameters = SmartList<UParameter>(KotlinReceiverUParameter(receiverLight, receiver, uMethod))
        lightParams.drop(1).mapTo(uParameters) { KotlinUParameter(it, parameterOrigin(it), uMethod) }
        return uParameters
    }
}
