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

enum class Family {
    Iterables,
    Collections,
    Lists,
    Sets,
    Maps,
    InvariantArraysOfObjects,
    ArraysOfObjects,
    ArraysOfPrimitives,
    Sequences,
    CharSequences,
    Strings,
    Ranges,
    RangesOfPrimitives,
    ProgressionsOfPrimitives,
    Generic,
    Primitives;

    val isPrimitiveSpecialization: Boolean by lazy { this in primitiveSpecializations }

    class DocExtension(val family: Family)
    class CodeExtension(val family: Family)
    val doc = DocExtension(this)
    val code = CodeExtension(this)

    companion object {
        val primitiveSpecializations = setOf(ArraysOfPrimitives, RangesOfPrimitives, ProgressionsOfPrimitives, Primitives)
        val defaultFamilies = setOf(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives)
    }
}

enum class PrimitiveType {
    Byte,
    Short,
    Int,
    Long,
    Float,
    Double,
    Boolean,
    Char;

    val capacity by lazy { descendingByDomainCapacity.indexOf(this).let { if (it < 0) it else descendingByDomainCapacity.size - it } }

    companion object {
        val defaultPrimitives = PrimitiveType.values().toSet()
        val numericPrimitives = setOf(Int, Long, Byte, Short, Double, Float)
        val integralPrimitives = setOf(Int, Long, Byte, Short, Char)

        val descendingByDomainCapacity = listOf(Double, Float, Long, Int, Short, Char, Byte)

        fun maxByCapacity(fromType: PrimitiveType, toType: PrimitiveType): PrimitiveType = descendingByDomainCapacity.first { it == fromType || it == toType }
    }
}

fun PrimitiveType.isIntegral(): Boolean = this in PrimitiveType.integralPrimitives
fun PrimitiveType.isNumeric(): Boolean = this in PrimitiveType.numericPrimitives
enum class Inline {
    No,
    Yes,
    Only;

    fun isInline() = this != No
}

enum class Platform {
    Common,
    JVM,
    JS
}

enum class SequenceClass {
    terminal,
    intermediate,
    stateless,
    stateful
}

data class Deprecation(val message: String, val replaceWith: String? = null, val level: DeprecationLevel = DeprecationLevel.WARNING)
val forBinaryCompatibility = Deprecation("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)