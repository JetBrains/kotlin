/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.uast.UParameter

internal interface FirKotlinUMethodParametersProducer {
    fun produceUastParameters(uMethod: BaseKotlinUMethod, receiverTypeReference: KtTypeReference?): List<UParameter> {
        val lightParams = uMethod.psi.parameterList.parameters
        val receiver = receiverTypeReference ?: return lightParams.map { FirKotlinUParameter(it, getKotlinMemberOrigin(it), uMethod) }
        val lightReceiver = lightParams.firstOrNull() ?: return emptyList()
        val uParameters = SmartList<UParameter>(FirKotlinReceiverUParameter(lightReceiver, receiver, uMethod))
        lightParams.drop(1).mapTo(uParameters) { FirKotlinUParameter(it, getKotlinMemberOrigin(it), uMethod) }
        return uParameters
    }
}
