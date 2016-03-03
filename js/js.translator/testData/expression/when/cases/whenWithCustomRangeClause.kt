// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange for custom classes that implement ClosedRange
package foo

fun box(): String {
    var result = testFun(-1) + testFun(0) + testFun(5) + testFun(9) + testFun(10) + testFun(150)
    return if (result == "misshithithitmisshit!") "OK" else "fail"
}
fun testFun(index: Int): String {
    var lower = Wrapper(0)
    var upper = Wrapper(9)
    var secondRange = Wrapper(100)..Wrapper(200)
    return when (Wrapper(index)) {
        in lower..upper -> "hit"
        in secondRange -> "hit!"
        else -> "miss"
    }
}

class Wrapper(val value: Int) : Comparable<Wrapper> {
    operator fun rangeTo(upper: Wrapper) = WrapperRange(this, upper)
    override operator fun compareTo(other: Wrapper) = value.compareTo(other.value)
}
class WrapperRange(override val start: Wrapper, override val endInclusive: Wrapper) : ClosedRange<Wrapper>
