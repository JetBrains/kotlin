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

package org.jetbrains.kotlin.js.inline.util

import com.google.dart.compiler.backend.js.ast.JsLiteral.*
import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.typeCheck
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.any

fun JsExpression.canHaveSideEffect(): Boolean =
        any { it is JsExpression && it.canHaveOwnSideEffect() }

fun JsExpression.canHaveOwnSideEffect(): Boolean =
    when (this) {
        is JsValueLiteral,
        is JsConditional,
        is JsArrayAccess,
        is JsArrayLiteral,
        is JsNameRef -> false
        is JsBinaryOperation -> operator.isAssignment
        is JsInvocation -> !isFunctionCreatorInvocation(this)
        else -> true
    }

fun JsExpression.needToAlias(): Boolean =
        any { it is JsExpression && it.shouldHaveOwnAlias() }

fun JsExpression.shouldHaveOwnAlias(): Boolean =
        when (this) {
            is JsConditional,
            is JsBinaryOperation,
            is JsArrayLiteral -> true
            is JsInvocation -> if (typeCheck == null) canHaveSideEffect() else false
            else -> canHaveOwnSideEffect()
        }
