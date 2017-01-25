package konan.internal

@ExportForCppRuntime
internal fun ThrowNullPointerException(): Nothing {
    throw NullPointerException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException(): Nothing {
    throw IndexOutOfBoundsException()
}

@ExportForCppRuntime
internal fun ThrowClassCastException(): Nothing {
    throw ClassCastException()
}

@ExportForCppRuntime
internal fun ThrowArithmeticException() : Nothing {
    throw ArithmeticException()
}

internal fun ThrowNoWhenBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

@ExportForCppRuntime
internal fun TheEmptyString() = ""

internal fun <T: Enum<T>> valueOfForEnum(name: String, arr: Array<T>) : T
{
    for (x in arr)
        if (x.name == name)
            return x
    throw Exception("Invalid enum name: $name")
}

internal fun <T: Enum<T>> valuesForEnum(values: Array<T>): Array<T>
{
    return values.clone() as Array<T>
}