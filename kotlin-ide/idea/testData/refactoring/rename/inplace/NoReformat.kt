public class OneLineKotlin {
    var <caret>subject = 1
    var aux = 1
    public fun simple0() { subject }
    public fun simple1() { subject = 2 }
    public fun simple2() { aux = subject }
} 