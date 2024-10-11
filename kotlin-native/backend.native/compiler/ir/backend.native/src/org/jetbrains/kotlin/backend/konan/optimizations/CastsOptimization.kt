/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.getInlinedClassNative
import org.jetbrains.kotlin.backend.konan.ir.isAny
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.copy
import org.jetbrains.kotlin.utils.forEachBit
import java.util.*

internal val STATEMENT_ORIGIN_NO_CAST_NEEDED = IrStatementOriginImpl("NO_CAST_NEEDED")

private val theUnsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        .apply { isAccessible = true }
        .get(null) as sun.misc.Unsafe

private val wordsOffset = theUnsafe.objectFieldOffset(BitSet::class.java.getDeclaredField("words"))

private fun BitSet.getWords(): LongArray = theUnsafe.getObject(this, wordsOffset) as LongArray

private data class LeafIndexWithValue(val index: Int, val value: Boolean) {
    val bitIndex: Int get() = index * 2 + (if (value) 0 else 1)
}

private infix fun Int.setTo(value: Boolean) = LeafIndexWithValue(this, value)

private sealed class LeafTerm {
    abstract fun format(inverted: Boolean): String
}

// A complex boolean function which couldn't be reduced to simpler terms, usually an unknown function call.
private data class ComplexTerm(val element: IrElement) : LeafTerm() {
    override fun format(inverted: Boolean) = when (element) {
        is IrValueDeclaration -> if (inverted) "!${element.name}" else "${element.name}"
        else -> "[${element::class.java.simpleName}@0x${System.identityHashCode(element).toString(16)} is ${!inverted}]"
    }
}

private sealed class SimpleTerm(val variable: IrValueDeclaration) : LeafTerm() {
    class Is(value: IrValueDeclaration, val irClass: IrClass) : SimpleTerm(value) {
        override fun format(inverted: Boolean) = "${variable.name} ${if (inverted) "!is" else "is"} ${irClass.defaultType.render()}"
    }

    class IsNull(value: IrValueDeclaration) : SimpleTerm(value) {
        override fun format(inverted: Boolean) = "${variable.name} ${if (inverted) "!=" else "=="} null"
    }
}

private class Disjunction(val terms: BitSet) {
    val size = terms.cardinality()

    init {
        require(size > 0)
    }

    fun format(leafTerms: List<LeafTerm>) = buildString {
        val isSingleTerm = size == 1
        var first = true
        terms.forEachBit { bit ->
            if (!isSingleTerm) {
                if (first)
                    first = false
                else
                    append(" | ")
                append('(')
            }
            val index = bit / 2
            val inverted = bit % 2 == 1
            append(leafTerms[index].format(inverted))
            if (!isSingleTerm)
                append(')')
        }
    }

    // (a | b) & (a) = a
    infix fun followsFrom(other: Disjunction): Boolean {
        // Check if [other] is a subset of [this]. Seems weird why there is no such function in BitSet.
        val words = terms.getWords()
        val otherWords = other.terms.getWords()
        for (i in otherWords.indices) {
            val otherWord = otherWords[i]
            if (i >= words.size) {
                if (otherWord != 0L) return false
            } else {
                val word = words[i]
                if (word != word or otherWord) return false
            }
        }
        return true
    }
}

private sealed class Predicate {
    open fun format(leafTerms: List<LeafTerm>) = toString()
    open fun size(): Int = 0

    data object False : Predicate()
    data object Empty : Predicate()
}

private class Conjunction(val terms: List<Disjunction>) : Predicate() {
    init {
        require(terms.isNotEmpty())
    }

    override fun format(leafTerms: List<LeafTerm>) = when (terms.size) {
        1 -> terms.first().format(leafTerms)
        else -> terms.joinToString(separator = " & ") { "(${it.format(leafTerms)})" }
    }

    override fun size() = terms.sumOf { it.size }
}

@Suppress("ConvertArgumentToSet")
private object Predicates {
    fun disjunctionOf(vararg terms: LeafIndexWithValue): Predicate =
            Conjunction(listOf(Disjunction(BitSet().apply { terms.forEach { set(it.bitIndex) } })))

    fun isSubtypeOf(
            variable: IrValueDeclaration, type: IrType,
            buildSimpleTerm: (IrValueDeclaration, IrClass?) -> Int, // If IrClass is null, builds the term for nullability check.
    ): Predicate {
        val variableIsNullable = variable.type.isNullable()
        val typeIsNullable = type.isNullable()
        val dstClass = type.erasedUpperBound
        val isSuperClassCast = dstClass.isAny() ||
                (variable.type.classifierOrNull !is IrTypeParameterSymbol // Due to unsafe casts, see unchecked_cast8.kt as an example.
                        && variable.type.isSubtypeOfClass(dstClass.symbol)
                        && variable.type.getInlinedClassNative() == dstClass.defaultType.getInlinedClassNative())
        // TODO: Support never succeeding casts (KT-77671).
        return when {
            isSuperClassCast -> {
                if (variableIsNullable && !typeIsNullable) // (variable: A?) is A = variable != null
                    disjunctionOf(buildSimpleTerm(variable, null) setTo false)
                else Predicate.Empty
            }
            else -> {
                if (variableIsNullable && typeIsNullable) // (variable: A?) is B? = variable == null || variable is B
                    disjunctionOf(buildSimpleTerm(variable, null) setTo true, buildSimpleTerm(variable, dstClass) setTo true)
                else
                    disjunctionOf(buildSimpleTerm(variable, dstClass) setTo true)
            }
        }
    }

    /*
     * The trick here is that at certain points a predicate can be simplified by assuming all the complex terms to be true.
     * This might be conservative but helps with maintaining the predicates' size tractable.
     * Couple of examples:
     *  - the predicate (foo(..) | (x is A)) can just be omitted from consideration since foo is an unknown function,
     *    and for all we know it could have returned true meaning nothing can be deduced about x.
     *  - the predicate (foo(..) & (x is A)) can be simplified to just (x is A), as we don't care much about foo
     *    (conservatively assuming all calls to foo are different and must be considered separately from one another).
     *
     * But it's important to do this only after all intermediate computations have been completed:
     * consider the code: if (foo(..) || x !is A) { .. } else { .. }
     * the predicate would be (no surprise) (foo(..) | (x !is A)). If the call to foo gets optimized,
     * then the predicate will just become an empty predicate (meaning no deductions have been made),
     * but then in the else clause the predicate also will be empty which is suboptimal since in reality
     * we know that x is A inside the else clause (the full predicate is (!foo(..) & (x is A))).
     * In this case the call to foo(..) can be optimized away after the full if/else clause have been handled.
     */
    fun optimizeAwayComplexTerms(predicate: Predicate, complexTermsMask: BitSet): Predicate {
        val conjunction = predicate as? Conjunction ?: return predicate
        val terms = conjunction.terms.filterNot { disjunction -> disjunction.terms.intersects(complexTermsMask) }
        return if (terms.isEmpty()) Predicate.Empty else Conjunction(terms)
    }

    // 010101...010101 in binary.
    private const val CHECKERS_MASK = 0x5555555555555555L

