/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.calculator.arithmeticparser

fun parseAndCompute(expression: String): PartialParser.Result<Double, String> =
        PartialParser(Calculator(), PartialRenderer()).parseWithPartial(expression)

class Calculator : ExpressionComposer<Double> {
    override fun number(value: Double) = value
    override fun plus(left: Double, right: Double) = left + right
    override fun minus(left: Double, right: Double) = left - right
    override fun mult(left: Double, right: Double) = left * right
    override fun div(left: Double, right: Double) = left / right
}

class PartialRenderer : PartialExpressionComposer<Double, String> {
    override fun missing() = "..."

    override fun ending(expression: Double) = "$expression ..."

    override fun plus(left: Double, partialRight: String) = "$left + $partialRight"
    override fun minus(left: Double, partialRight: String) = "$left - $partialRight"
    override fun mult(left: Double, partialRight: String) = "$left * $partialRight"
    override fun div(left: Double, partialRight: String) = "$left / $partialRight"
    override fun leftParenthesized(partialExpression: String) = "($partialExpression"
}

interface ExpressionComposer<E : Any> {
    fun number(value: Double): E
    fun plus(left: E, right: E): E
    fun minus(left: E, right: E): E
    fun mult(left: E, right: E): E
    fun div(left: E, right: E): E
}

open class Parser<E : Any>(private val composer: ExpressionComposer<E>) {
    fun parse(expression: String): E? {
        val tokenizer = Tokenizer(expression)
        val prefix = parseAsPrefix(tokenizer)

        if (prefix is EndedWithExpression && !tokenizer.hasNext()) {
            val reduced = prefix.reduced()
            if (reduced.prefix is Empty) {
                return reduced.expression
            }
        }

        return null
    }

    internal fun parseAsPrefix(tokenizer: Tokenizer): ExpressionPrefix<E> =
            generateSequence<ExpressionPrefix<E>>(Empty) {
                it.tryExtend(tokenizer)
            }.last()

    private fun ExpressionPrefix<E>.tryExtend(tokenizer: Tokenizer): ExpressionPrefix<E>? = when (this) {
        is ContinuableWithExpression -> {
            val number = tokenizer.tryReadNumber()
            when {
                number != null -> this.with(composer.number(number))
                tokenizer.tryReadLeftParenthesis() -> this.withLeftParenthesis()
                else -> null
            }
        }

        is EndedWithExpression -> {
            val operator = tokenizer.tryReadBinaryOperator()
            if (operator != null) {
                this.extendedWithOperator(operator)
            } else {
                val reduced = this.reduced()
                if (reduced.prefix is EndedWithLeftParenthesis && tokenizer.tryReadRightParenthesis()) {
                    // Drop parens:
                    reduced.prefix.prefix.with(reduced.expression)
                } else {
                    null
                }
            }
        }
    }

    private tailrec fun EndedWithExpression<E>.extendedWithOperator(operator: BinaryOperator): EndedWithOperator<E> =
            if (this.prefix is EndedWithOperator && this.prefix.operator.precedence >= operator.precedence) {
                // Apply the operator
                this.prefix
                        .withOperatorApplied(this.expression)
                        .extendedWithOperator(operator)
            } else {
                EndedWithOperator(this.prefix, this.expression, operator)
            }

    internal tailrec fun EndedWithExpression<E>.reduced(): EndedWithExpression<E> = when (this.prefix) {
        Empty, is EndedWithLeftParenthesis -> this

        is EndedWithOperator ->
            this.prefix
                    .withOperatorApplied(this.expression)
                    .reduced()
    }

    private fun EndedWithOperator<E>.withOperatorApplied(rightOperand: E) =
            this.prefix.with(composer.compose(this.operator, this.leftOperand, rightOperand))

    private fun ExpressionComposer<E>.compose(
            binaryOperator: BinaryOperator, left: E, right: E
    ): E = when (binaryOperator) {
        BinaryOperator.PLUS -> plus(left, right)
        BinaryOperator.MINUS -> minus(left, right)
        BinaryOperator.MULT -> mult(left, right)
        BinaryOperator.DIV -> div(left, right)
    }

}

interface PartialExpressionComposer<E : Any, PE : Any> {
    fun missing(): PE
    fun ending(expression: E): PE

    fun plus(left: E, partialRight: PE): PE
    fun minus(left: E, partialRight: PE): PE
    fun mult(left: E, partialRight: PE): PE
    fun div(left: E, partialRight: PE): PE

    fun leftParenthesized(partialExpression: PE): PE
}

