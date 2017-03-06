package konan.internal

@ExportForCppRuntime
fun ThrowNullPointerException(): Nothing {
    throw NullPointerException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException(): Nothing {
    throw IndexOutOfBoundsException()
}

@ExportForCppRuntime
fun ThrowClassCastException(): Nothing {
    throw ClassCastException()
}

@ExportForCppRuntime
internal fun ThrowArithmeticException() : Nothing {
    throw ArithmeticException()
}

fun ThrowNoWhenBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
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
    return result as Array<T>
}