    fun someTermBothTrueAndFalse(terms: BitSet): Boolean {
        val words = terms.getWords()
        for (word in words) {
            if (word == 0L) continue
            val trueTerms = word and CHECKERS_MASK
            val falseTerms = (word shr 1) and CHECKERS_MASK
            if (trueTerms and falseTerms != 0L) return true
        }

        return false
    }

    fun invertTerms(terms: BitSet): BitSet {
        val words = terms.getWords()
        val resultWords = LongArray(words.size)
        for (index in words.indices) {
            val word = words[index]
            val trueTerms = word and CHECKERS_MASK
            val falseTerms = (word shr 1) and CHECKERS_MASK
            resultWords[index] = (trueTerms shl 1) or falseTerms
        }

        return BitSet.valueOf(resultWords)
    }

    private val removedMarker = Disjunction(BitSet().apply { set(0) })
    private val removedMarkerSingletonList = listOf(removedMarker)

    // TODO: Support type hierarchy here (KT-77671).
    fun or(leftPredicate: Predicate, rightPredicate: Predicate): Predicate = when {
        leftPredicate == Predicate.False -> rightPredicate
        rightPredicate == Predicate.False -> leftPredicate
        leftPredicate == Predicate.Empty -> rightPredicate
        rightPredicate == Predicate.Empty -> leftPredicate
        else -> {
            // (a1 & a2 &.. ak) | (b1 & b2 &.. bl) = &[i=1..k, j=1..l] (ai | bj)
            val leftTerms = (leftPredicate as Conjunction).terms
            val rightTerms = (rightPredicate as Conjunction).terms
            val resultDisjunctions = ArrayList<Disjunction>((leftTerms.size + rightTerms.size) * 2)
            var removedCount = 0
            for (leftTerm in leftTerms) {
                for (rightTerm in rightTerms) {
                    val terms = leftTerm.terms.copy().apply { or(rightTerm.terms) }
                    if (someTermBothTrueAndFalse(terms)) continue
                    val currentDisjunction = Disjunction(terms)
                    if (resultDisjunctions.any { it !== removedMarker && currentDisjunction followsFrom it })
                        continue
                    var replacedRemoved = false
                    for (i in resultDisjunctions.indices) {
                        var disjunction = resultDisjunctions[i]
                        if (disjunction !== removedMarker && disjunction followsFrom currentDisjunction) {
                            resultDisjunctions[i] = removedMarker
                            disjunction = removedMarker
                            ++removedCount
                        }
                        if (!replacedRemoved && disjunction === removedMarker) {
                            resultDisjunctions[i] = currentDisjunction
                            --removedCount
                            replacedRemoved = true
                        }
                    }
                    if (!replacedRemoved)
                        resultDisjunctions.add(currentDisjunction)
                }
            }

            when {
                removedCount == resultDisjunctions.size -> Predicate.Empty
                else -> {
                    if (removedCount > 0)
                        resultDisjunctions.removeAll(removedMarkerSingletonList)
                    Conjunction(resultDisjunctions)
                }
            }
        }
    }

    // TODO: Support type hierarchy here (KT-77671).
    fun and(leftPredicate: Predicate, rightPredicate: Predicate): Predicate {
        if (leftPredicate == Predicate.False || rightPredicate == Predicate.False)
            return Predicate.False
        if (leftPredicate == Predicate.Empty)
            return rightPredicate
        if (rightPredicate == Predicate.Empty)
            return leftPredicate

        // (a | b) & (!a) = b & !a
        // (a1 | a2 | b) & !a1 & !a2 = (a1 | b) & !a1 & !a2 = b & !a1 & !a2
        val allSingleTerms = BitSet()
        for (term in (leftPredicate as Conjunction).terms) {
            if (term.size == 1)
                allSingleTerms.or(term.terms)
        }
        for (term in (rightPredicate as Conjunction).terms) {
            if (term.size == 1)
                allSingleTerms.or(term.terms)
        }
        if (someTermBothTrueAndFalse(allSingleTerms))
            return Predicate.False
        val allSingleTermsInverted = invertTerms(allSingleTerms)

        val leftTermsCount = leftPredicate.terms.size
        val rightTermsCount = rightPredicate.terms.size
        val allTermsCount = leftTermsCount + rightTermsCount
        val allTerms = ArrayList<Disjunction>(leftTermsCount + rightTermsCount)
        for (i in 0..<allTermsCount) {
            val term = if (i < leftTermsCount) leftPredicate.terms[i] else rightPredicate.terms[i - leftTermsCount]
            val refinedTerm = if (allSingleTerms.isEmpty)
                term
            else {
                val terms = term.terms.copy()
                terms.andNot(allSingleTermsInverted)
                if (terms.isEmpty) // left and right are contradicting one another.
                    return Predicate.False
                Disjunction(terms)
            }
            allTerms.add(refinedTerm)
        }

        var somethingRemoved = false
        for (i in 0..<leftTermsCount) {
            for (j in leftTermsCount..<allTerms.size)
                if (allTerms[i] followsFrom allTerms[j]) {
                    allTerms[i] = removedMarker
                    somethingRemoved = true
                    break
                }
        }
        for (i in leftTermsCount..<allTerms.size) {
            for (j in 0..<leftTermsCount)
                if (allTerms[j] !== removedMarker && allTerms[i] followsFrom allTerms[j]) {
                    allTerms[i] = removedMarker
                    somethingRemoved = true
                    break
                }
        }
        if (somethingRemoved)
            allTerms.removeAll(removedMarkerSingletonList)

        return Conjunction(allTerms)
    }

    fun invert(predicate: Predicate): Predicate = when (predicate) {
        Predicate.False -> Predicate.Empty
        Predicate.Empty -> Predicate.False
        is Conjunction -> when {
            predicate.terms.size == 1 -> {
                val terms = mutableListOf<Disjunction>()
                predicate.terms.first().terms.forEachBit { bit ->
                    terms.add(Disjunction(BitSet().apply { set(bit xor 1 /* invert the term */) }))
                }
                Conjunction(terms)
            }
            else -> {
                or(
                        invert(Conjunction(listOf(predicate.terms.first()))),
                        invert(Conjunction(predicate.terms.drop(1)))
                )
            }
        }
    }
}

internal class CastsOptimization(val context: Context) : BodyLoweringPass {
    private val not = context.irBuiltIns.booleanNotSymbol
    private val eqeq = context.irBuiltIns.eqeqSymbol
    private val eqeqeq = context.irBuiltIns.eqeqeqSymbol
    private val ieee754EqualsSymbols: Set<IrSimpleFunctionSymbol> =
            context.irBuiltIns.ieee754equalsFunByOperandType.values.toSet()
    private val throwClassCastException = context.symbols.throwClassCastException
    private val unitType = context.irBuiltIns.unitType

    private fun IrExpression.isNullConst() = this is IrConst && this.value == null

    private fun IrTypeOperatorCall.isCast() =
            operator == IrTypeOperator.CAST || operator == IrTypeOperator.IMPLICIT_CAST

    private fun IrTypeOperatorCall.isTypeCheck() =
            operator == IrTypeOperator.INSTANCEOF || operator == IrTypeOperator.NOT_INSTANCEOF

