// EXPECTED_REACHABLE_NODES: 494
package foo

fun box(): String {

    val arr = LongArray(2)

    val expected: Long = 0
    assertEquals(arr.size, 2)
    assertEquals(expected, arr[0])
    assertEquals(expected, arr[1])

    arr[0] = 2432902008176640000L
    assertEquals(2432902008176640000L, arr[0])

    val arr1 = longArrayOf(1,2,3, 2432902008176640000L)
    assertEquals(1L, arr1[0])
    assertEquals(2L, arr1[1])
    assertEquals(3L, arr1[2])
    assertEquals(2432902008176640000L, arr1[3])

    return "OK"
}
