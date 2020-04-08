// "Change type of 'myFunction' to '(Int, (Int) -> Boolean) -> Boolean'" "true"
// WITH_RUNTIME

fun foo() {
    var myFunction: (Int, Int) -> Int = <caret>::verifyData
}

fun verifyData(a: Int, b: (Int) -> Boolean) = b(a)