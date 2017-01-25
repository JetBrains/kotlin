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

fun <T: Enum<T>> valueOfForEnum(name: String, arr: Array<T>) : T
{
    for (x in arr)
        if (x.name == name)
            return x
    throw Exception("Invalid enum name: $name")
}

fun <T: Enum<T>> valuesForEnum(values: Array<T>): Array<T>
{
    return values.clone() as Array<T>
}