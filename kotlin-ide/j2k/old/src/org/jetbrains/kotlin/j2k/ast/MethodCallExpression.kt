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

package org.jetbrains.kotlin.j2k.ast

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.append

class MethodCallExpression(
        val methodExpression: Expression,
        val argumentList: ArgumentList,
        val typeArguments: List<Type>,
        override val isNullable: Boolean
) : Expression() {

    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, methodExpression).append(typeArguments, ", ", "<", ">")
        builder.append(argumentList)
    }

    companion object {
        fun buildNonNull(
                receiver: Expression?,
                methodName: String,
                argumentList: ArgumentList = ArgumentList.withNoPrototype(),
                typeArguments: List<Type> = emptyList(),
                dotPrototype: PsiElement? = null
        ): MethodCallExpression = build(receiver, methodName, argumentList, typeArguments, false, dotPrototype)

        fun buildNullable(
                receiver: Expression?,
                methodName: String,
                argumentList: ArgumentList = ArgumentList.withNoPrototype(),
                typeArguments: List<Type> = emptyList(),
                dotPrototype: PsiElement? = null
        ): MethodCallExpression = build(receiver, methodName, argumentList, typeArguments, true, dotPrototype)

        fun build(
                receiver: Expression?,
                methodName: String,
                argumentList: ArgumentList,
                typeArguments: List<Type>,
                isNullable: Boolean,
                dotPrototype: PsiElement? = null
        ): MethodCallExpression {
            val identifier = Identifier.withNoPrototype(methodName, isNullable = false)
            val methodExpression = if (receiver != null)
                QualifiedExpression(receiver, identifier, dotPrototype).assignNoPrototype()
            else
                identifier
            return MethodCallExpression(methodExpression, argumentList, typeArguments, isNullable)
        }
    }
}
