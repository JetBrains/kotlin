/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.result

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.references.ReferenceAccess
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

class ForEachTransformation(
    loop: KtForExpression,
    private val inputVariable: KtCallableDeclaration,
    private val indexVariable: KtCallableDeclaration?,
    private val statement: KtExpression
) : ReplaceLoopResultTransformation(loop) {

    private val functionName = if (indexVariable != null) "forEachIndexed" else "forEach"

    override val presentation: String
        get() = "$functionName{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, indexVariable, statement, chainedCallGenerator.reformat)
        return chainedCallGenerator.generate("$functionName $0:'{}'", lambda)
    }

    /**
     * Matches:
     *     for (...) {
     *         ...
     *         <statement>
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override val shouldUseInputVariables: Boolean
            get() = false

        override fun match(state: MatchingState): TransformationMatch.Result? {
            // do not suggest conversion to just ".forEach{}" or ".forEachIndexed{}"
            if (state.previousTransformations.isEmpty()) return null

            val statement = state.statements.singleOrNull() ?: return null
            if (statement is KtReturnExpression) return null

            if (!state.lazySequence) {
                // check if it changes any variable that has other usages in the loop - then only lazy sequence is correct
                val onlySequence = statement.anyDescendantOfType<KtNameReferenceExpression> { nameExpr ->
                    if (nameExpr.getQualifiedExpressionForSelector() == null) {
                        // we don't use resolve for write access detection because '+=' which modifies a collection is kind of write access too
                        val isWrite = nameExpr.readWriteAccess(useResolveForReadWrite = false) != ReferenceAccess.READ
                        if (isWrite) {
                            val variable = nameExpr.mainReference.resolve() as? KtCallableDeclaration
                            if (variable != null && variable.countUsages(state.outerLoop) > 1) return@anyDescendantOfType true
                        }
                    }
                    false
                }
                if (onlySequence) return null
            }

            //TODO: should we disallow it for complicated statements like loops, if, when?
            val transformation = ForEachTransformation(state.outerLoop, state.inputVariable, state.indexVariable, statement)
            return TransformationMatch.Result(transformation)
        }
    }
}
