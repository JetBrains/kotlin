/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast.java

import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiPrefixExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UPrefixExpression
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUPrefixExpression(
        override val psi: PsiPrefixExpression,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UPrefixExpression, PsiElementBacked {
    override val operand by lz { JavaConverter.convertOrEmpty(psi.operand, this) }

    override val operatorIdentifier: UIdentifier?
        get() = UIdentifier(psi.operationSign, this)

    override fun resolveOperator() = null

    override val operator = when (psi.operationTokenType) {
        JavaTokenType.PLUS -> UastPrefixOperator.UNARY_PLUS
        JavaTokenType.MINUS -> UastPrefixOperator.UNARY_MINUS
        JavaTokenType.PLUSPLUS -> UastPrefixOperator.INC
        JavaTokenType.MINUSMINUS-> UastPrefixOperator.DEC
        JavaTokenType.EXCL -> UastPrefixOperator.LOGICAL_NOT
        JavaTokenType.TILDE -> UastPrefixOperator.BITWISE_NOT
        else -> UastPrefixOperator.UNKNOWN
    }
}