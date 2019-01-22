/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*

object Numeric : TemplateGroupBase() {

    init {
        defaultBuilder {
            sequenceClassification(SequenceClass.terminal)
            specialFor(ArraysOfUnsigned) {
                since("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }
        }
    }

    private val numericPrimitivesDefaultOrder = PrimitiveType.defaultPrimitives intersect PrimitiveType.numericPrimitives
    private val summablePrimitives = numericPrimitivesDefaultOrder + PrimitiveType.unsignedPrimitives

    val f_sum = fn("sum()") {
        listOf(Iterables, Sequences, ArraysOfObjects).forEach { include(it, summablePrimitives) }
        include(ArraysOfPrimitives, numericPrimitivesDefaultOrder)
        include(ArraysOfUnsigned)
    } builder {
        val p = primitive!!

        doc { "Returns the sum of all elements in the ${f.collection}." }
        returns(p.sumType().name)

        specialFor(ArraysOfUnsigned) {
            inlineOnly()

            body {
                if (p == p.sumType())
                    "return storage.sum().to${p.sumType().name}()"
                else
                    "return sumBy { it.to${p.sumType().name}() }"
            }
        }
        specialFor(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives) {
            platformName("sumOf<T>")

            if (p.isUnsigned()) {
                require(f != ArraysOfPrimitives) { "Arrays of unsigneds are separate from arrays of primitives." }
                specialFor(Iterables) { sourceFile(SourceFile.UCollections) }
                specialFor(Sequences) { sourceFile(SourceFile.USequences) }
                specialFor(ArraysOfObjects) { sourceFile(SourceFile.UArrays) }

                since("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }

            body {
                """
                var sum: ${p.sumType().name} = ${p.sumType().zero()}
                for (element in this) {
                    sum += element
                }
                return sum
                """
            }
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
