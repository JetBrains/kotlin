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
package org.jetbrains.uast


import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.internal.log
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents a call expression (method/constructor call, array initializer).
 */
interface UCallExpression : UExpression, UResolvable {
    /**
     * Returns the call kind.
     */
    val kind: UastCallKind

    /**
     * Returns the called method name, or null if the call is not a method call.
     * This property should return the actual resolved function name.
     */
    val methodName: String?

    /**
     * Returns the expression receiver.
     * For example, for call `a.b.[c()]` the receiver is `a.b`.
     */
    val receiver: UExpression?

    /**
     * Returns the receiver type, or null if the call has not a receiver.
     */
    val receiverType: PsiType?

    /**
     * Returns the function reference expression if the call is a non-constructor method call, null otherwise.
     */
    val methodIdentifier: UIdentifier?

    /**
     * Returns the class reference if the call is a constructor call, null otherwise.
     */
    val classReference: UReferenceExpression?

    /**
     * Returns the value argument count.
     *
     * Retrieving the argument count could be faster than getting the [valueArguments.size],
     *    because there is no need to create actual [UExpression] instances.
     */
    val valueArgumentCount: Int

    /**
     * Returns the list of value arguments.
     */
    val valueArguments: List<UExpression>

    /**
     * Returns the type argument count.
     */
    val typeArgumentCount: Int

    /**
     * Returns the type arguments for the call.
     */
    val typeArguments: List<PsiType>

    /**
     * Returns the return type of the called function, or null if the call is not a function call.
     */
    val returnType: PsiType?

    /**
     * Resolve the called method.
     * 
     * @return the [PsiMethod], or null if the method was not resolved. 
     * Note that the [PsiMethod] is an unwrapped [PsiMethod], not a [UMethod].
     */
    override fun resolve(): PsiMethod?

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitCallExpression(this)) return
        methodIdentifier?.accept(visitor)
        classReference?.accept(visitor)
        valueArguments.acceptList(visitor)
        visitor.afterVisitCallExpression(this)
    }

    override fun asLogString() = log("UCallExpression ($kind, argCount = $valueArgumentCount)", methodIdentifier, valueArguments)
    
    override fun asRenderString(): String {
        val ref = classReference?.asRenderString() ?: methodName ?: methodIdentifier?.asRenderString() ?: "<noref>"
        return ref + "(" + valueArguments.joinToString { it.asRenderString() } + ")"
    }
}