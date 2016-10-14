/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.*

abstract class LazyImportedCallIntrinsic(
        val simpleName: String,
        val isConstructor: Boolean = false,
        val receiverAsArgument: Boolean = true
) : FunctionIntrinsic() {
    private val internalNameMap = WeakHashMap<TranslationContext, JsName>()

    override fun apply(receiver: JsExpression?, arguments: MutableList<JsExpression>, context: TranslationContext): JsExpression {
        val rootContext = generateSequence(context) { it.parent }.last()
        val functionName = internalNameMap.getOrPut(rootContext) {
            val declaration = getExpression(rootContext)
            if (declaration is JsNameRef && declaration.name != null && declaration.qualifier == null) {
                declaration.name!!
            }
            else {
                rootContext.importDeclaration(simpleName, declaration)
            }
        }
        val allArguments = (if (receiverAsArgument) receiver else null).singletonOrEmptyList() + arguments
        val function = pureFqn(functionName, null)
        return if (isConstructor) JsNew(function, allArguments) else JsInvocation(function, allArguments)
    }

    abstract fun getExpression(context: TranslationContext): JsExpression
}