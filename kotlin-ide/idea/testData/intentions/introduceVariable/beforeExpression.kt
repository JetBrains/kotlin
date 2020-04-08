// IS_APPLICABLE: false
fun foo(a: Int, b: Int) = a + b

fun foo() {
   <caret> foo(1 + 2, 3 * 4)
}