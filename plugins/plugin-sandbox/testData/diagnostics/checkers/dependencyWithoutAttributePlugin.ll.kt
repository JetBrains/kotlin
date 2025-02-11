// LL_FIR_DIVERGENCE
// KT-75132
// LL_FIR_DIVERGENCE
import org.jetbrains.kotlin.plugin.sandbox.*

fun takePositive(x: @Positive Number) {}
fun takeNegative(x: @Negative Number) {}
fun takeAny(x: Number) {}

fun test_1(
    positiveInt: @Positive Int,
    negativeInt: @Negative Int,
    someInt: Int
) {
    consumePositiveInt(positiveInt)
    consumePositiveInt(negativeInt) // should be error
    consumePositiveInt(someInt) // should be error
}

fun test_2() {
    val x = producePositiveInt()
    takePositive(<!ILLEGAL_NUMBER_SIGN!>x<!>)
    takeNegative(<!ILLEGAL_NUMBER_SIGN!>x<!>) // should be error
    takeAny(x)
}

fun test_3() {
    val x = produceBoxedPositiveInt().value
    takePositive(<!ILLEGAL_NUMBER_SIGN!>x<!>)
    takeNegative(<!ILLEGAL_NUMBER_SIGN!>x<!>) // should be error
    takeAny(x)
}
