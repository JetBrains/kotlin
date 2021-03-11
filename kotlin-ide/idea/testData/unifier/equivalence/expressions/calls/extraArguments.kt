// DISABLE-ERRORS
fun bar(n: Int): Int = n + 1

fun foo() {
    <selection>bar(1, 2)</selection>
    bar(1)
    bar(1, 2)
    bar(2, 1)
}
