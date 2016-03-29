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

interface UCallExpression : UExpression, UResolvable {
    val functionReference: USimpleReferenceExpression?

    val classReference: USimpleReferenceExpression?

    val functionName: String?
    val functionNameElement: UElement?

    fun functionNameMatches(name: String) = functionName == name

    val valueArgumentCount: Int
    val valueArguments: List<UExpression>

    val typeArgumentCount: Int
    val typeArguments: List<UType>

    val kind: UastCallKind

    override fun resolve(context: UastContext): UFunction?
    override fun resolveOrEmpty(context: UastContext): UFunction = resolve(context) ?: UFunctionNotResolved

    override fun traverse(callback: UastCallback) {
        functionReference?.handleTraverse(callback)
        classReference?.handleTraverse(callback)
        functionNameElement?.handleTraverse(callback)
        valueArguments.handleTraverseList(callback)
        typeArguments.handleTraverseList(callback)
    }

    override fun logString() = log("UFunctionCallExpression ($kind, argCount = $valueArgumentCount)", functionReference, valueArguments)
    override fun renderString(): String {
        val ref = functionName ?: functionReference?.renderString() ?: classReference?.renderString() ?: "<noref>"
        return ref + "(" + valueArguments.joinToString { it.renderString() } + ")"
    }
}