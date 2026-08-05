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
import org.jetbrains.kotlin.js.backend.ast.JsName

internal abstract class FunctionPostProcessorStep : Function0<Boolean> {
    var hasChanges: Boolean = false
        protected set

    protected abstract fun apply()

    final override fun invoke(): Boolean {
        hasChanges = false
        apply()
        return hasChanges
    }
}

class FunctionPostProcessor(val root: JsFunction, voidName: JsName? = null) {
    private val optimizations: List<() -> FunctionPostProcessorStep> = listOfNotNull(
        { RedundantLabelRemoval(root.body) },
        { EmptyStatementElimination(root.body) },
        { DoWhileGuardElimination(root.body) },
        { TemporaryVariableElimination(root) },
        { RedundantCallElimination(root.body) },
        { IfStatementReduction(root.body) },
        { DeadCodeElimination(root.body) },
        { RedundantVariableDeclarationElimination(root.body) },
        { RedundantStatementElimination(root) },
        { BoxingUnboxingElimination(root.body) },
        { MoveTemporaryVariableDeclarationToAssignment(root.body) },
        voidName?.let { { VoidPropertiesElimination(root.body, voidName) } },
    )
    // TODO: reduce to A || B, A && B if possible

    fun apply() {
        do {
            var hasChanges = false
            for (passFactory in optimizations) {
                val opt = passFactory()
                hasChanges = hasChanges or opt()
            }
        } while (hasChanges)
    }
}
