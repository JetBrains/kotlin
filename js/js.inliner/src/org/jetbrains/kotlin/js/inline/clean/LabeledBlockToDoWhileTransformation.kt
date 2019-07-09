/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

object LabeledBlockToDoWhileTransformation {
    fun apply(root: JsNode) {
        object : JsVisitorWithContextImpl() {
            val loopOrSwitchStack = Stack<JsStatement>()
            val newFakeLoops = HashSet<JsDoWhile>()
            val statementsLabels = HashMap<JsStatement, JsLabel>()

            // If labeled block sits in between loop (or switch) and corresponding unlabeled breaks/continues
            // we have to label this loop, breaks and continues in order to preserve their
            // relationships across new fake do-while loop
            val loopsAndSwitchesToLabel = HashSet<JsStatement>()

            override fun endVisit(x: JsLabel, ctx: JsContext<JsNode>) {
                if (x.statement is JsBlock) {
                    loopsAndSwitchesToLabel.addIfNotNull(loopOrSwitchStack.lastOrNull())
                    val fakeLoop = JsDoWhile(JsBooleanLiteral(false), x.statement)
                    newFakeLoops.add(fakeLoop)
                    x.statement = fakeLoop
                }
                super.endVisit(x, ctx)
            }

            override fun visit(x: JsLabel, ctx: JsContext<JsNode>): Boolean {
                if (x.statement is JsLoop) {
                    statementsLabels[x.statement] = x
                }
                return true
            }

            override fun visit(x: JsLoop, ctx: JsContext<JsNode>): Boolean {
                loopOrSwitchStack.push(x)
                return true
            }

            override fun visit(x: JsSwitch, ctx: JsContext<JsNode>): Boolean {
                loopOrSwitchStack.push(x)
                return true
            }

            fun endVisitLoopOrSwitch(x: JsStatement, ctx: JsContext<JsNode>) {
                val top = loopOrSwitchStack.pop()
                assert(top === x)

                if (loopsAndSwitchesToLabel.contains(x)) {
                    // Reuse loop label if present. Otherwise create new label.
                    var label = statementsLabels[x]
                    if (label == null) {
                        val labelName = JsScope.declareTemporaryName("loop_label")
                        label = JsLabel(labelName, x)
                        statementsLabels[x] = label
                        ctx.replaceMe(label)
                    }
                    labelLoopBreaksAndContinues(x, newFakeLoops, label.name.makeRef())
                }
            }

            override fun endVisit(x: JsSwitch, ctx: JsContext<JsNode>) {
                endVisitLoopOrSwitch(x, ctx)
            }

            override fun endVisit(x: JsLoop, ctx: JsContext<JsNode>) {
                endVisitLoopOrSwitch(x, ctx)
            }

        }.accept(root)
    }

    /*
    Label unlabeled breaks that correspond to current loop.
    Skip newly created fake do-while loops.
     */
    private fun labelLoopBreaksAndContinues(loopOrSwitch: JsStatement, fakeLoops: Set<JsDoWhile>, label: JsNameRef) {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsLoop, ctx: JsContext<JsNode>): Boolean =
                fakeLoops.contains(x) || x === loopOrSwitch

            override fun visit(x: JsSwitch, ctx: JsContext<JsNode>): Boolean =
                x === loopOrSwitch

            override fun endVisit(x: JsBreak, ctx: JsContext<JsNode>) {
                if (x.label == null)
                    ctx.replaceMe(JsBreak(label))
                super.endVisit(x, ctx)
            }

            override fun endVisit(x: JsContinue, ctx: JsContext<JsNode>) {
                if (x.label == null)
                    ctx.replaceMe(JsContinue(label))
                super.endVisit(x, ctx)
            }
        }.accept(loopOrSwitch)
    }
}

fun transformLabeledBlockToDoWhile(fragments: Iterable<JsProgramFragment>) {
    for (fragment in fragments) {
        LabeledBlockToDoWhileTransformation.apply(fragment.declarationBlock)
        LabeledBlockToDoWhileTransformation.apply(fragment.initializerBlock)
    }
}