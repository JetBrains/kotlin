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

package org.jetbrains.kotlin.js.inline.clean

import com.google.dart.compiler.backend.js.ast.JsFunction

class FunctionPostProcessor(root: JsFunction) {
    val optimizations = listOf(
        { TemporaryAssignmentElimination(root.body).apply() },
        { RedundantLabelRemoval(root.body).apply() },
        { TemporaryVariableElimination(root).apply() },
        { IfStatementReduction(root.body).apply() },
        { DeadCodeElimination(root.body).apply() },
        { RedundantVariableDeclarationElimination(root.body).apply() },
        { IneffectiveStatementElimination(root).apply() }
    )
    // TODO: reduce to A || B, A && B if possible

    fun apply() {
        do {
            val hasChanges = optimizations.fold(false) { existing, f -> existing or f() }
        } while (hasChanges)
    }
}