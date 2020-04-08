// WITH_RUNTIME
// FIX: none
fun test(x: Int, y: String) {
    x.run {<caret> y::length }
}