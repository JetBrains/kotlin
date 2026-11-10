package test

import kotlin.test.*

fun asserts() {
    assertFalse(isBaseClass(Class()))
    assertTrue(isBaseInterface(Class()))
}
