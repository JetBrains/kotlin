/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*

// Replaces getReifiedTypeParameterKType(<Class Constructor>) with its KType
fun substituteKTypes(root: JsNode) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(invocation: JsInvocation, ctx: JsContext<in JsNode>) {
            // for invocations from non-inline contexts
            invocation.checkDoesNotCreateRecursiveKType()

            val qualifier = invocation.qualifier as? JsNameRef ?: return
            if (qualifier.name?.specialFunction != SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE) return

            val firstArg = invocation.arguments.first()
            getTransitiveKType(firstArg)?.let {
                // for invocations from inline contexts
                it.checkNoInvocationsWithRecursiveKType()
                ctx.replaceMe(it)
            }
        }
    }
    visitor.accept(root)
}

// Get KType from expression or from staticRef of referenced name.
// kType metadata is set on jsClass expressions.
// There can be a chain of local variables from jsClass to its usage.
// This methods uses staticRef to find the original expression with kType metadata.
private fun getTransitiveKType(e: JsExpression): JsExpression? {
    return when {
        e.kType != null ->
            e.kType

        e is JsNameRef -> {
            val staticRef = e.name?.staticRef as? JsExpression ?: return null
            getTransitiveKType(staticRef)
        }

        else -> null
    }
}

private fun JsExpression.checkNoInvocationsWithRecursiveKType() {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(invocation: JsInvocation, ctx: JsContext<in JsNode>) {
            invocation.checkDoesNotCreateRecursiveKType()
        }
    }
    visitor.accept(this)
}

private fun JsInvocation.checkDoesNotCreateRecursiveKType() {
    // See KT-40173
    if (kTypeWithRecursion) TODO("Non-reified type parameters with recursive bounds are not supported yet")
}
