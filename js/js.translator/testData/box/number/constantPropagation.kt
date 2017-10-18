// EXPECTED_REACHABLE_NODES: 1126
// PROPERTY_NOT_READ_FROM: longConst
// PROPERTY_READ_COUNT: name=longValue count=4
// PROPERTY_READ_COUNT: name=intConst count=2
// PROPERTY_READ_COUNT: name=longValue count=4
// PROPERTY_READ_COUNT: name=MAX_VALUE count=2
// PROPERTY_READ_COUNT: name=MIN_VALUE count=3

private const val longConst = 42L
private const val intConst = 42

fun box(): String {
    val longValue = 23L

    val longValueCopy = longValue
    val minusLongValue = -longValue
    val minusLongValueParenthesized = -(longValue)
    val twiceLongValue = 2 * longValue

    val longConstCopy = longConst
    val minusLongConst = -longConst
    val minusLongConstParenthesized = -(longConst)
    val twiceLongConst = 2 * longConst

    val longMaxValue = Long.MAX_VALUE
    val minusLongMaxValue = -Long.MAX_VALUE
    val longMinValue = Long.MIN_VALUE
    val minusLongMinValue = -Long.MIN_VALUE

    val intValue = 23L

    val intValueCopy = intValue
    val minusIntValue = -intValue
    val minusIntValueParenthesized = -(intValue)
    val twiceIntValue = 2 * intValue

    val intConstCopy = intConst
    val minusIntConst = -intConst
    val minusIntConstParenthesized = -(intConst)
    val twiceIntConst = 2 * intConst

    val intMaxValue = Int.MAX_VALUE
    val minusIntMaxValue = -Int.MAX_VALUE
    val intMinValue = Int.MIN_VALUE
    val minusIntMinValue = -Int.MIN_VALUE

    return "OK"
}