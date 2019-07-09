// IGNORE_BACKEND: JS
// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1281

package foo

fun checkBooleanVararg(vararg xs: Boolean) {
    assertTrue(xs is BooleanArray)
}

fun checkLongVararg(vararg xs: Long) {
    assertTrue(xs is LongArray)
}

fun checkCharVararg(vararg xs: Char) {
    assertTrue(xs is CharArray)
}

fun box(): String {
    checkBooleanVararg()
    checkBooleanVararg(true)
    checkBooleanVararg(true, false)
    checkBooleanVararg(*booleanArrayOf())
    checkBooleanVararg(*booleanArrayOf(true, false))
    checkBooleanVararg(true, *booleanArrayOf(false), false, *booleanArrayOf())

    checkLongVararg()
    checkLongVararg(1L)
    checkLongVararg(2L, 3L)
    checkLongVararg(*longArrayOf())
    checkLongVararg(*longArrayOf(4L, 5L))
    checkLongVararg(*longArrayOf(4L, 5L), 10L, 20L, 30L)

    checkCharVararg()
    checkCharVararg('a')
    checkCharVararg('b', 'c')
    checkCharVararg(*charArrayOf())
    checkCharVararg(*charArrayOf('d', 'e'))
    checkCharVararg(*charArrayOf(), *charArrayOf(), *charArrayOf())
    checkCharVararg('e', *charArrayOf(), 'f', *charArrayOf(), 'x', *charArrayOf(), 'd')

    return "OK"
}