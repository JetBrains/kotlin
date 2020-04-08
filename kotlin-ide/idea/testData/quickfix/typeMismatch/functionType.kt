// "Change type of 'myFunction' to '(Int, Int) -> Boolean'" "true"
// WITH_RUNTIME

fun foo() {
    var myFunction: (Int, Int) -> Int = <caret>::verifyData
}

fun verifyData(a: Int, b: Int) = (a > 10 && b > 10)