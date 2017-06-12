// EXPECTED_REACHABLE_NODES: 501
// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange for custom classes that implement ClosedRange
package foo

fun box(): String {
    var result = testFun(-1) + testFun(0) + testFun(5) + testFun(9) + testFun(10) + testFun(150)
    if (result != "misshithithitmisshit!") return "fail1: $result"
    result = testFun2(-1) + testFun2(0) + testFun2(9) + testFun2(10)
    if (result != "hitmissmisshit") return "fail2: $result"
    return "OK"
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
fun testFun2(index: Int): String {
    return when (Wrapper(index)) {
        !in Wrapper(0)..Wrapper(9) -> "hit"
        else -> "miss"
    }
}

class Wrapper(val value: Int) : Comparable<Wrapper> {
    operator fun rangeTo(upper: Wrapper) = WrapperRange(this, upper)
    override operator fun compareTo(other: Wrapper) = value.compareTo(other.value)
}
class WrapperRange(override val start: Wrapper, override val endInclusive: Wrapper) : ClosedRange<Wrapper>
