// IS_APPLICABLE: false
fun foo(a: (Int) -> Int): Int = a(1)
val x = foo {
    val it = 12
    <caret>it
}