    private fun IrExpression.matchAndAnd(): Pair<IrExpression, IrExpression>? = when {
        // a && b == if (a) b else false
        (this as? IrWhen)?.branches?.size == 2
                && this.branches[1].isUnconditional()
                && (this.branches[1].result as? IrConst)?.value == false
        -> Pair(this.branches[0].condition, this.branches[0].result)
        else -> null
    }

    private fun IrExpression.matchOrOr(): Pair<IrExpression, IrExpression>? = when {
        // a || b == if (a) true else b
        (this as? IrWhen)?.branches?.size == 2
                && this.branches[1].isUnconditional()
                && (this.branches[0].result as? IrConst)?.value == true
        -> Pair(this.branches[0].condition, this.branches[1].result)
        else -> null
    }

    private fun IrExpression.matchNot(): IrExpression? = when {
        (this as? IrCall)?.symbol == not -> this.dispatchReceiver!!
        else -> null
    }

    private fun IrSimpleFunctionSymbol.isEqualityOperator() = this == eqeq || this == eqeqeq || this in ieee754EqualsSymbols

    private fun IrExpression.matchEquality(): Pair<IrExpression, IrExpression>? = when {
        (this as? IrCall)?.symbol?.isEqualityOperator() == true ->
            Pair(this.arguments[0]!!, this.arguments[1]!!)
        else -> null
    }

    private fun IrExpression.matchSafeCall(): Pair<IrExpression, IrExpression>? {
        val statements = (this as? IrBlock)?.statements?.takeIf { it.size == 2 } ?: return null
        val safeReceiver = statements[0] as? IrVariable ?: return null
        val initializer = safeReceiver.initializer ?: return null
        val safeCallResultWhen = (statements[1] as? IrWhen)?.takeIf { it.branches.size == 2 } ?: return null
        val equalityMatchResult = safeCallResultWhen.branches[0].condition.matchEquality() ?: return null
        if ((equalityMatchResult.first as? IrGetValue)?.symbol?.owner != safeReceiver
                || !equalityMatchResult.second.isNullConst()
                || !safeCallResultWhen.branches[0].result.isNullConst())
            return null
        if (!safeCallResultWhen.branches[1].isUnconditional())
            return null

        return Pair(initializer, safeCallResultWhen.branches[1].result)
    }

    private data class BooleanPredicate(val ifTrue: Predicate, val ifFalse: Predicate) {
        fun invert() = BooleanPredicate(ifFalse, ifTrue)
    }

    private data class NullablePredicate(val ifNull: Predicate, val ifNotNull: Predicate)

    private sealed class VariableValue {
        class BooleanPredicate(val predicate: CastsOptimization.BooleanPredicate) : VariableValue()
        class NullablePredicate(val predicate: CastsOptimization.NullablePredicate) : VariableValue()
    }

    private class ControlFlowMergePointInfo(
            // The upper level predicates' stack size.
            val level: Int,
            // This is either an alias (a variable) for the result if it is the only one, or a special marker for multiple values.
            // Keeping all possible values is possible but not very useful (it's hard to build the predicates with them):
            // Consider we have a phi node w merging two variables a and b. What is the predicate for (w is A) then?
            // Is it something like (a is A) | (b is A)? It's not clear and complicates the analysis a lot, so here we just
            // handle a simple but practical case when the phi node only has a single value.
            var phiNodeAlias: IrValueDeclaration? = null,
            var predicate: Predicate = Predicate.Empty,
            val variableAliases: MutableMap<IrVariable, IrValueDeclaration> = mutableMapOf(),
    ) {
        constructor(other: ControlFlowMergePointInfo)
                : this(other.level, other.phiNodeAlias, other.predicate, other.variableAliases.toMutableMap())

        fun contentEquals(other: ControlFlowMergePointInfo): Boolean {
            if (level != other.level) return false
            if (phiNodeAlias != other.phiNodeAlias) return false
            if (variableAliases.size != other.variableAliases.size) return false
            for ((variable, alias) in variableAliases)
                if (other.variableAliases[variable] != alias) return false

            return Predicates.and(
                    Predicates.or(Predicates.invert(predicate), other.predicate),
                    Predicates.or(predicate, Predicates.invert(other.predicate)),
            ) == Predicate.Empty
        }
    }

    private data class VisitorResult(
            // The predicate after evaluating the current expression.
            var predicate: Predicate = Predicate.Empty,
            // If the result of the current expression comes from a variable
            // (basically, IrGetValue possibly wrapped with casts/blocks etc.)
            var variable: IrValueDeclaration? = null,
    ) {
        fun copyFrom(other: VisitorResult) {
            this.predicate = other.predicate
            this.variable = other.variable
        }

        companion object {
            val Nothing = VisitorResult(Predicate.False, null)
        }
    }

    private enum class TypeCheckResult {
        ALWAYS_SUCCEEDS,
        NEVER_SUCCEEDS,
        UNKNOWN
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        var maxSize = 0
        val typeCheckResults = mutableMapOf<IrTypeOperatorCall, TypeCheckResult>()
        irBody.accept(object : IrVisitor<VisitorResult, Predicate>() {
            val leafTerms = mutableListOf<LeafTerm>()
            val simpleTermsMap = mutableMapOf<Pair<IrValueDeclaration, IrClass?>, Int>()
            val complexTermsMap = mutableMapOf<IrElement, Int>()
            val complexTermsMask = BitSet()

            // It's convenient to think of the predicate as a stack of sub-predicates which get anded to get the result.
            val upperLevelPredicates = mutableListOf<Predicate>()

            val variableValueCounters = mutableMapOf<IrVariable, Int>()
            val phantomVariables = mutableMapOf<IrExpression, IrVariable>()
            val phantomCFMPValues = mutableMapOf<Pair<IrElement, IrVariable>, IrExpression>()
            val variableValues = mutableMapOf<IrValueDeclaration, VariableValue>()
            val topLevelPropertyPhantomVariables = mutableMapOf<IrProperty, IrVariable>()
            val instancePropertyPhantomVariables = mutableMapOf<Pair<IrValueDeclaration, IrProperty>, IrVariable>()

            val multipleValuesMarker = createVariable("\$TheMarker", unitType)
            val variableAliases = mutableMapOf<IrVariable, IrValueDeclaration>()
            val returnableBlockCFMPInfos = mutableMapOf<IrReturnableBlock, ControlFlowMergePointInfo>()
            val breaksCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
            val continuesCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()

            fun createVariable(name: String, type: IrType) =
                    IrVariableImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            IrDeclarationOrigin.DEFINED,
                            IrVariableSymbolImpl(),
                            Name.identifier(name),
                            type,
                            isVar = false,
                            isConst = false,
                            isLateinit = false,
                    )

            fun createPhantomVariable(variable: IrVariable, type: IrType): IrVariable {
                val counter = variableValueCounters.getOrPut(variable) { 0 }
                variableValueCounters[variable] = counter + 1
                return createVariable("${variable.name}\$$counter", type)
            }

            fun createPhantomVariable(variable: IrVariable, value: IrExpression) =
                    phantomVariables.getOrPut(value) {
                        createPhantomVariable(variable, value.type)
                    }

            fun createPhantomValueAt(variable: IrVariable, irElement: IrElement) =
                    phantomCFMPValues.getOrPut(Pair(irElement, variable)) {
                        // This is just a stub.
                        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, variable.symbol)
                    }

