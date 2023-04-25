// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1454

// MODULE: lib1
// FILE: lib1.kt

package foo

// PROPERTY_READ_COUNT: name=longValue count=4 scope=testLongVal
// PROPERTY_READ_COUNT: name=L23 count=2 scope=testLongVal
// PROPERTY_READ_COUNT: name=L_23 count=2 scope=testLongVal
// PROPERTY_READ_COUNT: name=L46 count=1 scope=testLongVal
fun testLongVal() {
    val longValue = 23L

    val longValueCopy = longValue
    assertEquals(23L, longValueCopy)

    val minusLongValue = -longValue
    assertEquals(-23L, minusLongValue)

    val minusLongValueParenthesized = -(longValue)
    assertEquals(-23L, minusLongValueParenthesized)

    val twiceLongValue = 2 * longValue
    assertEquals(46L, twiceLongValue)
}

private const val privateLongConst = 10 * 10L

internal const val internalLongConst = 10 * 100L

const val longConst = 42L

// PROPERTY_READ_COUNT: name=privateLongConst count=1 scope=testLongConst
// PROPERTY_READ_COUNT: name=L100 count=1 scope=testLongConst
// PROPERTY_READ_COUNT: name=internalLongConst count=1 scope=testLongConst
// PROPERTY_READ_COUNT: name=L1000 count=1 scope=testLongConst
// PROPERTY_READ_COUNT: name=longConst count=1 scope=testLongConst
// PROPERTY_READ_COUNT: name=L42 count=1 scope=testLongConst
// PROPERTY_READ_COUNT: name=L_42 count=4 scope=testLongConst
// PROPERTY_READ_COUNT: name=L84 count=2 scope=testLongConst
fun testLongConst() {
    assertEquals(100L, privateLongConst)

    assertEquals(1000L, internalLongConst)

    val longConstCopy = longConst
    assertEquals(42L, longConstCopy)

    val minusLongConst = -longConst
    assertEquals(-42L, minusLongConst)

    val minusLongConstParenthesized = -(longConst)
    assertEquals(-42L, minusLongConstParenthesized)

    val twiceLongConst = 2 * longConst
    assertEquals(84L, twiceLongConst)
}

// PROPERTY_READ_COUNT: name=Long$Companion$MAX_VALUE count=2 scope=testLongMaxMinValue
// PROPERTY_READ_COUNT: name=L_9223372036854775807 count=2 scope=testLongMaxMinValue
// PROPERTY_READ_COUNT: name=Long$Companion$MIN_VALUE count=4 scope=testLongMaxMinValue
fun testLongMaxMinValue() {
    val longMaxValue = Long.MAX_VALUE
    assertEquals(9223372036854775807L, longMaxValue)

    val minusLongMaxValue = -Long.MAX_VALUE
    assertEquals(-9223372036854775807L, minusLongMaxValue)

    val longMinValue = Long.MIN_VALUE
    assertEquals(-9223372036854775807L - 1L, longMinValue)

    val minusLongMinValue = -Long.MIN_VALUE
    assertEquals(-9223372036854775807L - 1L, minusLongMinValue)
}

// PROPERTY_READ_COUNT: name=intValue count=4 scope=testIntVal
fun testIntVal() {
    val intValue = 23

    val intValueCopy = intValue
    assertEquals(23, intValueCopy)

    val minusIntValue = -intValue
    assertEquals(-23, minusIntValue)

    val minusIntValueParenthesized = -(intValue)
    assertEquals(-23, minusIntValueParenthesized)

    val twiceIntValue = 2 * intValue
    assertEquals(46, twiceIntValue)
}

const val intConst = 42

// PROPERTY_NOT_READ_FROM: intConst scope=testIntConst
fun testIntConst() {
    val intConstCopy = intConst
    assertEquals(42, intConstCopy)

    val minusIntConst = -intConst
    assertEquals(-42, minusIntConst)

    val minusIntConstParenthesized = -(intConst)
    assertEquals(-42, minusIntConstParenthesized)

    val twiceIntConst = 2 * intConst
    assertEquals(84, twiceIntConst)
}

// PROPERTY_NOT_READ_FROM: MAX_VALUE scope=testIntMaxMinValue
// PROPERTY_NOT_READ_FROM: MIN_VALUE scope=testIntMaxMinValue
fun testIntMaxMinValue() {
    val intMaxValue = Int.MAX_VALUE
    assertEquals(2147483647, intMaxValue)

    val minusIntMaxValue = -Int.MAX_VALUE
    assertEquals(-2147483647, minusIntMaxValue)

    val intMinValue = Int.MIN_VALUE
    assertEquals(-2147483648, intMinValue)

    val minusIntMinValue = -Int.MIN_VALUE
    assertEquals(-2147483648, minusIntMinValue)
}

const val bigLongConst = 123456789012345L

// PROPERTY_READ_COUNT: name=longConst count=1 scope=testImportedLongConstInlineFunLib1
// PROPERTY_READ_COUNT: name=bigLongConst count=1 scope=testImportedLongConstInlineFunLib1
inline fun testImportedLongConstInlineFunLib1() {
    val longConstCopy = longConst
    assertEquals(42L, longConstCopy)

    val minusLongConst = -longConst
    assertEquals(-42L, minusLongConst)

    val minusLongConstParenthesized = -(longConst)
    assertEquals(-42L, minusLongConstParenthesized)

    val twiceLongConst = 2 * longConst
    assertEquals(84L, twiceLongConst)

    val bigLongConstCopy = bigLongConst
    assertEquals(123456789012345L, bigLongConstCopy)
}