class PartialParser<E : Any, PE : Any>(
        composer: ExpressionComposer<E>,
        private val partialComposer: PartialExpressionComposer<E, PE>
) : Parser<E>(composer) {

    data class Result<E : Any, PE : Any>(val expression: E?, val partialExpression: PE, val remainder: String?)

    fun parseWithPartial(expression: String): Result<E, PE> {
        val tokenizer = Tokenizer(expression)
        val prefix = parseAsPrefix(tokenizer)

        val remainder = tokenizer.getRemainder()

        return Result(
                if (remainder != null) null else tryReduce(prefix),
                prefix.toPartialExpression(),
                remainder
        )
    }

    private fun tryReduce(prefix: ExpressionPrefix<E>): E? {
        if (prefix is EndedWithExpression) {
            val reduced = prefix.reduced()
            if (reduced.prefix is Empty) {
                return reduced.expression
            }
        }

        return null
    }

    private fun ExpressionPrefix<E>.toPartialExpression(): PE = when (this) {
        is EndedWithExpression -> this.prefix.toPartialExpressionWith(
                ending = partialComposer.ending(this.expression)
        )
        is ContinuableWithExpression -> this.toPartialExpressionWith(ending = partialComposer.missing())
    }

    private tailrec fun ContinuableWithExpression<E>.toPartialExpressionWith(
            ending: PE
    ): PE = when (this) {
        Empty -> ending

        is EndedWithLeftParenthesis -> this.prefix.toPartialExpressionWith(
                ending = partialComposer.leftParenthesized(ending)
        )

        is EndedWithOperator -> this.prefix.toPartialExpressionWith(
                ending = partialComposer.compose(this.operator, this.leftOperand, ending)
        )
    }

    private fun PartialExpressionComposer<E, PE>.compose(
            binaryOperator: BinaryOperator,
            left: E,
            right: PE
    ): PE = when (binaryOperator) {
        BinaryOperator.PLUS -> plus(left, right)
        BinaryOperator.MINUS -> minus(left, right)
        BinaryOperator.MULT -> mult(left, right)
        BinaryOperator.DIV -> div(left, right)
    }
}

/**
 * Immutable prefix of expression partially parsed to combination of abstractly represented expressions.
 * The prefix representation can be thought as "almost AST", i.e. AST with unfinished rightmost leaf
 * (referenced by this object), and its nodes contain links to parent and (if needed) left child.
 *
 * @param E abstract representation of expression, e.g. its value, AST etc.
 */
internal sealed class ExpressionPrefix<out E>

internal data class EndedWithExpression<E>(
        val prefix: ContinuableWithExpression<E>,
        val expression: E
) : ExpressionPrefix<E>()

internal sealed class ContinuableWithExpression<out E> : ExpressionPrefix<E>()

private fun <E> ContinuableWithExpression<E>.with(expression: E) =
        EndedWithExpression(this, expression)

private object Empty : ContinuableWithExpression<Nothing>()

private data class EndedWithLeftParenthesis<out E>(
        val prefix: ContinuableWithExpression<E>
) : ContinuableWithExpression<E>()

private fun <E> ContinuableWithExpression<E>.withLeftParenthesis() =
        EndedWithLeftParenthesis(this)

private data class EndedWithOperator<out E>(
        val prefix: ContinuableWithExpression<E>,
        val leftOperand: E,
        val operator: BinaryOperator
) : ContinuableWithExpression<E>()

internal enum class BinaryOperator(val sign: Char, val precedence: Int) {
    PLUS('+', 2),
    MINUS('-', 2),
    MULT('*', 3),
    DIV('/', 3)
}

internal class Tokenizer(private val expression: String) {
    private var index = 0

    init {
        skipSpaces()
    }

    fun hasNext(): Boolean = (index < expression.length)

    fun getRemainder(): String? = if (this.hasNext()) {
        expression.substring(index)
    } else {
        null
    }

    fun tryReadNumber(): Double? {
        var endIndex = index
        while (expression.getOrNull(endIndex)?.isNumberChar() == true) {
            ++endIndex
        }

        return expression.substring(index, endIndex).toDoubleOrNull()?.also {
            index = endIndex
            skipSpaces()
        }
    }

    private fun Char.isNumberChar(): Boolean = this in '0'..'9' || this == '.'

    fun tryReadBinaryOperator(): BinaryOperator? = BinaryOperator.values().firstOrNull { tryRead(it.sign) }

    fun tryReadLeftParenthesis(): Boolean = tryRead('(')

    fun tryReadRightParenthesis(): Boolean = tryRead(')')


    private fun tryRead(char: Char): Boolean = if (hasNext() && expression[index] == char) {
        ++index
        skipSpaces()
        true
    } else {
        false
    }

    private fun skipSpaces() {
        while (expression.getOrNull(index)?.isWhitespace() == true) {
            ++index
        }
    }
}