            fun controlFlowMergePoint(cfmpInfo: ControlFlowMergePointInfo, result: VisitorResult) {
                for ((variable, alias) in variableAliases) {
                    val accumulatedAlias = cfmpInfo.variableAliases[variable]
                    if (accumulatedAlias == null)
                        cfmpInfo.variableAliases[variable] = alias
                    else if (accumulatedAlias != alias && accumulatedAlias != multipleValuesMarker)
                        cfmpInfo.variableAliases[variable] = multipleValuesMarker
                }
                val resultVariable = result.variable
                if (resultVariable != null) {
                    if (cfmpInfo.phiNodeAlias == null)
                        cfmpInfo.phiNodeAlias = resultVariable
                    else if (cfmpInfo.phiNodeAlias != resultVariable)
                        cfmpInfo.phiNodeAlias = multipleValuesMarker
                }
                cfmpInfo.predicate = Predicates.or(
                        cfmpInfo.predicate,
                        getFullPredicate(result.predicate, false, cfmpInfo.level)
                )
            }

            fun finishControlFlowMerging(irElement: IrElement, cfmpInfo: ControlFlowMergePointInfo): VisitorResult {
                variableAliases.clear()
                for ((variable, alias) in cfmpInfo.variableAliases) {
                    variableAliases[variable] = if (alias != multipleValuesMarker)
                        alias
                    else
                        createPhantomVariable(variable, createPhantomValueAt(variable, irElement))
                }
                return VisitorResult(
                        Predicates.optimizeAwayComplexTerms(cfmpInfo.predicate, complexTermsMask),
                        cfmpInfo.phiNodeAlias.takeIf { it != multipleValuesMarker }
                )
            }

            val IrVariable.isMutable: Boolean
                get() = isVar || initializer == null

            inline fun <R> usingUpperLevelPredicate(predicate: Predicate, block: () -> R): R {
                upperLevelPredicates.push(predicate)
                val result = block()
                upperLevelPredicates.pop()
                return result
            }

            fun buildSimpleTerm(variable: IrValueDeclaration, irClass: IrClass?): Int =
                    simpleTermsMap.getOrPut(Pair(variable, irClass)) {
                        leafTerms.add(if (irClass == null) SimpleTerm.IsNull(variable) else SimpleTerm.Is(variable, irClass))
                        leafTerms.size - 1
                    }

            fun buildComplexTerm(element: IrElement): Int =
                    complexTermsMap.getOrPut(element) {
                        leafTerms.add(ComplexTerm(element))
                        val termIndex = leafTerms.size - 1
                        complexTermsMask.set((termIndex setTo true).bitIndex)
                        complexTermsMask.set((termIndex setTo false).bitIndex)
                        termIndex
                    }

            fun getFullPredicate(currentPredicate: Predicate, optimizeAwayComplexTerms: Boolean, level: Int) =
                    usingUpperLevelPredicate(currentPredicate) {
                        val initialPredicate: Predicate = Predicate.Empty
                        upperLevelPredicates.drop(level).fold(initialPredicate) { acc, predicate ->
                            Predicates.and(
                                    acc,
                                    if (optimizeAwayComplexTerms)
                                        Predicates.optimizeAwayComplexTerms(predicate, complexTermsMask)
                                    else predicate
                            )
                        }
                    }

            fun buildIsNotSubtypeOfPredicate(variable: IrValueDeclaration, type: IrType): Predicate =
                    Predicates.invert(Predicates.isSubtypeOf((variableAliases[variable] ?: variable), type, ::buildSimpleTerm))

            fun buildIsSubtypeOfPredicate(variable: IrValueDeclaration, type: IrType): Predicate =
                    Predicates.isSubtypeOf((variableAliases[variable] ?: variable), type, ::buildSimpleTerm)

            fun buildNullablePredicate(variable: IrValueDeclaration): NullablePredicate =
                    variableAliases[variable]?.let { buildNullablePredicate(it) }
                            ?: when (val variableValue = variableValues[variable]) {
                                null -> {
                                    val term = buildSimpleTerm(variable, null)
                                    NullablePredicate(
                                            ifNull = Predicates.disjunctionOf(term setTo true),
                                            ifNotNull = Predicates.disjunctionOf(term setTo false)
                                    )
                                }
                                is VariableValue.NullablePredicate -> variableValue.predicate
                                is VariableValue.BooleanPredicate -> error("Unexpected boolean predicate for ${variable.render()}")
                            }

            fun buildBooleanPredicate(variable: IrValueDeclaration): BooleanPredicate =
                    variableAliases[variable]?.let { buildBooleanPredicate(it) }
                            ?: when (val variableValue = variableValues[variable]) {
                                is VariableValue.BooleanPredicate -> variableValue.predicate
                                else -> {
                                    val term = buildComplexTerm(variable)
                                    BooleanPredicate(
                                            ifTrue = Predicates.disjunctionOf(term setTo true),
                                            ifFalse = Predicates.disjunctionOf(term setTo false)
                                    )
                                }
                            }

            private fun buildNullablePredicate(expression: IrExpression, result: VisitorResult): NullablePredicate? {
                if (!expression.type.isNullable()) {
                    result.copyFrom(expression.accept(this, Predicate.Empty))
                    return NullablePredicate(ifNull = Predicate.False, ifNotNull = Predicate.Empty)
                }
                if (expression is IrTypeOperatorCall && expression.operator == IrTypeOperator.SAFE_CAST) {
                    val (predicate, variable) = expression.argument.accept(this, Predicate.Empty)
                    result.predicate = predicate
                    return if (variable == null) {
                        null
                    } else {
                        tryOptimizeTypeCheck(expression, variable, predicate)
                        return NullablePredicate(
                                ifNull = buildIsNotSubtypeOfPredicate(variable, expression.typeOperand),
                                ifNotNull = buildIsSubtypeOfPredicate(variable, expression.typeOperand)
                        )
                    }
                }
                val matchResultSafeCall = expression.matchSafeCall()
                if (matchResultSafeCall != null) {
                    val (safeReceiverInitializer, safeCallResult) = matchResultSafeCall
                    val safeReceiverPredicate = buildNullablePredicate(safeReceiverInitializer, result)
                    result.variable = null
                    return if (safeReceiverPredicate == null) {
                        null
                    } else {
                        NullablePredicate(
                                ifNull = safeReceiverPredicate.ifNull,
                                ifNotNull = usingUpperLevelPredicate(result.predicate) {
                                    safeCallResult.accept(this, safeReceiverPredicate.ifNotNull).predicate
                                }
                        )
                    }
                }
                result.copyFrom(expression.accept(this, Predicate.Empty))
                return result.variable?.let { buildNullablePredicate(it) }
            }