// PROPERTY_READ_COUNT: name=longConst count=1 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L42 count=1 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L_42 count=4 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L84 count=2 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=bigLongConst count=1 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L123456789012345 count=1 scope=testImportedLongConstInlinedLocally
private fun testImportedLongConstInlinedLocally() {
    testImportedLongConstInlineFunLib1()
}

class A {
    companion object {
        private const val a = 10L

        const val b = 20L
    }

    fun testCompanion() {
        assertEquals(10L, a)
        assertEquals(20L, b)
    }
}

fun testCompanionVal() {
    A().testCompanion()
}

fun testLib1() {
    testLongVal()
    testLongConst()
    testLongMaxMinValue()

    testIntVal()
    testIntConst()
    testIntMaxMinValue()

    testImportedLongConstInlinedLocally()

    testCompanionVal()
}

// MODULE: lib2(lib1)
// FILE: lib2.kt

package foo

// PROPERTY_NOT_READ_FROM: $module$lib1.foo.longConst

// PROPERTY_READ_COUNT: name=longConst count=1 scope=testImportedLongConst
// PROPERTY_READ_COUNT: name=L42 count=1 scope=testImportedLongConst
// PROPERTY_READ_COUNT: name=L_42 count=4 scope=testImportedLongConst
// PROPERTY_READ_COUNT: name=L84 count=2 scope=testImportedLongConst
// PROPERTY_READ_COUNT: name=bigLongConst count=1 scope=testImportedLongConst
// PROPERTY_READ_COUNT: name=L123456789012345 count=1 scope=testImportedLongConst
fun testImportedLongConst() {
    val longConstCopy = longConst
    assertEquals(42L, longConstCopy)

    val minusLongConst = -longConst
    assertEquals(-42L, minusLongConst)

    val minusLongConstParenthesized = -(longConst)
    assertEquals(-42L, minusLongConstParenthesized)

    val twiceLongConst = 2 * longConst
    assertEquals(84L, twiceLongConst)

    val bigLongConstCopy = bigLongConst
    assertEquals(123456789012345L, bigLongConstCopy)
}

// PROPERTY_READ_COUNT: name=longConst count=1 scope=testImportedLongConstInlineFun
// PROPERTY_READ_COUNT: name=bigLongConst count=1 scope=testImportedLongConstInlineFun
inline fun testImportedLongConstInlineFun() {
    val longConstCopy = longConst
    assertEquals(42L, longConstCopy)

    val minusLongConst = -longConst
    assertEquals(-42L, minusLongConst)

    val minusLongConstParenthesized = -(longConst)
    assertEquals(-42L, minusLongConstParenthesized)

    val twiceLongConst = 2 * longConst
    assertEquals(84L, twiceLongConst)

    val bigLongConstCopy = bigLongConst
    assertEquals(123456789012345L, bigLongConstCopy)
}

// PROPERTY_READ_COUNT: name=longConst count=1 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L42 count=1 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L_42 count=4 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L84 count=2 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=bigLongConst count=1 scope=testImportedLongConstInlinedLocally
// PROPERTY_READ_COUNT: name=L123456789012345 count=1 scope=testImportedLongConstInlinedLocally
fun testImportedLongConstInlinedLocally() {
    testImportedLongConstInlineFun()
}

// PROPERTY_READ_COUNT: name=longConst count=1 scope=testImportedLongConstInlinedLocallyFromOtherModule
// PROPERTY_READ_COUNT: name=L42 count=1 scope=testImportedLongConstInlinedLocallyFromOtherModule
// PROPERTY_READ_COUNT: name=L_42 count=4 scope=testImportedLongConstInlinedLocallyFromOtherModule
// PROPERTY_READ_COUNT: name=L84 count=2 scope=testImportedLongConstInlinedLocallyFromOtherModule
// PROPERTY_READ_COUNT: name=bigLongConst count=1 scope=testImportedLongConstInlinedLocallyFromOtherModule
// PROPERTY_READ_COUNT: name=L123456789012345 count=1 scope=testImportedLongConstInlinedLocallyFromOtherModule
private fun testImportedLongConstInlinedLocallyFromOtherModule() {
    testImportedLongConstInlineFunLib1()
}

fun testLib2() {
    testLib1()

    testImportedLongConst()
    testImportedLongConstInlinedLocallyFromOtherModule()

    assertEquals(20L, A.b)
}

// MODULE: main(lib2)
// FILE: main.kt
package foo

// PROPERTY_READ_COUNT: name=longConst count=1 scope=testImportedLongConstInlinedFromOtherModule TARGET_BACKENDS=JS
// PROPERTY_READ_COUNT: name=L42 count=1 scope=testImportedLongConstInlinedFromOtherModule TARGET_BACKENDS=JS
// PROPERTY_READ_COUNT: name=L_42 count=4 scope=testImportedLongConstInlinedFromOtherModule TARGET_BACKENDS=JS
// PROPERTY_READ_COUNT: name=L84 count=2 scope=testImportedLongConstInlinedFromOtherModule TARGET_BACKENDS=JS
// PROPERTY_READ_COUNT: name=bigLongConst count=1 scope=testImportedLongConstInlinedFromOtherModule TARGET_BACKENDS=JS
// PROPERTY_READ_COUNT: name=L123456789012345 count=1 scope=testImportedLongConstInlinedFromOtherModule TARGET_BACKENDS=JS
fun testImportedLongConstInlinedFromOtherModule() {
    testImportedLongConstInlineFun()
}


fun box(): String {
    testLib2()

    testImportedLongConstInlinedLocally()
    testImportedLongConstInlinedFromOtherModule()

    return "OK"
}
