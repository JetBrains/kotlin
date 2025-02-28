fun box(): String = runAll(
    "\"abc\".test1" to { "abc".test1() },
    "null.test1" to { null.test1() },
)

fun String?.test1() {
    assert(this?.length == 5)
}
