package foo

fun box(): String {
    val testStr: String? = "test "
    val nullStr: String? = null

    if (testStr + "is ok!" != "test is ok!") return "testStr + 'is ok!'"
    if (testStr + 55 != "test 55") return "testStr + 55 "
    if (testStr + nullStr != "test null") return "testStr + nullStr"
    if (nullStr + 55 != "null55") return "nullStr + 55"

    return "OK"
}