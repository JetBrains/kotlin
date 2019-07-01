// EXPECTED_REACHABLE_NODES: 1273
// KJS_WITH_FULL_RUNTIME

// Copy of stdlib test test.collections.ArraysTest.plusInference

fun box(): String {
    val arrayOfArrays: Array<Array<out Any>> = arrayOf(arrayOf<Any>("s") as Array<out Any>)
    val elementArray = arrayOf<Any>("a") as Array<out Any>
    val arrayPlusElement: Array<Array<out Any>> = arrayOfArrays.plusElement(elementArray)
    assertEquals("a", arrayPlusElement[1][0])

    val arrayOfStringArrays = arrayOf(arrayOf("s"))
    val arrayPlusArray = arrayOfStringArrays + arrayOfStringArrays
    assertEquals("s", arrayPlusArray[1][0])

    return "OK"
}
