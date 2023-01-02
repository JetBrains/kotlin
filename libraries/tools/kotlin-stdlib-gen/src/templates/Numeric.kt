/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*

object Numeric : TemplateGroupBase() {

    init {
        defaultBuilder {
            sequenceClassification(SequenceClass.terminal)
            specialFor(ArraysOfUnsigned) {
                sinceAtLeast("1.3")
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
                    "return sumOf { it.to${p.sumType().name}() }"
            }
        }
        specialFor(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives) {
            platformName("sumOf<T>")

            if (p.isUnsigned()) {
                require(f != ArraysOfPrimitives) { "Arrays of unsigneds are separate from arrays of primitives." }
                specialFor(Iterables) { sourceFile(SourceFile.UCollections) }
                specialFor(Sequences) { sourceFile(SourceFile.USequences) }
                specialFor(ArraysOfObjects) { sourceFile(SourceFile.UArrays) }

                since("1.5")
                wasExperimental("ExperimentalUnsignedTypes")
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

    val f_median = fn("median()") {
        Family.defaultFamilies.forEach { family -> include(family, numericPrimitivesDefaultOrder) }
    } builder {
        doc {
            """
            Returns the median value of the elements in the ${f.collection}, or Double.NaN if the ${f.collection} is empty.
            
            It does so in linear time, based on [QuickSelect](https://en.wikipedia.org/wiki/Quickselect).
            """
        }
        sample("samples.collections.Collections.Numeric.median")
        returns("Double")
        platformName("medianOf<T>")
        body {
            """
            return with(toMutableList()) {
                if (size == 0) {
                    return Double.NaN
                }
            
                fun swap(index1: Int, index2: Int) {
                    val temp = get(index1)
                    this[index1] = this[index2]
                    this[index2] = temp
                }
            
                fun partition(leftIndex: Int, rightIndex: Int): Int {
                    val pivotIndex = (leftIndex + rightIndex) / 2
                    val pivot = get(pivotIndex)
                    swap(pivotIndex, rightIndex)
                    var storageIndex = leftIndex
                    for(i in leftIndex until rightIndex) {
                        if(get(i) < pivot) {
                            swap(i, storageIndex)
                            storageIndex++
                        }
                    }
                    swap(rightIndex, storageIndex)
                    return storageIndex
                }
            
                val midIndex = size / 2
                var left = 0
                var lastLeft = 0
                var right = lastIndex
                var pivotIndex = partition(left, right)
                while (pivotIndex != midIndex) {
                    if (pivotIndex > midIndex) {
                        right = pivotIndex - 1
                    } else {
                        lastLeft = left
                        left = pivotIndex + 1
                    }
                    pivotIndex = partition(left, right)
                }
                if (size % 2 == 0) {
                    // The right-sided middle value was already found, now we need to find the biggest 
                    // element to its left side. At this point, we're sure that the element is between
                    // the lastLeft and midIndex, so just iterate through this range
                    var currentMax = get(lastLeft)
                    for (i in lastLeft until midIndex) {
                        if (get(i) > currentMax)
                            currentMax = get(i)
                    }
                    get(pivotIndex) * 0.5 + currentMax * 0.5
                } else {
                    get(pivotIndex).toDouble()
                }
            }
            """
        }
    }

}
