/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.backend.ast.metadata.primitiveKClass
import org.jetbrains.kotlin.js.backend.ast.metadata.specialFunction

// Replaces getKClass(<Primive class constructor>) with PrimitiveClasses.<primitive class KClass>
fun emergePrimitiveKClass(root: JsNode) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(invocation: JsInvocation, ctx: JsContext<in JsNode>) {
            val qualifier = invocation.qualifier as? JsNameRef ?: return
            if (qualifier.name?.specialFunction != SpecialFunction.GET_KCLASS) return

            val firstArg = invocation.arguments.firstOrNull() as? JsNameRef ?: return
            firstArg.primitiveKClass?.let {
                ctx.replaceMe(it)
            }
        }
    }
    visitor.accept(root)
}