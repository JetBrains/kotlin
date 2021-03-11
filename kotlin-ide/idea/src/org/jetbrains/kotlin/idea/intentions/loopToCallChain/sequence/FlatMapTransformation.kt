/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class FlatMapTransformation(
    override val loop: KtForExpression,
    val inputVariable: KtCallableDeclaration,
    val transform: KtExpression
) : SequenceTransformation {

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "flatMap{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, transform, chainedCallGenerator.reformat)
        return chainedCallGenerator.generate("flatMap$0:'{}'", lambda)
    }

    /**
     * Matches:
     *     for (...) {
     *         ...
     *         for (...) {
     *             ...
     *         }
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override fun match(state: MatchingState): TransformationMatch.Sequence? {
            val nestedLoop = state.statements.singleOrNull() as? KtForExpression ?: return null

            val transform = nestedLoop.loopRange ?: return null
            // check that we iterate over Iterable
            val nestedSequenceType = transform.analyze(BodyResolveMode.PARTIAL).getType(transform) ?: return null
            val builtIns = transform.builtIns
            val iterableType = FuzzyType(builtIns.iterableType, builtIns.iterable.declaredTypeParameters)
            if (iterableType.checkIsSuperTypeOf(nestedSequenceType) == null) return null

            val nestedLoopBody = nestedLoop.body ?: return null
            val newInputVariable = nestedLoop.loopParameter ?: return null

            if (state.indexVariable != null && state.indexVariable.hasUsages(transform)) {
                // if nested loop range uses index, convert to "mapIndexed {...}.flatMap { it }"
                val mapIndexedTransformation =
                    MapTransformation(state.outerLoop, state.inputVariable, state.indexVariable, transform, mapNotNull = false)
                val inputVarExpression = KtPsiFactory(nestedLoop).createExpressionByPattern(
                    "$0", state.inputVariable.nameAsSafeName,
                    reformat = state.reformat
                )
                val transformToUse = if (state.lazySequence) inputVarExpression.asSequence(state.reformat) else inputVarExpression
                val flatMapTransformation = FlatMapTransformation(state.outerLoop, state.inputVariable, transformToUse)
                val newState = state.copy(
                    innerLoop = nestedLoop,
                    statements = listOf(nestedLoopBody),
                    inputVariable = newInputVariable
                )
                return TransformationMatch.Sequence(listOf(mapIndexedTransformation, flatMapTransformation), newState)
            }

            val transformToUse = if (state.lazySequence) transform.asSequence(state.reformat) else transform
            val transformation = FlatMapTransformation(state.outerLoop, state.inputVariable, transformToUse)
            val newState = state.copy(
                innerLoop = nestedLoop,
                statements = listOf(nestedLoopBody),
                inputVariable = newInputVariable
            )
            return TransformationMatch.Sequence(transformation, newState)
        }

        private fun KtExpression.asSequence(reformat: Boolean): KtExpression = KtPsiFactory(this).createExpressionByPattern(
            "$0.asSequence()", this,
            reformat = reformat
        )
    }
}