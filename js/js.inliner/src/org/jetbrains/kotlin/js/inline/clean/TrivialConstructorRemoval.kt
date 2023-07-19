/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.JsClass
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsSuperRef

class TrivialConstructorRemoval(private val klass: JsClass) {
    fun apply(): Boolean {
        if (klass.constructor?.isTrivial() != true) return false
        klass.constructor = null
        return true
    }

    private fun JsFunction.isTrivial(): Boolean {
        return body.statements.all { statement ->
            val expressionStatement = statement as? JsExpressionStatement
            val invocation = expressionStatement?.expression as? JsInvocation
            invocation?.qualifier is JsSuperRef && invocation.arguments.size == parameters.size && invocation.arguments.withIndex()
                .all { (index, argument) -> argument is JsNameRef && argument.name === parameters[index].name }
        }
    }
}