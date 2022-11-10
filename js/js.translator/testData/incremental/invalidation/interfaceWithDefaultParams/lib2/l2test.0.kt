private fun test(setepId: Int, i: InterfaceA): Int {
    return i.functionA(setepId, "", false)
}

fun testDefaltParam(setepId: Int): Int {
    return test(setepId, ClassA())
}
