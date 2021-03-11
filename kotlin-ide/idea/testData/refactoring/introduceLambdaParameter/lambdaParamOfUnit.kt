// WITH_DEFAULT_VALUE: false
fun foo(a: Int) {

}

fun bar(a: Int) {
    <selection>foo(a + 1)</selection>
}

fun test() {
    bar(1)
}