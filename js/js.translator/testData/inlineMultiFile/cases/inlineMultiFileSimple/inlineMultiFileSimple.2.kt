package foo

// CHECK_NOT_CALLED: sum

fun box(): String {
    val sum3 = sum(1, 2)
    assertEquals(3, sum3)

    return "OK"
}