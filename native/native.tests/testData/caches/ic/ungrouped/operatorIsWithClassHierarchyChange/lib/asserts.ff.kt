package test

import kotlin.test.*

fun asserts() {
    assertFalse(isBaseClass(Class()))
    assertFalse(isBaseInterface(Class()))
}
