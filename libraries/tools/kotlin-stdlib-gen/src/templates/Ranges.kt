/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.PrimitiveType.Companion.maxByCapacity

object RangeOps : TemplateGroupBase() {

    private val rangePrimitives = PrimitiveType.rangePrimitives
    private fun rangeElementType(fromType: PrimitiveType, toType: PrimitiveType) =
        maxByCapacity(fromType, toType).let {
            when {
                it == PrimitiveType.Char -> it
                it in PrimitiveType.unsignedPrimitives -> maxByCapacity(it, PrimitiveType.UInt)
                else -> maxByCapacity(it, PrimitiveType.Int)
            }
        }

    private fun shouldCheckForConversionOverflow(fromType: PrimitiveType, toType: PrimitiveType): Boolean {
        return toType.isIntegral() && fromType.capacity > toType.capacity ||
                toType.isUnsigned() && fromType.capacityUnsigned > toType.capacityUnsigned
    }

    private fun <T> Collection<T>.combinations(): List<Pair<T, T>> = flatMap { a -> map { b -> a to b } }

    private val numericCombinations = PrimitiveType.numericPrimitives.combinations()
    private val primitiveCombinations = numericCombinations + (PrimitiveType.Char to PrimitiveType.Char)
    private val integralCombinations = primitiveCombinations.filter { it.first.isIntegral() && it.second.isIntegral() }
    private val unsignedCombinations = PrimitiveType.unsignedPrimitives.combinations()
    private val unsignedMappings = PrimitiveType.unsignedPrimitives.map { it to it }

    val PrimitiveType.stepType get() = when(this) {
        PrimitiveType.Char -> "Int"
        PrimitiveType.Int, PrimitiveType.Long -> name
        PrimitiveType.UInt, PrimitiveType.ULong -> name.drop(1)
        else -> error("Unsupported progression specialization: $this")
    }

    init {
        defaultBuilder {
            sourceFile(SourceFile.Ranges)
            if (primitive in PrimitiveType.unsignedPrimitives) {
                sinceAtLeast("1.5")
                if (since == null || since!!.toDouble() <= 1.5) {
                    wasExperimental("ExperimentalUnsignedTypes")
                }
                sourceFile(SourceFile.URanges)
            }
        }
    }

    val f_reversed = fn("reversed()") {
        include(ProgressionsOfPrimitives, rangePrimitives)
    } builder {
        doc { "Returns a progression that goes over the same range in the opposite direction with the same step." }
        returns("TProgression")
        body {
            "return TProgression.fromClosedRange(last, first, -step)"
        }
    }

