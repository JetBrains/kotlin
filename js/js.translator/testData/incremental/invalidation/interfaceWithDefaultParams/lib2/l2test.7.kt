private fun test(setepId: Int, i: InterfaceA): Int {
    return i.functionA(
        x = 3,
        i = 2
    )
}

fun testDefaltParam(setepId: Int): Int {
    return test(setepId, ClassA())
}
