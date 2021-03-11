fun bar(): Int = 1
fun baz()

fun foo() {
    if (true) <selection>bar()</selection>; else baz()
}