import org.jetbrains.kotlin.fir.plugin.Positive
import org.jetbrains.kotlin.fir.plugin.Negative

fun takePositive(x: @Positive Number) {}
fun takeNegative(x: @Negative Number) {}
fun takeAny(x: Number) {}

fun <K> id(x: K): K = x
fun <K> select(x: K, y: K): K = x

fun test(
    positiveInt: @Positive Int,
    positiveDouble: @Positive Double,
    negativeDouble: @Negative Double
) {
    takePositive(positiveInt) // ok
    takeNegative(negativeDouble) // ok
    takeAny(positiveInt)

    takePositive(<!ILLEGAL_NUMBER_SIGN!>negativeDouble<!>) // error
    takeNegative(<!ILLEGAL_NUMBER_SIGN!>positiveInt<!>) // error

    takePositive(id(positiveInt)) // ok
    takeNegative(<!ILLEGAL_NUMBER_SIGN!>id(positiveInt)<!>) // error

    takePositive(select(positiveInt, positiveInt)) // ok
    // Should be ok, but currently attributes are not passed through common super type calculation
    takePositive(<!ILLEGAL_NUMBER_SIGN!>select(positiveInt, positiveDouble)<!>)
    takePositive(<!ILLEGAL_NUMBER_SIGN!>select(positiveInt, negativeDouble)<!>) // error
    takeNegative(<!ILLEGAL_NUMBER_SIGN!>select(positiveInt, negativeDouble)<!>) // error
}
