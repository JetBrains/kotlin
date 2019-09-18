/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.nj2k.postProcessing.runUndoTransparentActionInEdt
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory

class InferenceFacade(
    private val typeVariablesCollector: ContextCollector,
    private val constraintsCollectorAggregator: ConstraintsCollectorAggregator,
    private val boundTypeCalculator: BoundTypeCalculator,
    private val stateUpdater: StateUpdater,
    private val defaultStateProvider: DefaultStateProvider,
    private val renderDebugTypes: Boolean = false,
    private val printDebugConstraints: Boolean = false
) {
    fun runOn(elements: List<KtElement>) {
        val inferenceContext = runReadAction { typeVariablesCollector.collectTypeVariables(elements) }
        val constraints = runReadAction {
            constraintsCollectorAggregator.collectConstraints(boundTypeCalculator, inferenceContext, elements)
        }

        val initialConstraints = if (renderDebugTypes) constraints.map { it.copy() } else null
        runReadAction {
            Solver(inferenceContext, printDebugConstraints, defaultStateProvider).solveConstraints(constraints)
        }

        if (renderDebugTypes) {
            with(DebugPrinter(inferenceContext)) {
                runUndoTransparentActionInEdt(inWriteAction = true) {
                    for ((expression, boundType) in boundTypeCalculator.expressionsWithBoundType()) {
                        val comment = KtPsiFactory(expression.project).createComment("/*${boundType.asString()}*/")
                        expression.parent.addAfter(comment, expression)
                    }
                    for (element in elements) {
                        element.addTypeVariablesNames()
                    }
                    for (element in elements) {
                        val factory = KtPsiFactory(element)
                        element.add(factory.createNewLine(lineBreaks = 2))
                        for (constraint in initialConstraints!!) {
                            element.add(factory.createComment("//${constraint.asString()}"))
                            element.add(factory.createNewLine(lineBreaks = 1))
                        }
                    }
                }
            }
        }
        runUndoTransparentActionInEdt(inWriteAction = true) {
            stateUpdater.updateStates(inferenceContext)
        }
    }
}