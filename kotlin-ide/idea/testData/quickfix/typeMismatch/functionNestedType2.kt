// "Change type of 'myFunction' to '(Int) -> KFunction0<Boolean>'" "true"
// WITH_RUNTIME

fun foo() {
    var myFunction: (Int, Int) -> Int = <caret>::verifyData
}

fun Int.internalVerifyData() = this > 0

fun verifyData(a: Int) = a::internalVerifyData