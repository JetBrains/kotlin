/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils

open class KotlinAliasedFunctionIntrinsic(private val functionName: String) : FunctionIntrinsicWithReceiverComputed() {
    override fun apply(
            receiver: JsExpression?,
            arguments: List<JsExpression>,
            context: TranslationContext
    ): JsExpression {
        val function = context.getReferenceToIntrinsic(functionName)
        return JsInvocation(function, if (receiver == null) arguments else TranslationUtils.generateInvocationArguments(receiver, arguments))
    }
}