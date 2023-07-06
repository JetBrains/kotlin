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

import org.jetbrains.kotlin.js.backend.ast.JsFunction

class FunctionPostProcessor(val root: JsFunction) {
    val optimizations = listOf(
        { RedundantLabelRemoval(root.body).apply() },
        { EmptyStatementElimination(root.body).apply() },
        { WhileConditionFolding(root.body).apply() },
        { DoWhileGuardElimination(root.body).apply() },
        { TemporaryVariableElimination(root).apply() },
        { RedundantCallElimination(root.body).apply() },
        { IfStatementReduction(root.body).apply() },
        { DeadCodeElimination(root.body).apply() },
        { RedundantVariableDeclarationElimination(root.body).apply() },
        { RedundantStatementElimination(root).apply() },
        { CoroutineStateElimination(root.body).apply() },
        { BoxingUnboxingElimination(root.body).apply() },
        { MoveTemporaryVariableDeclarationToAssignment(root.body).apply() }
    )
    // TODO: reduce to A || B, A && B if possible

    fun apply() {
        do {
            var hasChanges = false
            for (opt in optimizations) {
                hasChanges = hasChanges or opt()
            }
        } while (hasChanges)
    }
}