            fun buildBooleanPredicate(expression: IrExpression): BooleanPredicate {
                expression.matchAndAnd()?.let { return buildAndAnd(it) }
                expression.matchOrOr()?.let { return buildOrOr(it) }
                expression.matchNot()?.let { return buildBooleanPredicate(it).invert() }
                expression.matchEquality()?.let { return buildEqEq(expression, it) }

                if ((expression as? IrConst)?.value == true) {
                    return BooleanPredicate(ifTrue = Predicate.Empty, ifFalse = Predicate.False)
                }
                if ((expression as? IrConst)?.value == false) {
                    return BooleanPredicate(ifTrue = Predicate.False, ifFalse = Predicate.Empty)
                }

                if (expression is IrTypeOperatorCall && expression.isTypeCheck()) {
                    val (predicate, variable) = expression.argument.accept(this, Predicate.Empty)
                    return if (variable == null) {
                        val term = buildComplexTerm(expression)
                        BooleanPredicate(
                                ifTrue = Predicates.and(Predicates.disjunctionOf(term setTo true), predicate),
                                ifFalse = Predicates.and(Predicates.disjunctionOf(term setTo false), predicate)
                        )
                    } else {
                        tryOptimizeTypeCheck(expression, variable, predicate)
                        val fullIsSubtypeOfPredicate = Predicates.and(predicate, buildIsSubtypeOfPredicate(variable, expression.typeOperand))
                        val fullIsNotSubtypeOfPredicate = Predicates.and(predicate, buildIsNotSubtypeOfPredicate(variable, expression.typeOperand))
                        return if (expression.operator == IrTypeOperator.INSTANCEOF)
                            BooleanPredicate(ifTrue = fullIsSubtypeOfPredicate, ifFalse = fullIsNotSubtypeOfPredicate)
                        else
                            BooleanPredicate(ifTrue = fullIsNotSubtypeOfPredicate, ifFalse = fullIsSubtypeOfPredicate)
                    }
                }

                val (predicate, variable) = expression.accept(this, Predicate.Empty)
                return if (variable == null) {
                    val term = buildComplexTerm(expression)
                    BooleanPredicate(
                            ifTrue = Predicates.and(predicate, Predicates.disjunctionOf(term setTo true)),
                            ifFalse = Predicates.and(predicate, Predicates.disjunctionOf(term setTo false))
                    )
                } else {
                    val variablePredicate = buildBooleanPredicate(variable)
                    return BooleanPredicate(
                            ifTrue = Predicates.and(predicate, variablePredicate.ifTrue),
                            ifFalse = Predicates.and(predicate, variablePredicate.ifFalse)
                    )
                }
            }

            fun buildAndAnd(matchResult: Pair<IrExpression, IrExpression>): BooleanPredicate {
                val (left, right) = matchResult
                val leftBooleanPredicate = buildBooleanPredicate(left)
                val rightBooleanPredicate = usingUpperLevelPredicate(leftBooleanPredicate.ifTrue) { buildBooleanPredicate(right) }
                return BooleanPredicate(
                        ifTrue = Predicates.and(leftBooleanPredicate.ifTrue, rightBooleanPredicate.ifTrue),
                        ifFalse = Predicates.or(
                                leftBooleanPredicate.ifFalse,
                                Predicates.and(leftBooleanPredicate.ifTrue, rightBooleanPredicate.ifFalse)
                        )
                )
            }

            fun buildOrOr(matchResult: Pair<IrExpression, IrExpression>): BooleanPredicate {
                val (left, right) = matchResult
                val leftBooleanPredicate = buildBooleanPredicate(left)
                val rightBooleanPredicate = usingUpperLevelPredicate(leftBooleanPredicate.ifFalse) { buildBooleanPredicate(right) }
                return BooleanPredicate(
                        ifTrue = Predicates.or(
                                leftBooleanPredicate.ifTrue,
                                Predicates.and(leftBooleanPredicate.ifFalse, rightBooleanPredicate.ifTrue)
                        ),
                        ifFalse = Predicates.and(leftBooleanPredicate.ifFalse, rightBooleanPredicate.ifFalse)
                )
            }

            fun buildEqEq(expression: IrExpression, matchResult: Pair<IrExpression, IrExpression>): BooleanPredicate {
                // if (x as? A != null) ...  =  if (x is A) ...
                // if ((x as? A)?.y == ..)
                val (left, right) = matchResult
                val leftIsNullConst = left.isNullConst()
                val rightIsNullConst = right.isNullConst()
                return if ((leftIsNullConst || !left.type.isNullable()) && right.type.isNullable()) {
                    val leftPredicate = if (leftIsNullConst)
                        Predicate.Empty
                    else
                        left.accept(this, Predicate.Empty).predicate
                    val rightResult = VisitorResult()
                    val nullablePredicate = usingUpperLevelPredicate(leftPredicate) { buildNullablePredicate(right, rightResult) }
                    val result = Predicates.and(leftPredicate, rightResult.predicate)
                    if (nullablePredicate == null) {
                        val term = buildComplexTerm(expression)
                        BooleanPredicate(
                                ifTrue = Predicates.and(result, Predicates.disjunctionOf(term setTo true)),
                                ifFalse = Predicates.and(result, Predicates.disjunctionOf(term setTo false))
                        )
                    } else if (leftIsNullConst) {
                        BooleanPredicate(
                                ifTrue = Predicates.and(result, nullablePredicate.ifNull),
                                ifFalse = Predicates.and(result, nullablePredicate.ifNotNull)
                        )
                    } else {
                        val term = buildComplexTerm(expression)
                        BooleanPredicate(
                                ifTrue = Predicates.and(
                                        result,
                                        Predicates.and(
                                                nullablePredicate.ifNotNull,
                                                Predicates.disjunctionOf(term setTo true)
                                        )
                                ),
                                ifFalse = Predicates.and(
                                        result,
                                        Predicates.or(
                                                nullablePredicate.ifNull,
                                                Predicates.and(nullablePredicate.ifNotNull, Predicates.disjunctionOf(term setTo false))
                                        )
                                )
                        )
                    }
                } else if ((rightIsNullConst || !right.type.isNullable()) && left.type.isNullable()) {
                    val leftResult = VisitorResult()
                    val nullablePredicate = buildNullablePredicate(left, leftResult)
                    val leftCommonPredicate = leftResult.predicate
                    return if (nullablePredicate == null) {
                        val result = right.accept(this, leftCommonPredicate).predicate
                        val term = buildComplexTerm(expression)
                        BooleanPredicate(
                                ifTrue = Predicates.and(result, Predicates.disjunctionOf(term setTo true)),
                                ifFalse = Predicates.and(result, Predicates.disjunctionOf(term setTo false))
                        )
                    } else if (rightIsNullConst) {
                        BooleanPredicate(
                                ifTrue = Predicates.and(leftCommonPredicate, nullablePredicate.ifNull),
                                ifFalse = Predicates.and(leftCommonPredicate, nullablePredicate.ifNotNull)
                        )
                    } else {
                        val leftIsNullPredicate = Predicates.and(leftCommonPredicate, nullablePredicate.ifNull)
                        val leftIsNotNullPredicate = Predicates.and(leftCommonPredicate, nullablePredicate.ifNotNull)
                        val leftPredicate = Predicates.and(
                                leftCommonPredicate,
                                Predicates.or(nullablePredicate.ifNull, nullablePredicate.ifNotNull)
                        )
                        val rightPredicate = usingUpperLevelPredicate(leftPredicate) { right.accept(this, Predicate.Empty).predicate }
                        val fullLeftIsNullPredicate = Predicates.and(leftIsNullPredicate, rightPredicate)
                        val fullLeftIsNotNullPredicate = Predicates.and(leftIsNotNullPredicate, rightPredicate)
                        val term = buildComplexTerm(expression)
                        BooleanPredicate(
                                ifTrue = Predicates.and(
                                        fullLeftIsNotNullPredicate,
                                        Predicates.disjunctionOf(term setTo true)
                                ),
                                ifFalse = Predicates.or(
                                        fullLeftIsNullPredicate,
                                        Predicates.and(
                                                fullLeftIsNotNullPredicate,
                                                Predicates.disjunctionOf(term setTo false)
                                        )
                                )
                        )
                    }
                } else {
                    val result = expression.accept(this, Predicate.Empty).predicate
                    val term = buildComplexTerm(expression)
                    return BooleanPredicate(
                            ifTrue = Predicates.and(result, Predicates.disjunctionOf(term setTo true)),
                            ifFalse = Predicates.and(result, Predicates.disjunctionOf(term setTo false))
                    )
                }
            }

