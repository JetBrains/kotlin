/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker.Companion.isJsCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.source.getPsi

fun PropertyDescriptor.hasValidJsCodeBody(bindingContext: BindingContext): Boolean {
    val property = source.getPsi() as? KtProperty ?: return false
    val initializer = property.initializer ?: return false
    return initializer.isJsCall(bindingContext)
}

fun FunctionDescriptor.hasValidJsCodeBody(bindingContext: BindingContext): Boolean {
    val function = source.getPsi() as? KtNamedFunction ?: return false
    return function.hasValidJsCodeBody(bindingContext)
}

private fun KtDeclarationWithBody.hasValidJsCodeBody(bindingContext: BindingContext): Boolean {
    if (!hasBody()) return false
    val body = bodyExpression!!
    return when {
        !hasBlockBody() -> body.isJsCall(bindingContext)
        body is KtBlockExpression -> {
            val statement = body.statements.singleOrNull() ?: return false
            statement.isJsCall(bindingContext)
        }
        else -> false
    }
}

private fun KtExpression.isJsCall(bindingContext: BindingContext): Boolean {
    return getResolvedCall(bindingContext)?.isJsCall() ?: false
}