package kotlin

fun assertEquals<T>(expected: T, actual: T, message: String? = null) {
    if (expected != actual) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        throw Exception("Unexpected value:$msg expected = '$expected', actual = '$actual'")
    }
}

fun assertNotEquals<T>(illegal: T, actual: T, message: String? = null) {
    if (illegal == actual) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        throw Exception("Illegal value:$msg illegal = '$illegal', actual = '$actual'")
    }
}