            private fun IrElement.getImmediateChildren(): List<IrElement> {
                val result = mutableListOf<IrElement>()
                acceptChildrenVoid(object : IrVisitorVoid() {
                    override fun visitElement(element: IrElement) {
                        result.add(element)
                        // Do not recurse.
                    }
                })
                return result
            }

            // Each visitXXX functions takes the predicate before evaluating an element and returns the predicate after the evaluation.
            override fun visitElement(element: IrElement, data: Predicate): VisitorResult {
                val children = element.getImmediateChildren()
                var predicate = data
                for (child in children)
                    predicate = child.accept(this, predicate).predicate
                return VisitorResult(predicate, null)
            }

            override fun visitBlock(expression: IrBlock, data: Predicate): VisitorResult {
                val returnableBlock = expression as? IrReturnableBlock
                val statements = expression.statements
                if (returnableBlock == null) {
                    var predicate = data
                    var resultVariable: IrValueDeclaration? = null
                    statements.forEachIndexed { index, statement ->
                        val result = statement.accept(this, predicate)
                        predicate = result.predicate
                        if (index == statements.lastIndex && expression.type != unitType)
                            resultVariable = result.variable
                    }
                    return VisitorResult(predicate, resultVariable)
                }

                val cfmpInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                returnableBlockCFMPInfos[returnableBlock] = cfmpInfo
                super.visitBlock(expression, data)
                returnableBlockCFMPInfos.remove(returnableBlock)

                return finishControlFlowMerging(expression, cfmpInfo)
            }

            override fun visitReturn(expression: IrReturn, data: Predicate): VisitorResult {
                val result = expression.value.accept(this, data)
                val returnableBlock = expression.returnTargetSymbol.owner as? IrReturnableBlock
                if (returnableBlock != null) {
                    val cfmpInfo = returnableBlockCFMPInfos[returnableBlock]!!
                    if (result.predicate != Predicate.False)
                        controlFlowMergePoint(cfmpInfo, result)
                    context.logMultiple {
                        +expression.dump()
                        +"    result = ${cfmpInfo.predicate.format(leafTerms)}"
                    }
                }
                return VisitorResult.Nothing
            }

            override fun visitThrow(expression: IrThrow, data: Predicate): VisitorResult {
                expression.value.accept(this, data)
                return VisitorResult.Nothing
            }

            override fun visitTry(aTry: IrTry, data: Predicate) = usingUpperLevelPredicate(data) {
                val savedVariableAliases = variableAliases.toMutableMap()

                fun forgetChangedVariables(irElement: IrElement) {
                    val changedVariables = mutableSetOf<IrVariable>()
                    for ((variable, alias) in variableAliases) {
                        val savedAlias = savedVariableAliases[variable]
                        if (savedAlias != null && savedAlias != alias)
                            changedVariables.add(variable)
                    }
                    for (variable in changedVariables) {
                        savedVariableAliases[variable] = createPhantomVariable(variable, createPhantomValueAt(variable, irElement))
                    }
                    variableAliases.clear()
                    for ((variable, alias) in savedVariableAliases) {
                        variableAliases[variable] = alias
                    }
                }

                aTry.tryResult.accept(this, Predicate.Empty)
                // Conservatively assume that the try block might throw an exception right away.
                // This means no variable change inside the try clause is visible for any of the catch clauses.
                forgetChangedVariables(aTry)

                for (aCatch in aTry.catches) {
                    aCatch.accept(this, Predicate.Empty)
                    // Same goes for all the catch clauses (we don't know which one is going to be executed).
                    forgetChangedVariables(aCatch)
                }

                VisitorResult(data)
            }

            override fun visitBreak(jump: IrBreak, data: Predicate): VisitorResult {
                val cfmpInfo = breaksCFMPInfos[jump.loop]!!
                controlFlowMergePoint(cfmpInfo, VisitorResult(data, null))

                return VisitorResult.Nothing
            }

            override fun visitContinue(jump: IrContinue, data: Predicate): VisitorResult {
                val cfmpInfo = continuesCFMPInfos[jump.loop]!!
                controlFlowMergePoint(cfmpInfo, VisitorResult(data, null))

                return VisitorResult.Nothing
            }

