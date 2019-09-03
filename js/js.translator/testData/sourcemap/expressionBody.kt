package foo

fun test1() = "O"
fun test2(s: String): String {
    return s
}

fun test(s:String) =
    test1() +
            test2(s)

fun box()
        = test("K")