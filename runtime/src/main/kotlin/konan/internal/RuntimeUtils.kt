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

package konan.internal

import kotlin.internal.getProgressionLastElement

@ExportForCppRuntime
fun ThrowNullPointerException(): Nothing {
    throw NullPointerException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException(): Nothing {
    throw ArrayIndexOutOfBoundsException()
}

@ExportForCppRuntime
fun ThrowClassCastException(): Nothing {
    throw ClassCastException()
}

fun ThrowTypeCastException(): Nothing {
    throw TypeCastException()
}

@ExportForCppRuntime
internal fun ThrowArithmeticException() : Nothing {
    throw ArithmeticException()
}

@ExportForCppRuntime
internal fun ThrowNumberFormatException() : Nothing {
    throw NumberFormatException()
}

@ExportForCppRuntime
internal fun ThrowOutOfMemoryError() : Nothing {
    throw OutOfMemoryError()
}

fun ThrowNoWhenBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

fun ThrowUninitializedPropertyAccessException(): Nothing {
    throw UninitializedPropertyAccessException()
}

@ExportForCppRuntime
internal fun TheEmptyString() = ""

fun <T: Enum<T>> valueOfForEnum(name: String, values: Array<T>) : T
{
    var left = 0
    var right = values.size - 1
    while (left <= right) {
        val middle = (left + right) / 2
        val x = values[middle].name.compareTo(name)
        when {
            x < 0 -> left = middle + 1
            x > 0 -> right = middle - 1
            else -> return values[middle]
        }
    }
    throw Exception("Invalid enum name: $name")
}

fun <T: Enum<T>> valuesForEnum(values: Array<T>): Array<T>
{
    val result = Array<T?>(values.size)
    for (value in values)
        result[value.ordinal] = value
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

fun checkProgressionStep(step: Int)  = if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")
fun checkProgressionStep(step: Long) = if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")

fun getProgressionBound(start: Char, end: Char, step: Int): Char =
        getProgressionBound(start.toInt(), end.toInt(), step).toChar()

fun getProgressionBound(start: Int, end: Int, step: Int): Int = getProgressionLastElement(start, end, step)
fun getProgressionBound(start: Long, end: Long, step: Long): Long = getProgressionLastElement(start, end, step)