            override fun visitLoop(loop: IrLoop, data: Predicate): VisitorResult = usingUpperLevelPredicate(data) {
                val startPredicate = if (loop is IrWhileLoop)
                    buildBooleanPredicate(loop.condition)
                else BooleanPredicate(ifTrue = Predicate.Empty, ifFalse = Predicate.False)

                val breaksCFMPInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                breaksCFMPInfos[loop] = breaksCFMPInfo
                var predicateBeforeBody = startPredicate.ifTrue
                context.logMultiple {
                    +"LOOP START ${loop.condition.render()}"
                    +"    ${predicateBeforeBody.format(leafTerms)}"
                }

                var iter = 0
                do {
                    val savedReturnableBlockCFMPInfos = mutableMapOf<IrReturnableBlock, ControlFlowMergePointInfo>()
                    val savedBreaksCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
                    val savedContinuesCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
                    returnableBlockCFMPInfos.forEach { (key, cfmpInfo) ->
                        savedReturnableBlockCFMPInfos[key] = ControlFlowMergePointInfo(cfmpInfo)
                    }
                    breaksCFMPInfos.forEach { (key, cfmpInfo) ->
                        savedBreaksCFMPInfos[key] = ControlFlowMergePointInfo(cfmpInfo)
                    }
                    continuesCFMPInfos.forEach { (key, cfmpInfo) ->
                        savedContinuesCFMPInfos[key] = ControlFlowMergePointInfo(cfmpInfo)
                    }

                    val body = loop.body
                    val predicateAfterBody = if (body == null)
                        predicateBeforeBody
                    else {
                        val continuesCFMPInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                        continuesCFMPInfos[loop] = continuesCFMPInfo
                        val bodyResult = body.accept(this, predicateBeforeBody)
                        continuesCFMPInfos.remove(loop)
                        controlFlowMergePoint(continuesCFMPInfo, VisitorResult(bodyResult.predicate, null))
                        finishControlFlowMerging(body, continuesCFMPInfo).predicate
                    }
                    val conditionPredicate = usingUpperLevelPredicate(predicateAfterBody) { buildBooleanPredicate(loop.condition) }
                    predicateBeforeBody = Predicates.and(predicateAfterBody, conditionPredicate.ifTrue)
                    controlFlowMergePoint(breaksCFMPInfo, VisitorResult(Predicates.and(predicateAfterBody, conditionPredicate.ifFalse), null))

                    context.logMultiple {
                        +"LOOP ${loop.condition.render()}"
                        +"    ${breaksCFMPInfo.predicate.format(leafTerms)}"
                    }

                    var somethingChanged = false
                    for ((key, cfmpInfo) in returnableBlockCFMPInfos) {
                        if (!savedReturnableBlockCFMPInfos[key]!!.contentEquals(cfmpInfo)) {
                            somethingChanged = true
                            break
                        }
                    }
                    if (!somethingChanged) {
                        for ((key, cfmpInfo) in breaksCFMPInfos) {
                            if (!savedBreaksCFMPInfos[key]!!.contentEquals(cfmpInfo)) {
                                somethingChanged = true
                                break
                            }
                        }
                    }
                    if (!somethingChanged) {
                        for ((key, cfmpInfo) in continuesCFMPInfos) {
                            if (!savedContinuesCFMPInfos[key]!!.contentEquals(cfmpInfo)) {
                                somethingChanged = true
                                break
                            }
                        }
                    }

                    if (!somethingChanged) break

                    ++iter
                } while (iter < 10)
                breaksCFMPInfos.remove(loop)

                @Suppress("ControlFlowWithEmptyBody")
                if (iter >= 10) {
                    // TODO: fallback for diverging analysis (KT-77672).
                }

                val result = finishControlFlowMerging(loop, breaksCFMPInfo)
                VisitorResult(Predicates.and(data, Predicates.or(startPredicate.ifFalse, result.predicate)), null)
            }

            fun tryOptimizeTypeCheck(expression: IrTypeOperatorCall, variable: IrValueDeclaration, predicate: Predicate) {
                val fullPredicate = getFullPredicate(predicate, true, 0)
                context.logMultiple {
                    +"TYPE CHECK: ${expression.dump()}"
                    +"    ${fullPredicate.format(leafTerms)}"
                }
                // Check if (predicate & (v !is T)) is identically equal to false: meaning the cast will always succeed.
                // Similarly, if (predicate & (v is T)) is identically equal to false, then the cast will never succeed.
                // Note: further improvement will be to check not only for identical equality to false but actually try to
                // find the combination of leaf terms satisfying the predicate (though it can be computationally unfeasible).
                val castIsFailedPredicate = Predicates.and(fullPredicate, buildIsNotSubtypeOfPredicate(variable, expression.typeOperand))
                val castIsSuccessfulPredicate = Predicates.and(fullPredicate, buildIsSubtypeOfPredicate(variable, expression.typeOperand))
                context.logMultiple {
                    +"    castIsFailedPredicate: ${castIsFailedPredicate.format(leafTerms)}"
                    +"    castIsSuccessfulPredicate: ${castIsSuccessfulPredicate.format(leafTerms)}"
                    +""
                }
                if (castIsFailedPredicate == Predicate.False) {
                    // The cast will always succeed.
                    val result = when (typeCheckResults[expression]) {
                        null, TypeCheckResult.ALWAYS_SUCCEEDS -> TypeCheckResult.ALWAYS_SUCCEEDS
                        else -> TypeCheckResult.UNKNOWN
                    }
                    typeCheckResults[expression] = result
                } else if (castIsSuccessfulPredicate == Predicate.False) {
                    // The cast will never succeed.
                    val result = when (typeCheckResults[expression]) {
                        null, TypeCheckResult.NEVER_SUCCEEDS -> TypeCheckResult.NEVER_SUCCEEDS
                        else -> TypeCheckResult.UNKNOWN
                    }
                    typeCheckResults[expression] = result
                } else {
                    // The cast is needed.
                    typeCheckResults[expression] = TypeCheckResult.UNKNOWN
                }
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Predicate): VisitorResult {
                /*
                  TYPE_OP type=<root>.A origin=IMPLICIT_CAST typeOperand=<root>.A
                    TYPE_OP type=<root>.A? origin=CAST typeOperand=<root>.A?
                      TYPE_OP type=kotlin.Any origin=IMPLICIT_CAST typeOperand=kotlin.Any
                        GET_VAR 'x: kotlin.Any declared in <root>.foo' type=kotlin.Any origin=null
                 */
                val (argumentPredicate, argumentVariable) = expression.argument.accept(this, data)
                if (expression.isCast() || expression.isTypeCheck() || expression.operator == IrTypeOperator.SAFE_CAST) {
                    if (argumentVariable != null) {
                        tryOptimizeTypeCheck(expression, argumentVariable, argumentPredicate)

                        return if (expression.isCast())
                            VisitorResult(
                                    Predicates.and(argumentPredicate, buildIsSubtypeOfPredicate(argumentVariable, expression.typeOperand)),
                                    argumentVariable.takeIf { // Only if no box/unbox operation is needed.
                                        it.type.getInlinedClassNative() == expression.typeOperand.getInlinedClassNative()
                                    }
                            )
                        else VisitorResult(argumentPredicate, null)
                    }
                }
                return VisitorResult(argumentPredicate, null)
            }

            override fun visitWhen(expression: IrWhen, data: Predicate): VisitorResult = usingUpperLevelPredicate(data) {
                val cfmpInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                var predicate: Predicate = Predicate.Empty
                for (branch in expression.branches) {
                    usingUpperLevelPredicate(predicate) {
                        val conditionBooleanPredicate = buildBooleanPredicate(branch.condition)
                        context.logMultiple {
                            +"WHEN: ${branch.condition.dump()}"
                            +"    upperLevelPredicate = ${getFullPredicate(Predicate.Empty, false, 0).format(leafTerms)}"
                            +"    condition = ${conditionBooleanPredicate.ifTrue.format(leafTerms)}"
                            +"    ~condition = ${conditionBooleanPredicate.ifFalse.format(leafTerms)}"
                            +"    result = ${cfmpInfo.predicate.format(leafTerms)}"
                            +""
                        }
                        val savedVariableAliases = variableAliases.toMap()
                        val branchResult = branch.result.accept(this, conditionBooleanPredicate.ifTrue)
                        if (branchResult.predicate != Predicate.False) { // The result is not unreachable.
                            controlFlowMergePoint(cfmpInfo, branchResult)
                            maxSize = kotlin.math.max(maxSize, cfmpInfo.predicate.size())
                        }
                        variableAliases.clear()
                        for ((variable, alias) in savedVariableAliases)
                            variableAliases[variable] = alias
                        predicate = Predicates.and(predicate, conditionBooleanPredicate.ifFalse)
                        maxSize = kotlin.math.max(maxSize, predicate.size())
                    }
                }
                context.logMultiple {
                    +"WHEN END"
                    +"    result = ${cfmpInfo.predicate.format(leafTerms)}"
                    +"    predicate = ${predicate.format(leafTerms)}"
                }
                if (!expression.branches.last().isUnconditional()) // Non-exhaustive when.
                    controlFlowMergePoint(cfmpInfo, VisitorResult(predicate, null))
                context.log { "    result = ${cfmpInfo.predicate.format(leafTerms)}" }

                val result = finishControlFlowMerging(expression, cfmpInfo)
                context.log { "    result = ${result.predicate.format(leafTerms)}" }
                val resultPredicate = Predicates.and(data, result.predicate)
                context.logMultiple {
                    +"    result = ${resultPredicate.format(leafTerms)}"
                    +""
                }
                VisitorResult(resultPredicate, result.variable)
            }

