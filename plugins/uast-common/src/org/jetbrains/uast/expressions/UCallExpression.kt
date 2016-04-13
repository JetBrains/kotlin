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

import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents a call expression (function call, constructor call, array initializer).
 */
interface UCallExpression : UExpression, UResolvable {
    /**
     * Returns the call kind.
     */
    val kind: UastCallKind

    /**
     * Returns the function reference expression if the call is a function call, null otherwise.
     */
    val functionReference: USimpleReferenceExpression?

    /**
     * Returns the class reference if the call is a constructor call, null otherwise.
     */
    val classReference: USimpleReferenceExpression?

    /**
     * Returns the function name if the call is a function call, null otherwise.
     *
     * [functionName] should only be used in debug messages.
     * Use [matchesFunctionName] to check against the name.
     */
    val functionName: String?

    /**
     * Returns an element for the function name node, or null if the node does not exist in the underlying AST (Psi).
     */
    val functionNameElement: UElement?

    /**
     * Checks if the function name is [name].
     *
     * @param name the name to check against.
     * @return true if the call is a function call, and the function name is [name], false otherwise.
     */
    open fun matchesFunctionName(name: String) = functionName == name

    /**
     * Checks if the function name is [name], and the function containing class qualified name is [containingClassFqName].
     *
     * @param containingClassFqName the required containing class qualified name.
     * @param name the function name to check against.
     * @return true if the call is a function call, the function name is [name],
     *              and the qualified name of the function direct containing class is [containingClassFqName],
     *         false otherwise.
     */
    open fun matchesFunctionNameWithContaining(containingClassFqName: String, name: String): Boolean {
        if (!matchesFunctionName(name)) return false
        val containingClass = parent as? UClass ?: return false
        return containingClass.matchesFqName(containingClassFqName)
    }

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
     * Returns the function type arguments.
     */
    val typeArguments: List<UType>

    /**
     * Resolve the call to the [UFunction] element.
     *
     * @param context the Uast context
     * @return the [UFunction] element, or null if the reference was not resolved.
     */
    override fun resolve(context: UastContext): UFunction?

    /**
     * Try to resolve the call to the [UFunction] element.
     *
     * @param context the Uast context
     * @return the [UFunction] element, of [UFunctionNotResolved] if the reference was not resolved,
     *         or the call is not a function call.
     */
    override fun resolveOrEmpty(context: UastContext): UFunction = resolve(context) ?: UFunctionNotResolved

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitCallExpression(this)) return
        functionReference?.accept(visitor)
        classReference?.accept(visitor)
        functionNameElement?.accept(visitor)
        valueArguments.acceptList(visitor)
        typeArguments.acceptList(visitor)
        visitor.afterVisitCallExpression(this)
    }

    override fun logString() = log("UFunctionCallExpression ($kind, argCount = $valueArgumentCount)", functionReference, valueArguments)
    override fun renderString(): String {
        val ref = functionName ?: classReference?.renderString() ?: functionReference?.renderString() ?: "<noref>"
        return ref + "(" + valueArguments.joinToString { it.renderString() } + ")"
    }
}