private fun test(setepId: Int, i: InterfaceA): Int {
    return i.functionA(
        x = setepId - 2,
        s = "s",
        b = false,
        i = 2
    )
}

fun testDefaltParam(setepId: Int): Int {
    return test(setepId, ClassA())
}
