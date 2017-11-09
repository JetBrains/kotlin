/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions.basic

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.TranslationContext

/**
 * Base for intrinsics that substitute standard function calls like Int#plus, Float#minus ... etc
 */
abstract class FunctionIntrinsic {
    abstract fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression

    open fun exists(): Boolean = true

    companion object {
        @JvmField
        val NO_INTRINSIC: FunctionIntrinsic = object : FunctionIntrinsic() {
            override fun exists() = false

            override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext) =
                    throw UnsupportedOperationException("FunctionIntrinsic#NO_INTRINSIC_#apply")

        }

        @JvmStatic
        protected fun getThisOrReceiverOrNull(callInfo: CallInfo): JsExpression? =
            callInfo.dispatchReceiver ?: callInfo.extensionReceiver
    }
}
