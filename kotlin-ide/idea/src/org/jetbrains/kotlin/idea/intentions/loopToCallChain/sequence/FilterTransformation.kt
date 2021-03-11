/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.result.FindTransformationMatcher
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.result.MaxOrMinTransformation
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

abstract class FilterTransformationBase : SequenceTransformation {
    abstract val effectiveCondition: Condition

    abstract val inputVariable: KtCallableDeclaration
    abstract val indexVariable: KtCallableDeclaration?

    override val affectsIndex: Boolean
        get() = true

    /**
     * Matches:
     *     for (...) {
     *         if (<condition>) {
     *             ...
     *         }
     *     }
     *
     * or
     *
     *     for (...) {
     *         if (<condition>) continue
     *         ...
     *     }
     * or
     *
     *     for (...) {
     *         if (<condition>) break
     *         ...
     *     }
     *
     * plus optionally consequent find operation (assignment or return)
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override fun match(state: MatchingState): TransformationMatch? {
            // we merge filter transformations here instead of FilterTransformation.mergeWithPrevious() because of filterIndexed that won't merge otherwise

            var (transformation, currentState) = matchOneTransformation(state) ?: return null
            assert(currentState.indexVariable == state.indexVariable) // indexVariable should not change

            if (transformation !is FilterTransformationBase) {
                return TransformationMatch.Sequence(transformation, currentState)
            }

            val atomicConditions = transformation.effectiveCondition.toAtomicConditions().toMutableList()
            while (true) {
                currentState = currentState.unwrapBlock()

                // do not take 'if' which is required for min/max matcher
                if (MaxOrMinTransformation.Matcher.match(currentState) != null) break

                val (nextTransformation, nextState) = matchOneTransformation(currentState) ?: break
                if (nextTransformation !is FilterTransformationBase) break
                assert(nextState.indexVariable == currentState.indexVariable) // indexVariable should not change

                atomicConditions.addAll(nextTransformation.effectiveCondition.toAtomicConditions())

                currentState = nextState
            }

            val transformations = createTransformationsByAtomicConditions(
                currentState.outerLoop,
                currentState.inputVariable,
                currentState.indexVariable,
                atomicConditions,
                currentState.statements,
                currentState.reformat
            )
            assert(transformations.isNotEmpty())

            val findTransformationMatch = FindTransformationMatcher.matchWithFilterBefore(currentState, transformations.last())
            return if (findTransformationMatch != null) {
                TransformationMatch.Result(
                    findTransformationMatch.resultTransformation,
                    transformations.dropLast(1) + findTransformationMatch.sequenceTransformations
                )
            } else {
                TransformationMatch.Sequence(transformations, currentState)
            }
        }

        private fun createTransformationsByAtomicConditions(
            loop: KtForExpression,
            inputVariable: KtCallableDeclaration,
            indexVariable: KtCallableDeclaration?,
            conditions: List<AtomicCondition>,
            restStatements: List<KtExpression>,
            reformat: Boolean
        ): List<FilterTransformationBase> {
            if (conditions.size == 1) {
                return listOf(createFilterTransformation(loop, inputVariable, indexVariable, conditions.single(), reformat = reformat))
            }

            var transformations = conditions.map { createFilterTransformation(loop, inputVariable, indexVariable, it, reformat = reformat) }

            val resultTransformations = ArrayList<FilterTransformationBase>()

            val lastUseOfIndex = transformations.lastOrNull { it.indexVariable != null }
            if (lastUseOfIndex != null) {
                val index = transformations.indexOf(lastUseOfIndex)
                val condition = CompositeCondition.create(conditions.take(index + 1))
                resultTransformations.add(createFilterTransformation(loop, inputVariable, indexVariable, condition, reformat = reformat))
                transformations = transformations.drop(index + 1)
            }

            for ((transformation, condition) in transformations.zip(conditions)) {
                if (transformation !is FilterTransformation && isSmartCastUsed(
                        inputVariable,
                        restStatements
                    )
                ) { // filterIsInstance of filterNotNull
                    resultTransformations.add(transformation)
                } else {
                    val prevFilter = resultTransformations.lastOrNull() as? FilterTransformation
                    if (prevFilter != null) {
                        val mergedCondition = CompositeCondition.create(
                            prevFilter.effectiveCondition.toAtomicConditions() + transformation.effectiveCondition.toAtomicConditions()
                        )
                        val mergedTransformation = createFilterTransformation(
                            loop,
                            inputVariable,
                            indexVariable,
                            mergedCondition,
                            onlyFilterOrFilterNot = true,
                            reformat = reformat
                        )
                        resultTransformations[resultTransformations.lastIndex] = mergedTransformation
                    } else {
                        resultTransformations.add(
                            createFilterTransformation(
                                loop,
                                inputVariable,
                                indexVariable,
                                condition,
                                onlyFilterOrFilterNot = true,
                                reformat = reformat
                            )
                        )
                    }
                }
            }
            return resultTransformations
        }

        private fun matchOneTransformation(state: MatchingState): Pair<SequenceTransformation, MatchingState>? {
            val ifStatement = state.statements.firstOrNull() as? KtIfExpression ?: return null
            val condition = ifStatement.condition ?: return null
            val thenBranch = ifStatement.then ?: return null
            val elseBranch = ifStatement.`else`

            if (elseBranch == null) {
                return matchOneTransformation(state, condition, false, thenBranch, state.statements.drop(1))
            } else if (state.statements.size == 1) {
                val thenStatement = thenBranch.blockExpressionsOrSingle().singleOrNull()
                if (thenStatement is KtBreakExpression || thenStatement is KtContinueExpression) {
                    return matchOneTransformation(state, condition, false, thenBranch, listOf(elseBranch))
                }

                val elseStatement = elseBranch.blockExpressionsOrSingle().singleOrNull()
                if (elseStatement is KtBreakExpression || elseStatement is KtContinueExpression) {
                    return matchOneTransformation(state, condition, true, elseBranch, listOf(thenBranch))
                }
            }

            return null
        }

        private fun matchOneTransformation(
            state: MatchingState,
            condition: KtExpression,
            negateCondition: Boolean,
            then: KtExpression,
            restStatements: List<KtExpression>
        ): Pair<SequenceTransformation, MatchingState>? {
            // we do not allow filter() which uses neither input variable nor index variable (though is technically possible but looks confusing)
            // shouldUseInputVariables = false does not work for us because we sometimes return Result match in this matcher
            if (!state.inputVariable.hasUsages(condition) &&
                (state.indexVariable == null || !state.indexVariable.hasUsages(condition))
            ) return null

            if (restStatements.isEmpty()) {
                val transformation = createFilterTransformation(
                    state.outerLoop, state.inputVariable, state.indexVariable, Condition.create(condition, negateCondition),
                    reformat = state.reformat
                )
                val newState = state.copy(statements = listOf(then))
                return transformation to newState
            } else {
                val statement = then.blockExpressionsOrSingle().singleOrNull() ?: return null
                when (statement) {
                    is KtContinueExpression -> {
                        if (statement.targetLoop() != state.innerLoop) return null
                        val transformation = createFilterTransformation(
                            state.outerLoop, state.inputVariable, state.indexVariable, Condition.create(condition, !negateCondition),
                            reformat = state.reformat
                        )
                        val newState = state.copy(statements = restStatements)
                        return transformation to newState
                    }

                    is KtBreakExpression -> {
                        if (statement.targetLoop() != state.outerLoop) return null
                        val transformation = TakeWhileTransformation(
                            state.outerLoop, state.inputVariable,
                            if (negateCondition) condition else condition.negate(reformat = state.reformat)
                        )
                        val newState = state.copy(statements = restStatements)
                        return transformation to newState
                    }

                    else -> return null
                }
            }
        }

        private fun createFilterTransformation(
            loop: KtForExpression,
            inputVariable: KtCallableDeclaration,
            indexVariable: KtCallableDeclaration?,
            condition: Condition,
            onlyFilterOrFilterNot: Boolean = false,
            reformat: Boolean
        ): FilterTransformationBase {

            if (indexVariable != null && condition.hasUsagesOf(indexVariable)) {
                return FilterTransformation(loop, inputVariable, indexVariable, condition, isFilterNot = false)
            }

            val conditionAsExpression = condition.asExpression(reformat)
            if (!onlyFilterOrFilterNot) {
                if (conditionAsExpression is KtIsExpression
                    && !conditionAsExpression.isNegated
                    // we cannot use isVariableReference here because expression can be non-physical
                    && conditionAsExpression.leftHandSide.isSimpleName(inputVariable.nameAsSafeName)
                ) {
                    val typeRef = conditionAsExpression.typeReference
                    if (typeRef != null) {
                        return FilterIsInstanceTransformation(loop, inputVariable, typeRef, condition)
                    }
                }

                if (conditionAsExpression is KtBinaryExpression
                    && conditionAsExpression.operationToken == KtTokens.EXCLEQ
                    && conditionAsExpression.right.isNullExpression()
                    && conditionAsExpression.left.isSimpleName(inputVariable.nameAsSafeName)
                ) {
                    return FilterNotNullTransformation(loop, inputVariable, condition)
                }
            }

            if (conditionAsExpression is KtPrefixExpression && conditionAsExpression.operationToken == KtTokens.EXCL) {
                return FilterTransformation(loop, inputVariable, null, condition, isFilterNot = true)
            }

            return FilterTransformation(loop, inputVariable, null, condition, isFilterNot = false)
        }

        private fun isSmartCastUsed(inputVariable: KtCallableDeclaration, statements: List<KtExpression>): Boolean {
            return statements.any { statement ->
                statement.anyDescendantOfType<KtNameReferenceExpression> {
                    it.mainReference.resolve() == inputVariable && it.analyze(BodyResolveMode.PARTIAL)[BindingContext.SMARTCAST, it] != null
                }
            }
        }
    }
}

class FilterTransformation(
    override val loop: KtForExpression,
    override val inputVariable: KtCallableDeclaration,
    override val indexVariable: KtCallableDeclaration?,
    override val effectiveCondition: Condition,
    val isFilterNot: Boolean
) : FilterTransformationBase() {

    init {
        if (isFilterNot) {
            assert(indexVariable == null)
        }
    }

    private val functionName = when {
        indexVariable != null -> "filterIndexed"
        isFilterNot -> "filterNot"
        else -> "filter"
    }

    override val presentation: String
        get() = "$functionName{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val reformat = chainedCallGenerator.reformat
        val lambda = if (indexVariable != null)
            generateLambda(inputVariable, indexVariable, effectiveCondition.asExpression(reformat), reformat)
        else
            generateLambda(
                inputVariable,
                if (isFilterNot) effectiveCondition.asNegatedExpression(reformat) else effectiveCondition.asExpression(reformat),
                reformat
            )
        return chainedCallGenerator.generate("$0$1:'{}'", functionName, lambda)
    }
}

class FilterIsInstanceTransformation(
    override val loop: KtForExpression,
    override val inputVariable: KtCallableDeclaration,
    private val type: KtTypeReference,
    override val effectiveCondition: Condition
) : FilterTransformationBase() {

    override val indexVariable: KtCallableDeclaration? get() = null

    override val presentation: String
        get() = "filterIsInstance<>()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterIsInstance<$0>()", type)
    }
}

class FilterNotNullTransformation(
    override val loop: KtForExpression,
    override val inputVariable: KtCallableDeclaration,
    override val effectiveCondition: Condition
) : FilterTransformationBase() {

    override val indexVariable: KtCallableDeclaration? get() = null

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation, reformat: Boolean): SequenceTransformation? {
        if (previousTransformation is MapTransformation) {
            return MapTransformation(
                loop,
                previousTransformation.inputVariable,
                previousTransformation.indexVariable,
                previousTransformation.mapping,
                mapNotNull = true
            )
        }
        return null
    }

    override val presentation: String
        get() = "filterNotNull()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterNotNull()")
    }
}

class TakeWhileTransformation(
    override val loop: KtForExpression,
    val inputVariable: KtCallableDeclaration,
    val condition: KtExpression
) : SequenceTransformation {

    //TODO: merge multiple

    override val affectsIndex: Boolean
        get() = false

    override val presentation: String
        get() = "takeWhile{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, condition, chainedCallGenerator.reformat)
        return chainedCallGenerator.generate("takeWhile$0:'{}'", lambda)
    }
}
