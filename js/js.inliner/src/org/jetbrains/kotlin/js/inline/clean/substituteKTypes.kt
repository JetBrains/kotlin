/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.backend.ast.metadata.kType
import org.jetbrains.kotlin.js.backend.ast.metadata.specialFunction

// Replaces getReifiedTypeParameterKType(<Class Constructor>) with its KType
fun substituteKTypes(root: JsNode) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(invocation: JsInvocation, ctx: JsContext<in JsNode>) {
            val qualifier = invocation.qualifier as? JsNameRef ?: return
            if (qualifier.name?.specialFunction != SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE) return
            val firstArg = invocation.arguments.first()
            firstArg.kType?.let {
                ctx.replaceMe(it)
            }
        }
    }
    visitor.accept(root)
}