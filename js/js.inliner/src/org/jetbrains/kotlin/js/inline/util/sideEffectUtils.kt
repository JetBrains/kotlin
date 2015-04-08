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
import org.jetbrains.kotlin.js.translate.utils.ast.any

public fun JsExpression.canHaveSideEffect(): Boolean =
        any { it is JsExpression && it.canHaveOwnSideEffect() }

public fun JsExpression.canHaveOwnSideEffect(): Boolean =
    when (this) {
        is JsValueLiteral,
        is JsConditional,
        is JsArrayAccess,
        is JsArrayLiteral,
        is JsNameRef -> false
        is JsBinaryOperation -> getOperator().isAssignment()
        else -> true
    }

public fun JsExpression.needToAlias(): Boolean =
        any { it is JsExpression && it.shouldHaveOwnAlias() }

public fun JsExpression.shouldHaveOwnAlias(): Boolean =
        when (this) {
            is JsThisRef,
            is JsConditional,
            is JsBinaryOperation,
            is JsArrayLiteral -> true
            is JsInvocation -> !isFunctionCreatorInvocation(this)
            else -> canHaveOwnSideEffect()
        }