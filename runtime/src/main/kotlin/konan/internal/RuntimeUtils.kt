package konan.internal

@ExportForCppRuntime
internal fun ThrowNullPointerException() {
    throw NullPointerException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException() {
    throw IndexOutOfBoundsException()
}

@ExportForCppRuntime
internal fun ThrowClassCastException() {
    throw ClassCastException()
}

@ExportForCppRuntime
internal fun TheEmptyString() = ""