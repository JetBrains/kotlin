package test

import kotlin.test.*

fun asserts() {
    assertTrue(isBaseClass(Class()))
    assertFalse(isBaseInterface(Class()))
}
