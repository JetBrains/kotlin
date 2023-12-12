import kotlin.test.*

// Check that compiler doesn't optimize it to `true`
fun selfCmp1(x: Int) = x + 1 > x

fun selfCmp2(x: Int) = x - 1 < x

fun box(): String {
    assertFalse(selfCmp1(Int.MAX_VALUE))
    assertFalse(selfCmp2(Int.MIN_VALUE))

    return "OK"
}
