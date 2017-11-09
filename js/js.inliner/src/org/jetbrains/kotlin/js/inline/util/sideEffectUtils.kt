/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.any

fun JsExpression.canHaveSideEffect(localVars: Set<JsName>) =
        any { it is JsExpression && it.canHaveOwnSideEffect(localVars) }

fun JsExpression.canHaveOwnSideEffect(vars: Set<JsName>) = when (this) {
    is JsConditional,
    is JsLiteral -> false
    is JsBinaryOperation -> operator.isAssignment
    is JsNameRef -> name !in vars && sideEffects != SideEffectKind.PURE
    else -> sideEffects != SideEffectKind.PURE
}