            fun setVariable(variable: IrVariable, value: IrExpression, data: Predicate): Predicate {
                return if (variable.type.isBoolean()) {
                    val booleanPredicate = usingUpperLevelPredicate(data) { buildBooleanPredicate(value) }
                    val alias = if (variable.isMutable)
                        createPhantomVariable(variable, value).also { variableAliases[variable] = it }
                    else variable
                    context.logMultiple {
                        +("SET VAR: ${variable.render()} is a bool and is delegated to ${alias.takeIf { it != variable }?.render()}" +
                                " ifTrue = ${booleanPredicate.ifTrue.format(leafTerms)}, ifFalse = ${booleanPredicate.ifFalse.format(leafTerms)}")
                        +""
                    }
                    variableValues[alias] = VariableValue.BooleanPredicate(booleanPredicate)
                    Predicates.and(data, Predicates.or(booleanPredicate.ifTrue, booleanPredicate.ifFalse))
                } else if (variable.type.isNullable()) {
                    val result = VisitorResult()
                    val nullablePredicate = usingUpperLevelPredicate(data) { buildNullablePredicate(value, result) }
                    val predicate = Predicates.and(data, result.predicate)
                    val alias = result.variable
                            ?: if (variable.isMutable) createPhantomVariable(variable, value) else variable
                    if (alias != variable)
                        variableAliases[variable] = alias
                    context.logMultiple {
                        +("SET VAR: ${variable.render()} is nullable and is delegated to ${alias.takeIf { it != variable }?.render()}." +
                                " ifNull = ${nullablePredicate?.ifNull?.format(leafTerms)}, ifNotNull = ${nullablePredicate?.ifNotNull?.format(leafTerms)}")
                        +""
                    }
                    if (nullablePredicate == null)
                        predicate
                    else {
                        variableValues[alias] = VariableValue.NullablePredicate(nullablePredicate)
                        Predicates.and(predicate, Predicates.or(nullablePredicate.ifNull, nullablePredicate.ifNotNull))
                    }
                } else {
                    val (predicate, delegatedVariable) = value.accept(this, data)
                    val alias = delegatedVariable
                            ?: if (variable.isMutable) createPhantomVariable(variable, value) else variable
                    if (alias != variable)
                        variableAliases[variable] = alias
                    context.logMultiple {
                        +"SET VAR: ${variable.render()} is delegated to ${alias.takeIf { it != variable }?.render()}"
                        +""
                    }
                    predicate
                }
            }

            override fun visitVariable(declaration: IrVariable, data: Predicate): VisitorResult {
                val initializer = declaration.initializer
                val resultPredicate = if (initializer == null) data else setVariable(declaration, initializer, data)
                return VisitorResult(resultPredicate, null)
            }

            override fun visitSetValue(expression: IrSetValue, data: Predicate): VisitorResult {
                val variable = expression.symbol.owner as? IrVariable ?: error("Unexpected set to ${expression.symbol.owner.render()}")
                return VisitorResult(setVariable(variable, expression.value, data), null)
            }

            override fun visitGetValue(expression: IrGetValue, data: Predicate): VisitorResult {
                return VisitorResult(data, variableAliases[expression.symbol.owner] ?: expression.symbol.owner)
            }

            override fun visitCall(expression: IrCall, data: Predicate): VisitorResult {
                val callee = expression.symbol.owner
                val correspondingProperty = callee.correspondingPropertySymbol?.owner
                val backingField = correspondingProperty?.backingField
                return if (backingField != null
                        && !correspondingProperty.isVar
                        && callee == correspondingProperty.getter
                        && callee.isTrivialGetter
                ) {
                    val receiverResult = expression.dispatchReceiver?.accept(this, data)
                    val phantomVariable = if (receiverResult == null) {
                        topLevelPropertyPhantomVariables.getOrPut(correspondingProperty) {
                            createVariable("<ROOT>.${correspondingProperty.name}", backingField.type)
                        }
                    } else {
                        receiverResult.variable?.let { receiver ->
                            instancePropertyPhantomVariables.getOrPut(Pair(receiver, correspondingProperty)) {
                                createVariable("${receiver.name}.${correspondingProperty.name}", backingField.type)
                            }
                        }
                    }

                    VisitorResult(receiverResult?.predicate ?: Predicate.Empty, phantomVariable)
                } else {
                    super.visitCall(expression, data)
                }
            }
        }, Predicate.Empty)

        if (maxSize > 0) // TODO: fallback if size is too big (KT-77672).
            context.log { "MAX SIZE = $maxSize" }

        if (typeCheckResults.isEmpty()) return
        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid()

                val typeCheckResult = when (typeCheckResults[expression]) {
                    null, TypeCheckResult.UNKNOWN -> return expression
                    TypeCheckResult.ALWAYS_SUCCEEDS -> true
                    TypeCheckResult.NEVER_SUCCEEDS -> false
                }
                return when (expression.operator) {
                    IrTypeOperator.INSTANCEOF -> irBuilder.at(expression).irBoolean(typeCheckResult)
                    IrTypeOperator.NOT_INSTANCEOF -> irBuilder.at(expression).irBoolean(!typeCheckResult)
                    IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> when {
                        typeCheckResult -> irBuilder.at(expression).irBlock(origin = STATEMENT_ORIGIN_NO_CAST_NEEDED) {
                            +irImplicitCast(expression.argument, expression.typeOperand)
                        }
                        else -> if (expression.operator == IrTypeOperator.SAFE_CAST)
                            irBuilder.at(expression).irNull()
                        else
                            irBuilder.at(expression).irCall(throwClassCastException).apply {
                                val typeOperandClass = expression.typeOperand.erasedUpperBound
                                val typeOperandClassReference = IrClassReferenceImpl(
                                        startOffset, endOffset,
                                        context.symbols.nativePtrType,
                                        typeOperandClass.symbol,
                                        typeOperandClass.defaultType
                                )
                                arguments[0] = expression.argument
                                arguments[1] = typeOperandClassReference
                            }
                    }
                    else -> error("Unexpected type operator: ${expression.operator}")
                }
            }
        })
    }
}
