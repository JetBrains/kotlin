/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

object Numeric : TemplateGroupBase() {

    init {
        defaultBuilder {
            sequenceClassification(SequenceClass.terminal)
        }
    }

    // TODO: use just numericPrimitives
    private val numericPrimitivesDefaultOrder = PrimitiveType.defaultPrimitives intersect PrimitiveType.numericPrimitives

    val f_sum = fn("sum()") {
        Family.defaultFamilies.forEach { family -> include(family, numericPrimitivesDefaultOrder) }
    } builder {

        doc { "Returns the sum of all elements in the ${f.collection}." }
        returns("SUM")
        platformName("sumOf<T>")
        body {
            """
            var sum: SUM = ZERO
            for (element in this) {
                sum += element
            }
            return sum
            """
        }
    }

    val f_average = fn("average()") {
        Family.defaultFamilies.forEach { family -> include(family, numericPrimitivesDefaultOrder) }
    } builder {
        doc { "Returns an average value of elements in the ${f.collection}."}
        returns("Double")
        platformName("averageOf<T>")
        body {
            fun checkOverflow(value: String) = if (f == Family.Sequences || f == Family.Iterables) "checkCountOverflow($value)" else value
            """
            var sum: Double = 0.0
            var count: Int = 0
            for (element in this) {
                sum += element
                ${checkOverflow("++count")}
            }
            return if (count == 0) Double.NaN else sum / count
            """
        }
    }

}
