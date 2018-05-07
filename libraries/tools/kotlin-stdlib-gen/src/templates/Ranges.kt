/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.PrimitiveType.Companion.maxByCapacity

object RangeOps : TemplateGroupBase() {

    private val rangePrimitives = setOf(PrimitiveType.Int, PrimitiveType.Long, PrimitiveType.Char)
    private fun rangeElementType(fromType: PrimitiveType, toType: PrimitiveType)
            = maxByCapacity(fromType, toType).let { if (it == PrimitiveType.Char) it else maxByCapacity(it, PrimitiveType.Int) }

    private fun <T> Collection<T>.permutations(): List<Pair<T, T>> = flatMap { a -> map { b -> a to b } }

    private val numericPrimitives = PrimitiveType.numericPrimitives
    private val numericPermutations = numericPrimitives.permutations()
    private val primitivePermutations = numericPermutations + (PrimitiveType.Char to PrimitiveType.Char)
    private val integralPermutations = primitivePermutations.filter { it.first.isIntegral() && it.second.isIntegral() }


    val f_reversed = fn("reversed()") {
        include(ProgressionsOfPrimitives, rangePrimitives)
    } builder {
        doc { "Returns a progression that goes over the same range in the opposite direction with the same step." }
        returns("TProgression")
        body {
            "return TProgression.fromClosedRange(last, first, -step)"
        }
    }

    val f_step = fn("step(step: SUM)") {
        include(ProgressionsOfPrimitives, rangePrimitives)
    } builder {
        infix(true)
        doc { "Returns a progression that goes over the same range with the given step." }
        returns("TProgression")
        body {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(first, last, if (this.step > 0) step else -step)
            """
        }
    }

    val f_downTo = fn("downTo(to: Primitive)").byTwoPrimitives {
        include(Primitives, integralPermutations)
    } builderWith { (fromType, toType) ->
        sourceFile(SourceFile.Ranges)

        val elementType = rangeElementType(fromType, toType)
        val progressionType = elementType.name + "Progression"

        infix()
        signature("downTo(to: $toType)")
        returns(progressionType)

        doc {
            """
            Returns a progression from this value down to the specified [to] value with the step -1.

            The [to] value should be less than or equal to `this` value.
            If the [to] value is greater than `this` value the returned progression is empty.
            """
        }


        val fromExpr = if (elementType == fromType) "this" else "this.to$elementType()"
        val toExpr = if (elementType == toType) "to" else "to.to$elementType()"
        val incrementExpr = when (elementType) {
            PrimitiveType.Long -> "-1L"
            PrimitiveType.Float -> "-1.0F"
            PrimitiveType.Double -> "-1.0"
            else -> "-1"
        }

        body {
            "return $progressionType.fromClosedRange($fromExpr, $toExpr, $incrementExpr)"
        }
    }


    val f_until = fn("until(to: Primitive)").byTwoPrimitives {
        include(Primitives, integralPermutations)
    } builderWith { (fromType, toType) ->
        sourceFile(SourceFile.Ranges)

        infix()
        signature("until(to: $toType)")

        val elementType = rangeElementType(fromType, toType)
        val progressionType = elementType.name + "Range"
        returns(progressionType)
        val minValue = if (elementType == PrimitiveType.Char) "'\\u0000'" else "$elementType.MIN_VALUE"
        val minValueRef = if (elementType == PrimitiveType.Char) "`$minValue`" else "[$minValue]"

        doc {
            """
            Returns a range from this value up to but excluding the specified [to] value.

            If the [to] value is less than or equal to `this` value the returned range is empty.

            ${textWhen(elementType == toType) {
                "If the [to] value is less than or equal to $minValueRef the returned range is empty."
            }}
            """
        }

        val fromExpr = if (elementType == fromType) "this" else "this.to$elementType()"

        if (elementType == toType) {
            // hack to work around incorrect char overflow behavior in JVM and int overflow behavior in JS
            val toExpr = when (toType) {
                PrimitiveType.Char -> "to.toInt()"
                PrimitiveType.Int -> "to.toLong()"
                else -> "to"
            }
            body {
                // <= instead of == for JS
                """
                if (to <= $minValue) return $progressionType.EMPTY
                return $fromExpr .. (to - 1).to$elementType()
                """
            }
        } else {
            body { "return $fromExpr .. (to.to$elementType() - 1).to$elementType()" }
        }
    }

    val f_contains = fn("contains(value: Primitive)").byTwoPrimitives {
        include(Ranges, numericPermutations)
        filter { _, (rangeType, itemType) -> rangeType != itemType }
    } builderWith { (rangeType, itemType) ->
        operator()
        signature("contains(value: $itemType)")

        check(rangeType.isNumeric() == itemType.isNumeric()) { "Required rangeType and itemType both to be numeric or both not, got: $rangeType, $itemType" }
        platformName("${rangeType.name.decapitalize()}RangeContains")
        returns("Boolean")
        doc { "Checks if the specified [value] belongs to this range." }
        body {
            if (rangeType.capacity > itemType.capacity || !rangeType.isIntegral())
                "return contains(value.to$rangeType())"
            else
                "return value.to${rangeType}ExactOrNull().let { if (it != null) contains(it) else false }"
        }
    }

    val f_toPrimitiveExactOrNull = fn("to{}ExactOrNull()").byTwoPrimitives {
        include(Primitives, numericPermutations)
        filter { _, (fromType, toType) -> fromType.capacity > toType.capacity && toType.isIntegral() }
    } builderWith { (fromType, toType) ->
        check(toType.isIntegral())
        visibility("internal")
        sourceFile(SourceFile.Ranges)

        signature("to${toType}ExactOrNull()")
        returns("$toType?")
        body {
            "return if (this in $toType.MIN_VALUE.to$fromType()..$toType.MAX_VALUE.to$fromType()) this.to$toType() else null"
        }
    }
}