    val f_step = fn("step(step: STEP)") {
        include(ProgressionsOfPrimitives, rangePrimitives)
    } builder {
        infix(true)
        doc { "Returns a progression that goes over the same range with the given step." }
        sample("samples.ranges.Ranges.step$primitive")
        signature("step(step: ${primitive!!.stepType})", notForSorting = true)
        returns("TProgression")
        body {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(first, last, if (this.step > 0) step else -step)
            """
        }
    }

    val f_downTo = fn("downTo(to: Primitive)").byTwoPrimitives {
        include(Primitives, integralCombinations + unsignedMappings)
    } builderWith { (fromType, toType) ->
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
            PrimitiveType.Long, PrimitiveType.ULong -> "-1L"
            PrimitiveType.Float -> "-1.0F"
            PrimitiveType.Double -> "-1.0"
            else -> "-1"
        }

        body {
            "return $progressionType.fromClosedRange($fromExpr, $toExpr, $incrementExpr)"
        }
    }


    val f_until = fn("until(to: Primitive)").byTwoPrimitives {
        include(Primitives, integralCombinations + unsignedMappings)
    } builderWith { (fromType, toType) ->
        infix()
        signature("until(to: $toType)")

        val elementType = rangeElementType(fromType, toType)
        val progressionType = elementType.name + "Range"
        returns(progressionType)

        doc {
            """
            Returns a range from this value up to but excluding the specified [to] value.

            If the [to] value is less than or equal to `this` value, then the returned range is empty.
            """
        }

        val minValue = when {
            elementType == PrimitiveType.Char -> "'\\u0000'"
            elementType.isUnsigned() -> "$toType.MIN_VALUE"
            else -> "$elementType.MIN_VALUE"
        }
        val fromExpr = if (elementType == fromType) "this" else "this.to$elementType()"
        val u = if (elementType.isUnsigned()) "u" else ""

        if (elementType == toType || elementType.isUnsigned()) {
            body {
                // <= instead of == for JS
                """
                if (to <= $minValue) return $progressionType.EMPTY
                return $fromExpr .. (to - 1$u).to$elementType()
                """
            }
        } else {
            body { "return $fromExpr .. (to.to$elementType() - 1$u).to$elementType()" }
        }
    }

    val f_containsMixedClosed = fn("contains(value: Primitive)").byTwoPrimitives {
        include(Ranges, numericCombinations)
        filter { _, (rangeType, itemType) -> rangeType != itemType }
    } builderWith { (rangeType, itemType) ->
        operator()
        signature("contains(value: $itemType)")

        check(rangeType.isNumeric() == itemType.isNumeric()) { "Required rangeType and itemType both to be numeric or both not, got: $rangeType, $itemType" }
        if (rangeType.isIntegral() != itemType.isIntegral()) {
            val message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed."
            deprecate(Deprecation(message, warningSince = "1.3", errorSince = "1.4", hiddenSince = "1.5"))
        }

        platformName("${rangeType.name.decapitalize()}RangeContains")
        returns("Boolean")
        doc { "Checks if the specified [value] belongs to this range." }
        body {
            if (shouldCheckForConversionOverflow(fromType = itemType, toType = rangeType))
                "return value.to${rangeType}ExactOrNull().let { if (it != null) contains(it) else false }"
            else
                "return contains(value.to$rangeType())"
        }
    }

    val f_containsMixedOpenAndPrimitive = fn("contains(value: Primitive)").byTwoPrimitives {
        include(OpenRanges, numericCombinations)
        include(RangesOfPrimitives, numericCombinations.filter { (rangeType, _) -> rangeType in rangePrimitives })
        filter { _, (rangeType, itemType) ->
            rangeType != itemType && rangeType.isIntegral() == itemType.isIntegral() &&
                    rangeType != PrimitiveType.Float
        }
    } builderWith { (rangeType, itemType) ->
        operator()
        specialFor(OpenRanges) {
            since("1.9")
            annotation("@WasExperimental(ExperimentalStdlibApi::class)")
        }
        signature("contains(value: $itemType)")

        check(rangeType.isNumeric() == itemType.isNumeric()) { "Required rangeType and itemType both to be numeric or both not, got: $rangeType, $itemType" }

        platformName("${rangeType.name.decapitalize()}RangeContains")
        returns("Boolean")
        doc { "Checks if the specified [value] belongs to this range." }
        body {
            if (shouldCheckForConversionOverflow(fromType = itemType, toType = rangeType))
                "return value.to${rangeType}ExactOrNull().let { if (it != null) contains(it) else false }"
            else
                "return contains(value.to$rangeType())"
        }
        specialFor(RangesOfPrimitives) {
            inlineOnly()
            body {
                "return (this as ClosedRange<$rangeType>).contains(value)"
            }
        }
    }

    val f_contains_nullable = fn("contains(element: T?)") {
        include(RangesOfPrimitives, rangePrimitives)
    } builder {
        since("1.3")
        operator()
        inlineOnly()

        doc {
            """
            Returns `true` if this ${f.collection} contains the specified [element].

            Always returns `false` if the [element] is `null`.
            """
        }

        returns("Boolean")
        body { "return element != null && contains(element)" }
    }

    val f_contains_unsigned = fn("contains(element: Primitive)").byTwoPrimitives {
        include(RangesOfPrimitives, unsignedCombinations)
        filter { _, (rangeType, itemType) -> rangeType in rangePrimitives && rangeType != itemType }
    } builderWith { (rangeType, itemType) ->
        operator()
        signature("contains(value: $itemType)")
        returns("Boolean")

        since("1.3")
        doc { "Checks if the specified [value] belongs to this range." }

        body {
            if (shouldCheckForConversionOverflow(fromType = itemType, toType = rangeType))
                "return (value shr $rangeType.SIZE_BITS) == ${itemType.zero()} && contains(value.to$rangeType())"
            else
                "return contains(value.to$rangeType())"
        }
    }

    val f_toPrimitiveExactOrNull = fn("to{}ExactOrNull()").byTwoPrimitives {
        include(Primitives, numericCombinations)
        filter { _, (fromType, toType) -> shouldCheckForConversionOverflow(fromType, toType) }
    } builderWith { (fromType, toType) ->
        check(toType.isIntegral())
        visibility("internal")

        signature("to${toType}ExactOrNull()")
        returns("$toType?")

        val isConversionDeprecated = fromType.isFloatingPoint() && toType in listOf(PrimitiveType.Byte, PrimitiveType.Short)
        val conversion = if (isConversionDeprecated) "toInt().to$toType" else "to$toType"

        body {
            "return if (this in $toType.MIN_VALUE.to$fromType()..$toType.MAX_VALUE.to$fromType()) this.$conversion() else null"
        }
    }
}
