// !LANGUAGE: +ProperIeee754Comparisons
// IGNORE_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1112
package foo


fun testNullable(): String {
    val undefined: Double? = js("undefined")
    val doubleNull: Double? = null

    val plusZero: Double? = +0.0
    val minusZero: Double? = -0.0

    if ((+0.0).equals(minusZero)) return "Total order fail"
    if (plusZero != minusZero) return "IEEE 754 equals fail"

    if (plusZero == doubleNull) return "+0.0 != null fail"
    if (plusZero == undefined) return "+0.0 != undefined fail"

    if (undefined != doubleNull) return "undefined == null fail"
    if (undefined != undefined) return "undefined = undefined fail"
    if (doubleNull != doubleNull) return "doubleNull = doubleNull fail"

    // Double == Float
    val plusZeroAny: Any? = +0.0
    val minusZeroAny: Any? = -0.0f

    if (plusZeroAny is Double && minusZeroAny is Float) {
        if  (plusZeroAny != minusZeroAny) return "IEEE 754 quals fail 2"
    }

    return "OK"
}

fun box(): String {
    val plusZero: Double = +0.0
    val minusZero: Double = -0.0

    if (plusZero.equals(minusZero)) return "Total order fail"
    if (plusZero != minusZero) return "IEEE 754 equals fail"

    val plusZeroFloat: Float = +0.0f
    val minusZeroFloat: Float = -0.0f

    if (plusZeroFloat.equals(minusZeroFloat)) return "Total order fail 2"
    if (plusZeroFloat != minusZeroFloat) return "IEEE 754 equals fail 2"

    if ((plusZero as Any) == (minusZero as Any)) return "Total order fail 4"
    if ((plusZeroFloat as Any) == (minusZeroFloat as Any)) return "Total order fail 5"
    if (plusZero == (minusZero as Any)) return "Total order fail 6"

    val nullableRes = testNullable()
    if (nullableRes != "OK")
        return "Nullable" + nullableRes

    return "OK"
}
