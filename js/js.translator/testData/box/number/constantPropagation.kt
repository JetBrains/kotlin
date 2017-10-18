// EXPECTED_REACHABLE_NODES: 1126
// PROPERTY_READ_COUNT: name=longValue count=4 scope=testLongVal
// PROPERTY_NOT_READ_FROM: longConst scope=testLongConst
// PROPERTY_READ_COUNT: name=intValue count=2 scope=testIntVal
// PROPERTY_READ_COUNT: name=intConst count=2 scope=testIntConst
// PROPERTY_READ_COUNT: name=MAX_VALUE count=1 scope=testLongMaxMinValue
// PROPERTY_READ_COUNT: name=MIN_VALUE count=2 scope=testLongMaxMinValue
// PROPERTY_READ_COUNT: name=MAX_VALUE count=1 scope=testIntMaxMinValue
// PROPERTY_READ_COUNT: name=MIN_VALUE count=1 scope=testIntMaxMinValue

fun testLongVal() {
    val longValue = 23L

    val longValueCopy = longValue
    val minusLongValue = -longValue
    val minusLongValueParenthesized = -(longValue)
    val twiceLongValue = 2 * longValue
}

private const val longConst = 42L

fun testLongConst() {
    val longConstCopy = longConst
    val minusLongConst = -longConst
    val minusLongConstParenthesized = -(longConst)
    val twiceLongConst = 2 * longConst
}

fun testLongMaxMinValue() {
    val longMaxValue = Long.MAX_VALUE
    val minusLongMaxValue = -Long.MAX_VALUE
    val longMinValue = Long.MIN_VALUE
    val minusLongMinValue = -Long.MIN_VALUE
}

fun testIntVal() {
    val intValue = 23

    val intValueCopy = intValue
    val minusIntValue = -intValue
    val minusIntValueParenthesized = -(intValue)
    val twiceIntValue = 2 * intValue
}

private const val intConst = 42

fun testIntConst() {
    val intConstCopy = intConst
    val minusIntConst = -intConst
    val minusIntConstParenthesized = -(intConst)
    val twiceIntConst = 2 * intConst
}

fun testIntMaxMinValue() {
    val intMaxValue = Int.MAX_VALUE
    val minusIntMaxValue = -Int.MAX_VALUE
    val intMinValue = Int.MIN_VALUE
    val minusIntMinValue = -Int.MIN_VALUE
}

fun box(): String = "OK"