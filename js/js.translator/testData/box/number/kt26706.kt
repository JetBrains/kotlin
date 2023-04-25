// IGNORE_BACKEND_K2: JS_IR
// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1378
package foo

fun <T: Long> longToString(x: T) = " $x"
fun <T: Long> nullableLongToString1(x: T?) = " $x"
fun <T: Long?> nullableLongToString2(x: T) = " $x"
fun <T: Long?> nullableLongToString3(x: T?) = " $x"

fun <T: Number> numberToString(x: T) = " $x"
fun <T: Number> nullableNumberToString1(x: T?) = " $x"
fun <T: Number?> nullableNumberToString2(x: T) = " $x"
fun <T: Number?> nullableNumberToString3(x: T?) = " $x"

fun <T: Comparable<*>> comparableToString(x: T) = " $x"
fun <T: Comparable<*>> nullableComparableToString1(x: T?) = " $x"
fun <T: Comparable<*>?> nullableComparableToString2(x: T) = " $x"
fun <T: Comparable<*>?> nullableComparableToString3(x: T?) = " $x"

fun <T: Any> anyToString(x: T) = " $x"
fun <T: Any> nullableAnyToString1(x: T?) = " $x"
fun <T: Any?> nullableAnyToString2(x: T) = " $x"
fun <T: Any?> nullableAnyToString3(x: T?) = " $x"

fun <T> arbitraryToString(x: T) = " $x"
fun <T> nullableArbitraryToString(x: T?) = " $x"

fun box(): String {
    val x = "895065487315017728"
    if (" ${x.toLong()}" != " $x") return "FAIL 1"
    if (" ${x.toLong() as Comparable<Long>}" != " $x") return "FAIL 2"
    if (" ${x.toLong() as Number}" != " $x") return "FAIL 3"
    if (" ${x.toLong() as Any}" != " $x") return "FAIL 4"
    if (" ${x.toLong() as Long?}" != " $x") return "FAIL 5"
    if (" ${null as Long?}" != " null") return "FAIL 6"
    if (" ${x.toLong() as Comparable<Long>?}" != " $x") return "FAIL 7"
    if (" ${null as Comparable<Long>?}" != " null") return "FAIL 8"
    if (" ${x.toLong() as Number?}" != " $x") return "FAIL 9"
    if (" ${null as Number?}" != " null") return "FAIL 10"
    if (" ${x.toLong() as Any?}" != " $x") return "FAIL 11"
    if (" ${null as Any?}" != " null") return "FAIL 12"
//    if (!testUtils.isLegacyBackend()) {
//        if (longToString(x.toLong()) != " $x") return "FAIL 13"
//        if (nullableLongToString1(x.toLong()) != " $x") return "FAIL 14"
//        if (nullableLongToString1(null) != " null") return "FAIL 15"
//        if (nullableLongToString2(x.toLong()) != " $x") return "FAIL 16"
//        if (nullableLongToString2(null) != " null") return "FAIL 17"
//        if (nullableLongToString3(x.toLong()) != " $x") return "FAIL 18"
//        if (nullableLongToString3(null) != " null") return "FAIL 19"
//        if (numberToString(x.toLong()) != " $x") return "FAIL 20"
//        if (nullableNumberToString1(x.toLong()) != " $x") return "FAIL 21"
//        if (nullableNumberToString1(null) != " null") return "FAIL 22"
//        if (nullableNumberToString2(x.toLong()) != " $x") return "FAIL 23"
//        if (nullableNumberToString2(null) != " null") return "FAIL 24"
//        if (nullableNumberToString3(x.toLong()) != " $x") return "FAIL 25"
//        if (nullableNumberToString3(null) != " null") return "FAIL 26"
//        if (comparableToString(x.toLong()) != " $x") return "FAIL 27"
//        if (nullableComparableToString1(x.toLong()) != " $x") return "FAIL 28"
//        if (nullableComparableToString1(null) != " null") return "FAIL 29"
//        if (nullableComparableToString2(x.toLong()) != " $x") return "FAIL 30"
//        if (nullableComparableToString2(null) != " null") return "FAIL 31"
//        if (nullableComparableToString3(x.toLong()) != " $x") return "FAIL 32"
//        if (nullableComparableToString3(null) != " null") return "FAIL 33"
//        if (anyToString(x.toLong()) != " $x") return "FAIL 34"
//        if (nullableAnyToString1(x.toLong()) != " $x") return "FAIL 35"
//        if (nullableAnyToString1(null) != " null") return "FAIL 36"
//        if (nullableAnyToString2(x.toLong()) != " $x") return "FAIL 37"
//        if (nullableAnyToString2(null) != " null") return "FAIL 38"
//        if (nullableAnyToString3(x.toLong()) != " $x") return "FAIL 39"
//        if (nullableAnyToString3(null) != " null") return "FAIL 40"
//        if (arbitraryToString(x.toLong()) != " $x") return "FAIL 41"
//        if (nullableArbitraryToString(x.toLong()) != " $x") return "FAIL 42"
//        if (nullableArbitraryToString(null) != " null") return "FAIL 43"
//    }

    return "OK"
}
