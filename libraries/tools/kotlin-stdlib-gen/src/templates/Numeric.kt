/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            """
            var sum: Double = 0.0
            var count: Int = 0
            for (element in this) {
                sum += element
                count += 1
            }
            return if (count == 0) Double.NaN else sum / count
            """
        }
    }

}
